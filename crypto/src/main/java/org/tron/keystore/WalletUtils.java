package org.tron.keystore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.SignInterface;
import org.tron.core.exception.CipherException;

/**
 * Utility functions for working with Wallet files.
 */
@Slf4j(topic = "keystore")
public class WalletUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final Set<PosixFilePermission> OWNER_ONLY =
      Collections.unmodifiableSet(EnumSet.of(
          PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

  static {
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static String generateWalletFile(
      String password, SignInterface ecKeyPair, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException {

    WalletFile walletFile;
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, ecKeyPair);
    } else {
      walletFile = Wallet.createLight(password, ecKeyPair);
    }

    String fileName = getWalletFileName(walletFile);
    File destination = new File(destinationDirectory, fileName);
    writeWalletFile(walletFile, destination);

    return fileName;
  }

  /**
   * Write a WalletFile to the given destination path with owner-only (0600)
   * permissions, using a temp file + atomic rename.
   *
   * <p>On POSIX filesystems, the temp file is created atomically with 0600
   * permissions via {@link Files#createTempFile(Path, String, String,
   * java.nio.file.attribute.FileAttribute[])}, avoiding any window where the
   * file is world-readable.
   *
   * <p>On non-POSIX filesystems (e.g. Windows) the fallback uses
   * {@link File#setReadable(boolean, boolean)} /
   * {@link File#setWritable(boolean, boolean)} which is best-effort — these
   * methods manipulate only DOS-style attributes on Windows and may not update
   * file ACLs. The sensitive keystore JSON is written only after the narrowing
   * calls, so no confidential data is exposed during the window, but callers
   * on Windows should not infer strict owner-only ACL enforcement from this.
   *
   * @param walletFile  the keystore to serialize
   * @param destination the final target file (existing file will be replaced)
   */
  public static void writeWalletFile(WalletFile walletFile, File destination)
      throws IOException {
    Path dir = destination.getAbsoluteFile().getParentFile().toPath();
    Files.createDirectories(dir);

    Path tmp;
    try {
      tmp = Files.createTempFile(dir, "keystore-", ".tmp",
          PosixFilePermissions.asFileAttribute(OWNER_ONLY));
    } catch (UnsupportedOperationException e) {
      // Windows / non-POSIX fallback — best-effort narrowing only (see JavaDoc)
      tmp = Files.createTempFile(dir, "keystore-", ".tmp");
      File tf = tmp.toFile();
      tf.setReadable(false, false);
      tf.setReadable(true, true);
      tf.setWritable(false, false);
      tf.setWritable(true, true);
    }

    try {
      objectMapper.writeValue(tmp.toFile(), walletFile);
      try {
        Files.move(tmp, destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException suppress) {
        e.addSuppressed(suppress);
      }
      throw e;
    }
  }

  public static Credentials loadCredentials(String password, File source, boolean ecKey)
      throws IOException, CipherException {
    warnIfSymbolicLink(source);
    WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    return Credentials.create(Wallet.decrypt(password, walletFile, ecKey));
  }

  /**
   * Emit a warning if {@code source} is a symbolic link. The keystore is still
   * read (following the symlink), preserving compatibility with legitimate
   * deployments that use symlinks to organize keystore files (e.g.
   * {@code /opt/tron/keystore/witness.json} -> {@code /mnt/encrypted/...},
   * container volume-mount paths). The warning gives operators a chance to
   * notice if a path they did not expect to be a symlink has become one — for
   * example if an attacker with config-injection ability has redirected the
   * SR startup keystore. This mirrors how Ethereum consensus clients (e.g.
   * Lighthouse) handle a configured {@code voting_keystore_path}.
   */
  private static void warnIfSymbolicLink(File source) {
    try {
      BasicFileAttributes attrs = Files.readAttributes(source.toPath(),
          BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (attrs.isSymbolicLink()) {
        logger.warn("Keystore file is a symbolic link: {} — proceeding, "
            + "but verify the symlink target points where you expect.",
            source.getPath());
      }
    } catch (IOException ignored) {
      // If we can't stat, let the subsequent readValue surface the real error.
    }
  }

  public static String getWalletFileName(WalletFile walletFile) {
    DateTimeFormatter format = DateTimeFormatter.ofPattern(
        "'UTC--'yyyy-MM-dd'T'HH-mm-ss.nVV'--'");
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    return now.format(format) + walletFile.getAddress() + ".json";
  }

  /**
   * Strip trailing line terminators ({@code \n}/{@code \r}) and a leading
   * UTF-8 BOM ({@code \uFEFF}) from a line of input. Unlike
   * {@link String#trim()} this preserves internal whitespace, so passwords
   * containing spaces (e.g. passphrases) survive intact.
   *
   * <p>Intended as the canonical helper for normalizing raw user-provided
   * password/line input across both CLI console and file-driven paths.
   * Returns {@code null} if the input is {@code null}.
   */
  public static String stripPasswordLine(String s) {
    if (s == null) {
      return null;
    }
    if (s.length() > 0 && s.charAt(0) == '\uFEFF') {
      s = s.substring(1);
    }
    int end = s.length();
    while (end > 0) {
      char c = s.charAt(end - 1);
      if (c == '\n' || c == '\r') {
        end--;
      } else {
        break;
      }
    }
    return s.substring(0, end);
  }

  public static boolean passwordValid(String password) {
    if (StringUtils.isEmpty(password)) {
      return false;
    }
    if (password.length() < 6) {
      return false;
    }
    //Other rule;
    return true;
  }

  /**
   * Lazily-initialized Scanner shared across successive
   * {@link #inputPassword()} calls on the non-TTY path so that
   * {@link #inputPassword2Twice()} can read two lines in sequence
   * without losing data. Each call to {@code new Scanner(System.in)}
   * internally buffers bytes from the underlying {@link BufferedReader};
   * constructing a second Scanner after the first has been discarded
   * drops any buffered bytes the first pulled from stdin, causing
   * {@code NoSuchElementException}.
   */
  private static Scanner sharedStdinScanner;

  /**
   * Visible for testing: reset the cached Scanner so subsequent calls
   * see a freshly rebound {@link System#in}.
   */
  static synchronized void resetSharedStdinScanner() {
    sharedStdinScanner = null;
  }

  private static synchronized Scanner getSharedStdinScanner() {
    if (sharedStdinScanner == null) {
      sharedStdinScanner = new Scanner(System.in);
    }
    return sharedStdinScanner;
  }

  public static String inputPassword() {
    String password;
    Console cons = System.console();
    Scanner in = cons == null ? getSharedStdinScanner() : null;
    while (true) {
      if (cons != null) {
        char[] pwd = cons.readPassword("password: ");
        password = String.valueOf(pwd);
      } else {
        // Preserve the full password including embedded whitespace.
        // The previous implementation applied trim() + split("\\s+")[0]
        // which silently truncated passwords like "correct horse battery
        // staple" to "correct" when piped via stdin (e.g. echo ... | java).
        // stripPasswordLine only removes the UTF-8 BOM and trailing line
        // terminators — internal whitespace is part of the password.
        password = stripPasswordLine(in.nextLine());
      }
      if (passwordValid(password)) {
        return password;
      }
      System.out.println("Invalid password, please input again.");
    }
  }

  public static String inputPassword2Twice() {
    String password0;
    while (true) {
      System.out.println("Please input password.");
      password0 = inputPassword();
      System.out.println("Please input password again.");
      String password1 = inputPassword();
      if (password0.equals(password1)) {
        break;
      }
      System.out.println("Two passwords do not match, please input again.");
    }
    return password0;
  }
}
