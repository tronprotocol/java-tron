package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.tron.common.crypto.SignInterface;
import org.tron.core.exception.CipherException;
import org.tron.keystore.Wallet;
import org.tron.keystore.WalletFile;
import org.tron.keystore.WalletUtils;

/**
 * Shared utilities for keystore CLI commands.
 */
final class KeystoreCliUtils {

  private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(
      PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private static final long MAX_FILE_SIZE = 1024;

  private KeystoreCliUtils() {
  }

  /**
   * Generate a keystore file using temp-file + atomic-rename to avoid
   * a TOCTOU window where the file is world-readable before permissions are set.
   *
   * @return the final keystore file name (not the full path)
   */
  static String generateKeystoreFile(String password, SignInterface keyPair,
      File destDir, boolean useFullScrypt, PrintWriter err)
      throws CipherException, IOException {

    WalletFile walletFile;
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, keyPair);
    } else {
      walletFile = Wallet.createLight(password, keyPair);
    }

    String fileName = WalletUtils.getWalletFileName(walletFile);
    File destination = new File(destDir, fileName);

    File tempFile = File.createTempFile("keystore-", ".tmp", destDir);
    try {
      setOwnerOnly(tempFile, err);
      MAPPER.writeValue(tempFile, walletFile);
      atomicMove(tempFile, destination);
    } catch (Exception e) {
      if (!tempFile.delete()) {
        err.println("Warning: could not delete temp file: " + tempFile.getName());
      }
      throw e;
    }

    return fileName;
  }

  /**
   * Atomic move with fallback for filesystems that don't support it.
   */
  static void atomicMove(File source, File target) throws IOException {
    try {
      Files.move(source.toPath(), target.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
      Files.move(source.toPath(), target.toPath(),
          StandardCopyOption.REPLACE_EXISTING);
    }
  }

  static String readPassword(File passwordFile, PrintWriter err) throws IOException {
    if (passwordFile != null) {
      if (!passwordFile.exists()) {
        err.println("Password file not found: " + passwordFile.getPath()
            + ". Omit --password-file for interactive input.");
        return null;
      }
      if (passwordFile.length() > MAX_FILE_SIZE) {
        err.println("Password file too large (max 1KB).");
        return null;
      }
      byte[] bytes = Files.readAllBytes(passwordFile.toPath());
      try {
        String password = stripLineEndings(
            new String(bytes, StandardCharsets.UTF_8));
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

  static String stripLineEndings(String s) {
    // Strip UTF-8 BOM if present (Windows Notepad adds this)
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

  static boolean checkFileExists(File file, String label, PrintWriter err) {
    if (file != null && !file.exists()) {
      err.println(label + " not found: " + file.getPath());
      return false;
    }
    return true;
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
   * Check if a WalletFile represents a valid V3 keystore.
   */
  static boolean isValidKeystoreFile(WalletFile wf) {
    return wf.getAddress() != null
        && wf.getCrypto() != null
        && wf.getVersion() == 3;
  }

  static void setOwnerOnly(File file, PrintWriter err) {
    try {
      Files.setPosixFilePermissions(file.toPath(), OWNER_ONLY);
    } catch (UnsupportedOperationException | IOException e) {
      err.println("Warning: could not set file permissions on " + file.getName()
          + ". Please manually restrict access to this file.");
    }
  }
}
