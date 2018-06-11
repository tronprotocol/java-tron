package org.tron.program;

import com.beust.jcommander.JCommander;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.keystore.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;


public class KeystoreFactory {

  private static final Logger logger = LoggerFactory.getLogger("KeystoreFactory");
  private static final String FilePath = "Wallet";

  private boolean priKeyValid(String priKey) {
    if (StringUtils.isEmpty(priKey)) {
      logger.warn("Warning: PrivateKey is empty !!");
      return false;
    }
    if (priKey.length() != 64) {
      logger.warn("Warning: PrivateKey length need 64 but " + priKey.length() + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  private void genKeystore() throws CipherException, IOException {
    String password = WalletUtils.inputPassword2Twice();

    ECKey eCkey = new ECKey(Utils.random);
    File file = new File(FilePath);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Make directory faild!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Make directory faild!");
          }
        } else {
          throw new IOException("File is exists and can not delete!");
        }
      }
    }
    String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName));
    System.out.println("Your address is " + credentials.getAddress());
  }

  private void importPrivatekey() throws CipherException, IOException {
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

    ECKey eCkey = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
    File file = new File(FilePath);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Make directory faild!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Make directory faild!");
          }
        } else {
          throw new IOException("File is exists and can not delete!");
        }
      }
    }
    String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName));
    System.out.println("Your address is " + credentials.getAddress());
  }

  private void help() {
    System.out.println("You can enter the following command: ");
    System.out.println("GenKeystore");
    System.out.println("ImportPrivatekey");
    System.out.println("Exit or Quit");
    System.out.println("Input any one of then, you will get more tips.");
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
        String cmdLowerCase = cmd.toLowerCase();

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
            importPrivatekey();
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

  public static void main(String[] args) {
    Args.setParam(args, Constant.TESTNET_CONF);
    KeystoreFactory cli = new KeystoreFactory();

    JCommander.newBuilder()
        .addObject(cli)
        .build()
        .parse(args);

    cli.run();
  }
}