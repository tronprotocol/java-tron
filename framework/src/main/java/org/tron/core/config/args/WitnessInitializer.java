package org.tron.core.config.args;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.SignInterface;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.LocalWitnesses;
import org.tron.core.exception.CipherException;
import org.tron.core.exception.TronError;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;

@Slf4j
public class WitnessInitializer {

  /**
   * Init from a single private key (and optional witness address).
   */
  public static LocalWitnesses initFromCLIPrivateKey(
      String privateKey, String witnessAddress) {
    LocalWitnesses witnesses = new LocalWitnesses(privateKey);

    byte[] address = null;
    if (StringUtils.isNotEmpty(witnessAddress)) {
      address = Commons.decodeFromBase58Check(witnessAddress);
      if (address == null) {
        throw new TronError(
            "LocalWitnessAccountAddress format from cmd is incorrect",
            TronError.ErrCode.WITNESS_INIT);
      }
      logger.debug("Got localWitnessAccountAddress from cmd");
    }

    witnesses.initWitnessAccountAddress(
        address, Args.getInstance().isECKeyCryptoEngine());
    logger.debug("Got privateKey from cmd");
    return witnesses;
  }

  /**
   * Init from a list of private keys.
   */
  public static LocalWitnesses initFromCFGPrivateKey(
      List<String> privateKeys, String witnessAccountAddress) {
    LocalWitnesses witnesses = new LocalWitnesses();
    witnesses.setPrivateKeys(privateKeys);
    logger.debug("Got privateKey from config.conf");

    byte[] address = resolveWitnessAddress(witnesses, witnessAccountAddress);
    witnesses.initWitnessAccountAddress(
        address, Args.getInstance().isECKeyCryptoEngine());
    return witnesses;
  }

  /**
   * Init from keystore files with password.
   */
  public static LocalWitnesses initFromKeystore(
      List<String> keystoreFiles, String password,
      String witnessAccountAddress) {
    if (keystoreFiles.size() > 1) {
      logger.warn("Multiple keystores detected. Only the first keystore will be used"
          + " as witness, all others will be ignored.");
    }

    String fileName = System.getProperty("user.dir") + "/" + keystoreFiles.get(0);
    String pwd;
    if (StringUtils.isEmpty(password)) {
      System.out.println("Please input your password.");
      pwd = WalletUtils.inputPassword();
    } else {
      pwd = password;
    }

    List<String> privateKeys = new ArrayList<>();
    try {
      Credentials credentials = WalletUtils.loadCredentials(pwd, new File(fileName));
      SignInterface sign = credentials.getSignInterface();
      String prikey = ByteArray.toHexString(sign.getPrivateKey());
      privateKeys.add(prikey);
    } catch (IOException | CipherException e) {
      logger.error("Witness node start failed!");
      throw new TronError(e, TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
    }

    LocalWitnesses witnesses = new LocalWitnesses();
    witnesses.setPrivateKeys(privateKeys);
    byte[] address = resolveWitnessAddress(witnesses, witnessAccountAddress);
    witnesses.initWitnessAccountAddress(
        address, Args.getInstance().isECKeyCryptoEngine());
    logger.debug("Got privateKey from keystore");
    return witnesses;
  }

  static byte[] resolveWitnessAddress(
      LocalWitnesses witnesses, String witnessAccountAddress) {
    if (StringUtils.isEmpty(witnessAccountAddress)) {
      return null;
    }

    if (witnesses.getPrivateKeys().size() != 1) {
      throw new TronError(
          "LocalWitnessAccountAddress can only be set when there is only one private key",
          TronError.ErrCode.WITNESS_INIT);
    }
    byte[] address = Commons.decodeFromBase58Check(witnessAccountAddress);
    if (address != null) {
      logger.debug("Got localWitnessAccountAddress from config.conf");
    } else {
      throw new TronError("LocalWitnessAccountAddress format from config is incorrect",
          TronError.ErrCode.WITNESS_INIT);
    }
    return address;
  }
}
