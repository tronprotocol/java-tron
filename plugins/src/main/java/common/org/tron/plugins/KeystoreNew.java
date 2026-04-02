package org.tron.plugins;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "new",
    mixinStandardHelpOptions = true,
    description = "Generate a new keystore file with a random keypair.")
public class KeystoreNew implements Callable<Integer> {

  @Option(names = {"--keystore-dir"},
      description = "Keystore directory (default: ./Wallet)",
      defaultValue = "Wallet")
  private File keystoreDir;

  @Option(names = {"--json"},
      description = "Output in JSON format")
  private boolean json;

  @Option(names = {"--password-file"},
      description = "Read password from file instead of interactive prompt")
  private File passwordFile;

  @Override
  public Integer call() {
    try {
      ensureDirectory(keystoreDir);

      String password = readPassword();
      if (password == null) {
        return 1;
      }

      boolean ecKey = true;
      SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), ecKey);
      String fileName = WalletUtils.generateWalletFile(password, keyPair, keystoreDir, true);
      Credentials credentials = WalletUtils.loadCredentials(password,
          new File(keystoreDir, fileName), ecKey);

      if (json) {
        System.out.printf("{\"address\":\"%s\",\"file\":\"%s\"}%n",
            credentials.getAddress(), fileName);
      } else {
        System.out.println("Generated keystore: " + fileName);
        System.out.println("Address: " + credentials.getAddress());
      }
      return 0;
    } catch (CipherException e) {
      System.err.println("Encryption error: " + e.getMessage());
      return 1;
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      return 1;
    }
  }

  private String readPassword() throws IOException {
    if (passwordFile != null) {
      String password = new String(Files.readAllBytes(passwordFile.toPath()),
          StandardCharsets.UTF_8).trim();
      if (!WalletUtils.passwordValid(password)) {
        System.err.println("Invalid password: must be at least 6 characters.");
        return null;
      }
      return password;
    }

    Console console = System.console();
    if (console == null) {
      System.err.println("No interactive terminal available. "
          + "Use --password-file to provide password.");
      return null;
    }

    char[] pwd1 = console.readPassword("Enter password: ");
    char[] pwd2 = console.readPassword("Confirm password: ");
    String password1 = new String(pwd1);
    String password2 = new String(pwd2);

    if (!password1.equals(password2)) {
      System.err.println("Passwords do not match.");
      return null;
    }
    if (!WalletUtils.passwordValid(password1)) {
      System.err.println("Invalid password: must be at least 6 characters.");
      return null;
    }
    return password1;
  }

  private void ensureDirectory(File dir) throws IOException {
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Cannot create directory: " + dir.getAbsolutePath());
    }
    if (dir.exists() && !dir.isDirectory()) {
      throw new IOException("Path exists but is not a directory: " + dir.getAbsolutePath());
    }
  }
}
