package org.tron.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
    // Hoisted out of the try so the legacy-truncation hint in the catch
    // block can inspect whether the user-supplied password contained
    // whitespace (which is the only case truncation can explain).
    String oldPassword = null;
    try {
      File keystoreFile = findKeystoreByAddress(address, err);
      if (keystoreFile == null) {
        // findKeystoreByAddress already prints the specific error
        return 1;
      }

      String newPassword;

      if (passwordFile != null) {
        byte[] bytes = KeystoreCliUtils.readRegularFile(
            passwordFile, 1024, "Password file", err);
        if (bytes == null) {
          return 1;
        }
        try {
          String content = new String(bytes, StandardCharsets.UTF_8);
          // Strip UTF-8 BOM if present (Windows Notepad)
          if (content.length() > 0 && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
          }
          // String.split with the default zero-limit form already drops
          // trailing empty strings, so "old\nnew" and "old\nnew\n" both
          // yield length 2; require strict equality so a stray third line
          // (e.g. someone confusingly providing a confirm line, or the
          // wrong file altogether) is reported rather than silently
          // discarded.
          String[] lines = content.split("\\r?\\n|\\r");
          if (lines.length != 2) {
            err.println("Password file must contain exactly two lines: "
                + "current password on the first line and new password "
                + "on the second line (no confirmation line).");
            return 1;
          }
          oldPassword = WalletUtils.stripPasswordLine(lines[0]);
          newPassword = WalletUtils.stripPasswordLine(lines[1]);
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
      // Re-read via NOFOLLOW byte channel to close the TOCTOU window between
      // findKeystoreByAddress and this read — an attacker with directory
      // write access could otherwise swap the file for a symlink in between.
      byte[] keystoreBytes = KeystoreCliUtils.readKeystoreFile(keystoreFile, err);
      if (keystoreBytes == null) {
        // readKeystoreFile already printed the specific reason
        return 1;
      }
      WalletFile walletFile = MAPPER.readValue(keystoreBytes, WalletFile.class);
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
      // Legacy-truncation hint: keystores created via
      // `FullNode.jar --keystore-factory` in non-TTY mode (e.g.
      // `echo PASS | java ...`) were encrypted with only the first
      // whitespace-separated word of the password due to a bug in the
      // legacy input path. The hint only fires if the provided password
      // actually contains whitespace — otherwise truncation cannot be the
      // cause of the decryption failure and the hint would be noise for
      // the far more common "wrong password" case.
      if (oldPassword != null && oldPassword.matches(".*\\s.*")) {
        err.println("Tip: if this keystore was created with "
            + "`FullNode.jar --keystore-factory` in non-TTY mode, the legacy "
            + "code truncated the password at the first whitespace. "
            + "Try re-running with only the first whitespace-separated word "
            + "of your passphrase as the current password; you can then "
            + "choose the full phrase as the new password.");
      }
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
      byte[] bytes = KeystoreCliUtils.readKeystoreFile(file, err);
      if (bytes == null) {
        continue;
      }
      try {
        WalletFile wf = MAPPER.readValue(bytes, WalletFile.class);
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
