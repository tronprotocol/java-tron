package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.tron.keystore.WalletFile;
import org.tron.keystore.WalletUtils;

/**
 * Shared utilities for keystore CLI commands.
 */
final class KeystoreCliUtils {

  private static final long MAX_FILE_SIZE = 1024;

  /**
   * Cap on the size of a single keystore JSON read during directory scans.
   * Standard V3 keystores are ~500–700 bytes; 8 KiB leaves headroom for
   * unusual scrypt parameter combinations while bounding the memory cost
   * of scanning a hostile directory of planted oversized files.
   */
  static final long MAX_KEYSTORE_SIZE = 8 * 1024;

  private KeystoreCliUtils() {
  }

  /**
   * Read a regular file safely without following symbolic links.
   *
   * <p>This prevents an attacker who can plant files in a user-supplied
   * path from redirecting the read to an arbitrary file on disk (e.g. a
   * symlink pointing at {@code /etc/shadow} or a user's SSH private key).
   * Also rejects FIFOs, devices and other non-regular files.
   *
   * @param file      the file to read
   * @param maxSize   maximum acceptable file size in bytes
   * @param label     human-readable label used in error messages
   * @param err       writer for diagnostic messages
   * @return file bytes, or {@code null} if the file is missing, a symlink,
   *         not a regular file, or too large (err is written in each case)
   */
  static byte[] readRegularFile(File file, long maxSize, String label, PrintWriter err)
      throws IOException {
    Path path = file.toPath();

    BasicFileAttributes attrs;
    try {
      attrs = Files.readAttributes(path, BasicFileAttributes.class,
          LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException e) {
      err.println(label + " not found: " + file.getPath());
      return null;
    }

    if (attrs.isSymbolicLink()) {
      err.println("Refusing to follow symbolic link: " + file.getPath());
      return null;
    }
    if (!attrs.isRegularFile()) {
      err.println("Not a regular file: " + file.getPath());
      return null;
    }
    if (attrs.size() > maxSize) {
      err.println(label + " too large (max " + maxSize + " bytes): " + file.getPath());
      return null;
    }

    int size = (int) attrs.size();
    java.util.Set<OpenOption> openOptions = new HashSet<>();
    openOptions.add(StandardOpenOption.READ);
    openOptions.add(LinkOption.NOFOLLOW_LINKS);
    try (SeekableByteChannel ch = Files.newByteChannel(path, openOptions)) {
      ByteBuffer buf = ByteBuffer.allocate(size);
      while (buf.hasRemaining()) {
        if (ch.read(buf) < 0) {
          break;
        }
      }
      if (buf.position() < size) {
        byte[] actual = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, actual, 0, buf.position());
        return actual;
      }
      return buf.array();
    }
  }

  static String readPassword(File passwordFile, PrintWriter err) throws IOException {
    if (passwordFile != null) {
      byte[] bytes = readRegularFile(passwordFile, MAX_FILE_SIZE, "Password file", err);
      if (bytes == null) {
        return null;
      }
      try {
        String password = WalletUtils.stripPasswordLine(
            new String(bytes, StandardCharsets.UTF_8));
        // Reject multi-line password files. stripPasswordLine only trims
        // trailing terminators; any remaining \n/\r means the file had
        // interior line breaks. A common mistake is passing a two-line
        // `keystore update` password file to `keystore new` / `import` —
        // without this guard the literal "old\nnew" would silently become
        // the password, and neither visible line alone would unlock the
        // keystore later.
        if (password.indexOf('\n') >= 0 || password.indexOf('\r') >= 0) {
          err.println("Password file contains multiple lines; provide a "
              + "single-line password (the `keystore update` two-line "
              + "format is not accepted here).");
          return null;
        }
        if (!WalletUtils.passwordValid(password)) {
          err.println("Invalid password: must be at least 6 characters.");
          return null;
        }
        return password;
      } finally {
        Arrays.fill(bytes, (byte) 0);
      }
    }

    Console console = System.console();
    if (console == null) {
      err.println("No interactive terminal available. "
          + "Use --password-file to provide password.");
      return null;
    }

    char[] pwd1 = console.readPassword("Enter password: ");
    if (pwd1 == null) {
      err.println("Password input cancelled.");
      return null;
    }
    char[] pwd2 = console.readPassword("Confirm password: ");
    if (pwd2 == null) {
      Arrays.fill(pwd1, '\0');
      err.println("Password input cancelled.");
      return null;
    }
    try {
      if (!Arrays.equals(pwd1, pwd2)) {
        err.println("Passwords do not match.");
        return null;
      }
      String password = new String(pwd1);
      if (!WalletUtils.passwordValid(password)) {
        err.println("Invalid password: must be at least 6 characters.");
        return null;
      }
      return password;
    } finally {
      Arrays.fill(pwd1, '\0');
      Arrays.fill(pwd2, '\0');
    }
  }

