package org.tron.plugins;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletFile;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "import",
    mixinStandardHelpOptions = true,
    description = "Import a private key into a new keystore file.")
public class KeystoreImport implements Callable<Integer> {

  @Spec
  private CommandSpec spec;

  @Option(names = {"--keystore-dir"},
      description = "Keystore directory (default: ./Wallet)",
      defaultValue = "Wallet")
  private File keystoreDir;

  @Option(names = {"--json"},
      description = "Output in JSON format")
  private boolean json;

  @Option(names = {"--key-file"},
      description = "Read private key from file instead of interactive prompt")
  private File keyFile;

  @Option(names = {"--password-file"},
      description = "Read password from file instead of interactive prompt")
  private File passwordFile;

  @Option(names = {"--sm2"},
      description = "Use SM2 algorithm instead of ECDSA")
  private boolean sm2;

  @Option(names = {"--force"},
      description = "Allow import even if address already exists")
  private boolean force;

  @Override
  public Integer call() {
    PrintWriter out = spec.commandLine().getOut();
    PrintWriter err = spec.commandLine().getErr();
    try {
      if (!KeystoreCliUtils.checkFileExists(keyFile, "Key file", err)) {
        return 1;
      }
      KeystoreCliUtils.ensureDirectory(keystoreDir);

      String privateKey = readPrivateKey(err);
      if (privateKey == null) {
        return 1;
      }

      if (privateKey.startsWith("0x") || privateKey.startsWith("0X")) {
        privateKey = privateKey.substring(2);
      }
      if (!isValidPrivateKey(privateKey)) {
        err.println("Invalid private key: must be 64 hex characters.");
        return 1;
      }

      String password = KeystoreCliUtils.readPassword(passwordFile, err);
      if (password == null) {
        return 1;
      }

      boolean ecKey = !sm2;
      SignInterface keyPair;
      try {
        keyPair = SignUtils.fromPrivate(
            ByteArray.fromHexString(privateKey), ecKey);
      } catch (Exception e) {
        err.println("Invalid private key: not a valid key"
            + " for the selected algorithm.");
        return 1;
      }
      String address = Credentials.create(keyPair).getAddress();
      String existingFile = findExistingKeystore(keystoreDir, address, err);
      if (existingFile != null && !force) {
        err.println("Keystore for address " + address
            + " already exists: " + existingFile
            + ". Use --force to import anyway.");
        return 1;
      }
      String fileName = WalletUtils.generateWalletFile(
          password, keyPair, keystoreDir, true);
      if (json) {
        KeystoreCliUtils.printJson(out, err, KeystoreCliUtils.jsonMap(
            "address", address, "file", fileName));
      } else {
        out.println("Imported keystore successfully");
        KeystoreCliUtils.printSecurityTips(out, address,
            new File(keystoreDir, fileName).getPath());
      }
      return 0;
    } catch (CipherException e) {
      err.println("Encryption error: " + e.getMessage());
      return 1;
    } catch (Exception e) {
      err.println("Error: " + e.getMessage());
      return 1;
    }
  }

  private String readPrivateKey(PrintWriter err) throws IOException {
    if (keyFile != null) {
      byte[] bytes = KeystoreCliUtils.readRegularFile(keyFile, 1024, "Key file", err);
      if (bytes == null) {
        return null;
      }
      try {
        return new String(bytes, StandardCharsets.UTF_8).trim();
      } finally {
        Arrays.fill(bytes, (byte) 0);
      }
    }

    Console console = System.console();
    if (console == null) {
      err.println("No interactive terminal available. "
          + "Use --key-file to provide private key.");
      return null;
    }

    char[] key = console.readPassword("Enter private key (hex): ");
    if (key == null) {
      err.println("Input cancelled.");
      return null;
    }
    try {
      return new String(key);
    } finally {
      Arrays.fill(key, '\0');
    }
  }

  private static final java.util.regex.Pattern HEX_PATTERN =
      java.util.regex.Pattern.compile("[0-9a-fA-F]{64}");

  private boolean isValidPrivateKey(String key) {
    return !StringUtils.isEmpty(key) && HEX_PATTERN.matcher(key).matches();
  }

  private String findExistingKeystore(File dir, String address, PrintWriter err) {
    if (!dir.exists() || !dir.isDirectory()) {
      return null;
    }
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    if (files == null) {
      return null;
    }
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        KeystoreCliUtils.mapper();
    for (File file : files) {
      byte[] bytes = KeystoreCliUtils.readKeystoreFile(file, err);
      if (bytes == null) {
        continue;
      }
      try {
        WalletFile wf = mapper.readValue(bytes, WalletFile.class);
        if (KeystoreCliUtils.isValidKeystoreFile(wf)
            && address.equals(wf.getAddress())) {
          return file.getName();
        }
      } catch (Exception e) {
        err.println("Warning: skipping unreadable file: " + file.getName());
      }
    }
    return null;
  }
}
