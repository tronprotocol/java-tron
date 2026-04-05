package org.tron.plugins;

import java.io.File;
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

  @Option(names = {"--sm2"},
      description = "Use SM2 algorithm instead of ECDSA")
  private boolean sm2;

  @Override
  public Integer call() {
    try {
      KeystoreCliUtils.ensureDirectory(keystoreDir);

      String password = KeystoreCliUtils.readPassword(passwordFile);
      if (password == null) {
        return 1;
      }

      boolean ecKey = !sm2;
      SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), ecKey);
      String fileName = WalletUtils.generateWalletFile(password, keyPair, keystoreDir, true);
      KeystoreCliUtils.setOwnerOnly(new File(keystoreDir, fileName));

      String address = Credentials.create(keyPair).getAddress();
      if (json) {
        KeystoreCliUtils.printJson(KeystoreCliUtils.jsonMap(
            "address", address, "file", fileName));
      } else {
        System.out.println("Your new key was generated");
        KeystoreCliUtils.printSecurityTips(address,
            new File(keystoreDir, fileName).getPath());
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
}
