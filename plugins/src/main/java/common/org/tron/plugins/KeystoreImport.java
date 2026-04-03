package org.tron.plugins;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "import",
    mixinStandardHelpOptions = true,
    description = "Import a private key into a new keystore file.")
public class KeystoreImport implements Callable<Integer> {

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

  @Override
  public Integer call() {
    try {
      KeystoreCliUtils.ensureDirectory(keystoreDir);

      String privateKey = readPrivateKey();
      if (privateKey == null) {
        return 1;
      }

      if (privateKey.startsWith("0x") || privateKey.startsWith("0X")) {
        privateKey = privateKey.substring(2);
      }
      if (!isValidPrivateKey(privateKey)) {
        System.err.println("Invalid private key: must be 64 hex characters.");
        return 1;
      }

      String password = KeystoreCliUtils.readPassword(passwordFile);
      if (password == null) {
        return 1;
      }

      boolean ecKey = !sm2;
      SignInterface keyPair;
      try {
        keyPair = SignUtils.fromPrivate(
            ByteArray.fromHexString(privateKey), ecKey);
      } catch (Exception e) {
        System.err.println("Invalid private key: not a valid key"
            + " for the selected algorithm.");
        return 1;
      }
      String address = Credentials.create(keyPair).getAddress();
      warnIfAddressExists(keystoreDir, address);
      String fileName = WalletUtils.generateWalletFile(password, keyPair, keystoreDir, true);
      KeystoreCliUtils.setOwnerOnly(new File(keystoreDir, fileName));
      if (json) {
        KeystoreCliUtils.printJson(KeystoreCliUtils.jsonMap(
            "address", address, "file", fileName));
      } else {
        System.out.println("Imported keystore: " + fileName);
        System.out.println("Address: " + address);
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

  private String readPrivateKey() throws IOException {
    if (keyFile != null) {
      if (keyFile.length() > 1024) {
        System.err.println("Key file too large (max 1KB).");
        return null;
      }
      byte[] bytes = Files.readAllBytes(keyFile.toPath());
      try {
        return new String(bytes, StandardCharsets.UTF_8).trim();
      } finally {
        Arrays.fill(bytes, (byte) 0);
      }
    }

    Console console = System.console();
    if (console == null) {
      System.err.println("No interactive terminal available. "
          + "Use --key-file to provide private key.");
      return null;
    }

    char[] key = console.readPassword("Enter private key (hex): ");
    if (key == null) {
      System.err.println("Input cancelled.");
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

  private void warnIfAddressExists(File dir, String address) {
    if (!dir.exists() || !dir.isDirectory()) {
      return;
    }
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    if (files == null) {
      return;
    }
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        KeystoreCliUtils.mapper();
    for (File file : files) {
      try {
        org.tron.keystore.WalletFile wf =
            mapper.readValue(file, org.tron.keystore.WalletFile.class);
        if (address.equals(wf.getAddress())) {
          System.err.println("WARNING: keystore for address "
              + address + " already exists: " + file.getName());
          return;
        }
      } catch (Exception e) {
        // Skip invalid files
      }
    }
  }
}
