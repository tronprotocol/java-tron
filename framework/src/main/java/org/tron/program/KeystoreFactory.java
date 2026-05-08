package org.tron.program;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;

@Slf4j(topic = "app")
@Deprecated
public class KeystoreFactory {

  private static final String FilePath = "Wallet";

  public static void start() {
    System.err.println("WARNING: --keystore-factory is deprecated and will be removed "
        + "in a future release.");
    System.err.println("Please use: java -jar Toolkit.jar keystore <command>");
    System.err.println("  keystore new       - Generate a new keystore");
    System.err.println("  keystore import    - Import a private key");
    System.err.println("  keystore list      - List keystores");
    System.err.println("  keystore update    - Change password");
    System.err.println();
    KeystoreFactory cli = new KeystoreFactory();
    cli.run();
  }

  private boolean priKeyValid(String priKey) {
    if (StringUtils.isEmpty(priKey)) {
      logger.warn("Warning: PrivateKey is empty!");
      return false;
    }
    if (priKey.length() != 64) {
      logger.warn("Warning: PrivateKey length needs to be 64, but " + priKey.length() + "!");
      return false;
    }
    //Other rule;
    return true;
  }

  private void fileCheck(File file) throws IOException {
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Creating directory failed!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Creating directory failed!");
          }
        } else {
          throw new IOException("File is already existed and can not be deleted!");
        }
      }
    }
  }


  private void genKeystore() throws CipherException, IOException {
    boolean ecKey = CommonParameter.getInstance().isECKeyCryptoEngine();
    String password = WalletUtils.inputPassword2Twice();

    SignInterface eCkey = SignUtils.getGeneratedRandomSign(Utils.random, ecKey);
    File file = new File(FilePath);
    fileCheck(file);
    String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName),
        ecKey);
    System.out.println("Your address is " + credentials.getAddress());
  }

  private void importPrivateKey() throws CipherException, IOException {
    Scanner in = new Scanner(System.in);
    String privateKey;
    System.out.println("Please input private key.");
    while (true) {
      String input = in.nextLine().trim();
      privateKey = input.split("\\s+")[0];
      if (priKeyValid(privateKey)) {
        break;
      }
      System.out.println("Invalid private key, please input again.");
    }

    String password = WalletUtils.inputPassword2Twice();

    boolean ecKey = CommonParameter.getInstance().isECKeyCryptoEngine();
    SignInterface eCkey = SignUtils.fromPrivate(ByteArray.fromHexString(privateKey), ecKey);
    File file = new File(FilePath);
    fileCheck(file);
    String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName),
        ecKey);
    System.out.println("Your address is " + credentials.getAddress());
  }

  private void help() {
    System.out.println("NOTE: --keystore-factory is deprecated. Use Toolkit.jar instead:");
    System.out.println("  java -jar Toolkit.jar keystore new|import|list|update");
    System.out.println();
    System.out.println("Legacy commands (will be removed):");
    System.out.println("  GenKeystore");
    System.out.println("  ImportPrivateKey");
    System.out.println("  Exit or Quit");
  }

  private void run() {
    Scanner in = new Scanner(System.in);
    help();
    while (in.hasNextLine()) {
      try {
        String cmdLine = in.nextLine().trim();
        String[] cmdArray = cmdLine.split("\\s+");
        // split on trim() string will always return at the minimum: [""]
        String cmd = cmdArray[0];
        if ("".equals(cmd)) {
          continue;
        }
        String cmdLowerCase = cmd.toLowerCase(Locale.ROOT);

        switch (cmdLowerCase) {
          case "help": {
            help();
            break;
          }
          case "genkeystore": {
            genKeystore();
            break;
          }
          case "importprivatekey": {
            importPrivateKey();
            break;
          }
          case "exit":
          case "quit": {
            System.out.println("Exit !!!");
            in.close();
            return;
          }
          default: {
            System.out.println("Invalid cmd: " + cmd);
            help();
          }
        }
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }
  }
}