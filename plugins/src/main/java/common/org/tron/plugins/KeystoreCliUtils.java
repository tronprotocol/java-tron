package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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

  static String readPassword(File passwordFile) throws IOException {
    if (passwordFile != null) {
      if (!passwordFile.exists()) {
        System.err.println("Password file not found: " + passwordFile.getPath()
            + ". Omit --password-file for interactive input.");
        return null;
      }
      if (passwordFile.length() > MAX_FILE_SIZE) {
        System.err.println("Password file too large (max 1KB).");
        return null;
      }
      byte[] bytes = Files.readAllBytes(passwordFile.toPath());
      try {
        String password = stripLineEndings(
            new String(bytes, StandardCharsets.UTF_8));
        if (!WalletUtils.passwordValid(password)) {
          System.err.println("Invalid password: must be at least 6 characters.");
          return null;
        }
        return password;
      } finally {
        Arrays.fill(bytes, (byte) 0);
      }
    }

    Console console = System.console();
    if (console == null) {
      System.err.println("No interactive terminal available. "
          + "Use --password-file to provide password.");
      return null;
    }

    char[] pwd1 = console.readPassword("Enter password: ");
    if (pwd1 == null) {
      System.err.println("Password input cancelled.");
      return null;
    }
    char[] pwd2 = console.readPassword("Confirm password: ");
    if (pwd2 == null) {
      Arrays.fill(pwd1, '\0');
      System.err.println("Password input cancelled.");
      return null;
    }
    try {
      if (!Arrays.equals(pwd1, pwd2)) {
        System.err.println("Passwords do not match.");
        return null;
      }
      String password = new String(pwd1);
      if (!WalletUtils.passwordValid(password)) {
        System.err.println("Invalid password: must be at least 6 characters.");
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

  static void printJson(Map<String, String> fields) {
    try {
      System.out.println(MAPPER.writeValueAsString(fields));
    } catch (Exception e) {
      System.err.println("Error writing JSON output");
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

  static boolean checkFileExists(File file, String label) {
    if (file != null && !file.exists()) {
      System.err.println(label + " not found: " + file.getPath());
      return false;
    }
    return true;
  }

  static void printSecurityTips(String address, String fileName) {
    System.out.println();
    System.out.println("Public address of the key:   " + address);
    System.out.println("Path of the secret key file: " + fileName);
    System.out.println();
    System.out.println(
        "- You can share your public address with anyone."
            + " Others need it to interact with you.");
    System.out.println(
        "- You must NEVER share the secret key with anyone!"
            + " The key controls access to your funds!");
    System.out.println(
        "- You must BACKUP your key file!"
            + " Without the key, it's impossible to access account funds!");
    System.out.println(
        "- You must REMEMBER your password!"
            + " Without the password, it's impossible to decrypt the key!");
  }

  static void setOwnerOnly(File file) {
    try {
      Files.setPosixFilePermissions(file.toPath(), OWNER_ONLY);
    } catch (UnsupportedOperationException e) {
      // Windows — skip
    } catch (IOException e) {
      System.err.println("Warning: could not set file permissions on " + file.getName());
    }
  }
}