  static void ensureDirectory(File dir) throws IOException {
    Path path = dir.toPath();
    if (Files.exists(path) && !Files.isDirectory(path)) {
      throw new IOException(
          "Path exists but is not a directory: " + dir.getAbsolutePath());
    }
    Files.createDirectories(path);
  }

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(
          com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  static ObjectMapper mapper() {
    return MAPPER;
  }

  static void printJson(PrintWriter out, PrintWriter err, Map<String, String> fields) {
    try {
      out.println(MAPPER.writeValueAsString(fields));
    } catch (Exception e) {
      err.println("Error writing JSON output");
    }
  }

  static Map<String, String> jsonMap(String... keyValues) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length - 1; i += 2) {
      map.put(keyValues[i], keyValues[i + 1]);
    }
    return map;
  }

  static boolean checkFileExists(File file, String label, PrintWriter err) {
    if (file != null && !file.exists()) {
      err.println(label + " not found: " + file.getPath());
      return false;
    }
    return true;
  }

  /**
   * Read the bytes of a keystore-directory entry, refusing to follow
   * symbolic links and rejecting non-regular files. Returns {@code null}
   * (with a warning to {@code err}) when the entry should be skipped.
   *
   * <p>Unlike {@code Files.readAttributes(...) + MAPPER.readValue(file, ...)},
   * this opens the channel with {@link LinkOption#NOFOLLOW_LINKS} so the
   * {@code O_NOFOLLOW} flag is enforced atomically by the kernel at
   * {@code open(2)} — closing the TOCTOU window between an lstat-style
   * check and a follow-symlink {@code FileInputStream} open. The caller
   * then deserializes the bytes via {@code ObjectMapper.readValue(byte[],
   * Class)}.
   *
   * <p>Files larger than {@link #MAX_KEYSTORE_SIZE} are skipped to bound
   * memory cost when scanning a hostile or oversized directory.
   */
  static byte[] readKeystoreFile(File file, PrintWriter err) {
    Path path = file.toPath();
    BasicFileAttributes attrs;
    try {
      attrs = Files.readAttributes(path, BasicFileAttributes.class,
          LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      err.println("Warning: skipping unreadable file: " + file.getName());
      return null;
    }
    if (attrs.isSymbolicLink()) {
      err.println("Warning: skipping symbolic link: " + file.getName());
      return null;
    }
    if (!attrs.isRegularFile()) {
      err.println("Warning: skipping non-regular file: " + file.getName());
      return null;
    }
    if (attrs.size() > MAX_KEYSTORE_SIZE) {
      err.println("Warning: skipping oversized file (>" + MAX_KEYSTORE_SIZE
          + " bytes): " + file.getName());
      return null;
    }

    int size = (int) attrs.size();
    java.util.Set<OpenOption> openOptions = new HashSet<>();
    openOptions.add(StandardOpenOption.READ);
    openOptions.add(LinkOption.NOFOLLOW_LINKS);
    try (SeekableByteChannel ch = Files.newByteChannel(path, openOptions)) {
      ByteBuffer buf = ByteBuffer.allocate(size);
      while (buf.hasRemaining()) {
        if (ch.read(buf) < 0) {
          break;
        }
      }
      if (buf.position() < size) {
        byte[] actual = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, actual, 0, buf.position());
        return actual;
      }
      return buf.array();
    } catch (IOException e) {
      err.println("Warning: skipping unreadable file: " + file.getName());
      return null;
    }
  }

  static void printSecurityTips(PrintWriter out, String address, String fileName) {
    out.println();
    out.println("Public address of the key:   " + address);
    out.println("Path of the secret key file: " + fileName);
    out.println();
    out.println(
        "- You can share your public address with anyone."
            + " Others need it to interact with you.");
    out.println(
        "- You must NEVER share the secret key with anyone!"
            + " The key controls access to your funds!");
    out.println(
        "- You must BACKUP your key file!"
            + " Without the key, it's impossible to access account funds!");
    out.println(
        "- You must REMEMBER your password!"
            + " Without the password, it's impossible to decrypt the key!");
  }

  /**
   * Check if a WalletFile represents a decryptable V3 keystore.
   * Delegates to {@link Wallet#isValidKeystoreFile(WalletFile)} so the
   * discovery predicate stays in sync with decryption-time validation —
   * a JSON stub with empty or unsupported cipher/KDF is rejected here
   * rather than silently showing up as a "keystore" and failing later.
   */
  static boolean isValidKeystoreFile(WalletFile wf) {
    return org.tron.keystore.Wallet.isValidKeystoreFile(wf);
  }
}
