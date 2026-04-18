package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.tron.common.crypto.SignInterface;
import org.tron.core.exception.CipherException;
import org.tron.keystore.Wallet;
import org.tron.keystore.WalletFile;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "update",
    mixinStandardHelpOptions = true,
    description = "Change the password of a keystore file.")
public class KeystoreUpdate implements Callable<Integer> {

  private static final ObjectMapper MAPPER = KeystoreCliUtils.mapper();
  private static final String INPUT_CANCELLED = "Password input cancelled.";

  @Spec
  private CommandSpec spec;

  @Parameters(index = "0", description = "Address of the keystore to update")
  private String address;

  @Option(names = {"--keystore-dir"},
      description = "Keystore directory (default: ./Wallet)",
      defaultValue = "Wallet")
  private File keystoreDir;

  @Option(names = {"--json"},
      description = "Output in JSON format")
  private boolean json;

  @Option(names = {"--password-file"},
      description = "Read old and new passwords from file (one per line)")
  private File passwordFile;

  @Option(names = {"--sm2"},
      description = "Use SM2 algorithm instead of ECDSA")
  private boolean sm2;

  @Override
  public Integer call() {
    PrintWriter out = spec.commandLine().getOut();
    PrintWriter err = spec.commandLine().getErr();
    try {
      File keystoreFile = findKeystoreByAddress(address, err);
      if (keystoreFile == null) {
        // findKeystoreByAddress already prints the specific error
        return 1;
      }

      String oldPassword;
      String newPassword;

      if (passwordFile != null) {
        if (!passwordFile.exists()) {
          err.println("Password file not found: " + passwordFile.getPath()
              + ". Omit --password-file for interactive input.");
          return 1;
        }
        if (passwordFile.length() > 1024) {
          err.println("Password file too large (max 1KB).");
          return 1;
        }
        byte[] bytes = Files.readAllBytes(passwordFile.toPath());
        try {
          String content = new String(bytes, StandardCharsets.UTF_8);
          // Strip UTF-8 BOM if present (Windows Notepad)
          if (content.length() > 0 && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
          }
          String[] lines = content.split("\\r?\\n|\\r");
          if (lines.length < 2) {
            err.println(
                "Password file must contain old and new passwords"
                    + " on separate lines.");
            return 1;
          }
          oldPassword = KeystoreCliUtils.stripLineEndings(lines[0]);
          newPassword = KeystoreCliUtils.stripLineEndings(lines[1]);
        } finally {
          Arrays.fill(bytes, (byte) 0);
        }
      } else {
        Console console = System.console();
        if (console == null) {
          err.println("No interactive terminal available. "
              + "Use --password-file to provide passwords.");
          return 1;
        }
        char[] oldPwd = console.readPassword("Enter current password: ");
        if (oldPwd == null) {
          err.println(INPUT_CANCELLED);
          return 1;
        }
        char[] newPwd = console.readPassword("Enter new password: ");
        if (newPwd == null) {
          Arrays.fill(oldPwd, '\0');
          err.println(INPUT_CANCELLED);
          return 1;
        }
        char[] confirmPwd = console.readPassword("Confirm new password: ");
        if (confirmPwd == null) {
          Arrays.fill(oldPwd, '\0');
          Arrays.fill(newPwd, '\0');
          err.println(INPUT_CANCELLED);
          return 1;
        }
        try {
          oldPassword = new String(oldPwd);
          newPassword = new String(newPwd);
          String confirmPassword = new String(confirmPwd);
          if (!newPassword.equals(confirmPassword)) {
            err.println("New passwords do not match.");
            return 1;
          }
        } finally {
          Arrays.fill(oldPwd, '\0');
          Arrays.fill(newPwd, '\0');
          Arrays.fill(confirmPwd, '\0');
        }
      }

      // Skip validation on old password: keystore may predate the minimum-length policy
      if (!WalletUtils.passwordValid(newPassword)) {
        err.println("Invalid new password: must be at least 6 characters.");
        return 1;
      }

      boolean ecKey = !sm2;
      WalletFile walletFile = MAPPER.readValue(keystoreFile, WalletFile.class);
      SignInterface keyPair = Wallet.decrypt(oldPassword, walletFile, ecKey);

      // createStandard already sets the correctly-derived address. Do NOT override
      // with walletFile.getAddress() — that would propagate a potentially spoofed
      // address from the JSON.
      WalletFile newWalletFile = Wallet.createStandard(newPassword, keyPair);
      // writeWalletFile does a secure temp-file + atomic rename internally.
      WalletUtils.writeWalletFile(newWalletFile, keystoreFile);

      // Use the derived address from newWalletFile, not walletFile.getAddress().
      // Defense-in-depth: Wallet.decrypt already rejects spoofed addresses, but
      // relying on the derived value keeps this code correct even if that check
      // is ever weakened.
      String verifiedAddress = newWalletFile.getAddress();
      if (json) {
        KeystoreCliUtils.printJson(out, err, KeystoreCliUtils.jsonMap(
            "address", verifiedAddress,
            "file", keystoreFile.getName(),
            "status", "updated"));
      } else {
        out.println("Password updated for: " + verifiedAddress);
      }
      return 0;
    } catch (CipherException e) {
      err.println("Decryption failed: " + e.getMessage());
      return 1;
    } catch (Exception e) {
      err.println("Error: " + e.getMessage());
      return 1;
    }
  }

  private File findKeystoreByAddress(String targetAddress, PrintWriter err) {
    if (!keystoreDir.exists() || !keystoreDir.isDirectory()) {
      err.println("No keystore found for address: " + targetAddress);
      return null;
    }
    File[] files = keystoreDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null) {
      err.println("No keystore found for address: " + targetAddress);
      return null;
    }
    java.util.List<File> matches = new java.util.ArrayList<>();
    for (File file : files) {
      try {
        WalletFile wf = MAPPER.readValue(file, WalletFile.class);
        if (KeystoreCliUtils.isValidKeystoreFile(wf)
            && targetAddress.equals(wf.getAddress())) {
          matches.add(file);
        }
      } catch (Exception e) {
        err.println("Warning: skipping unreadable file: " + file.getName());
      }
    }
    if (matches.size() > 1) {
      err.println("Multiple keystores found for address "
          + targetAddress + ":");
      for (File m : matches) {
        err.println("  " + m.getName());
      }
      err.println("Please remove duplicates and retry.");
      return null;
    }
    if (matches.isEmpty()) {
      err.println("No keystore found for address: " + targetAddress);
      return null;
    }
    return matches.get(0);
  }
}
