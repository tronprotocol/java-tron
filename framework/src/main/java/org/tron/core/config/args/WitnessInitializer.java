package org.tron.core.config.args;

import com.typesafe.config.Config;
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
import org.tron.core.Constant;
import org.tron.core.exception.CipherException;
import org.tron.core.exception.TronError;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;

@Slf4j
public class WitnessInitializer {

  private final Config config;

  private LocalWitnesses localWitnesses;

  public WitnessInitializer(Config config) {
    this.config = config;
    this.localWitnesses = new LocalWitnesses();
  }

  public LocalWitnesses initLocalWitnesses() {
    if (!Args.PARAMETER.isWitness()) {
      return localWitnesses;
    }

    if (tryInitFromCommandLine()) {
      return localWitnesses;
    }

    if (tryInitFromConfig()) {
      return localWitnesses;
    }

    tryInitFromKeystore();

    return localWitnesses;
  }

  private boolean tryInitFromCommandLine() {
    if (StringUtils.isBlank(Args.PARAMETER.privateKey)) {
      return false;
    }

    byte[] witnessAddress = null;
    this.localWitnesses = new LocalWitnesses(Args.PARAMETER.privateKey);
    if (StringUtils.isNotEmpty(Args.PARAMETER.witnessAddress)) {
      witnessAddress = Commons.decodeFromBase58Check(Args.PARAMETER.witnessAddress);
      if (witnessAddress == null) {
        throw new TronError("LocalWitnessAccountAddress format from cmd is incorrect",
            TronError.ErrCode.WITNESS_INIT);
      }
      logger.debug("Got localWitnessAccountAddress from cmd");
    }

    this.localWitnesses.initWitnessAccountAddress(witnessAddress,
        Args.PARAMETER.isECKeyCryptoEngine());
    logger.debug("Got privateKey from cmd");
    return true;
  }

  private boolean tryInitFromConfig() {
    if (!config.hasPath(Constant.LOCAL_WITNESS) || config.getStringList(Constant.LOCAL_WITNESS)
        .isEmpty()) {
      return false;
    }

    List<String> localWitness = config.getStringList(Constant.LOCAL_WITNESS);
    this.localWitnesses.setPrivateKeys(localWitness);
    logger.debug("Got privateKey from config.conf");
    byte[] witnessAddress = getWitnessAddress();
    this.localWitnesses.initWitnessAccountAddress(witnessAddress,
        Args.PARAMETER.isECKeyCryptoEngine());
    return true;
  }

  private void tryInitFromKeystore() {
    if (!config.hasPath(Constant.LOCAL_WITNESS_KEYSTORE)
        || config.getStringList(Constant.LOCAL_WITNESS_KEYSTORE).isEmpty()) {
      return;
    }

    List<String> localWitness = config.getStringList(Constant.LOCAL_WITNESS_KEYSTORE);
    if (localWitness.size() > 1) {
      logger.warn(
          "Multiple keystores detected. Only the first keystore will be used as witness, all "
              + "others will be ignored.");
    }

    List<String> privateKeys = new ArrayList<>();
    String fileName = System.getProperty("user.dir") + "/" + localWitness.get(0);
    String password;
    if (StringUtils.isEmpty(Args.PARAMETER.password)) {
      System.out.println("Please input your password.");
      password = WalletUtils.inputPassword();
    } else {
      password = Args.PARAMETER.password;
      Args.PARAMETER.password = null;
    }

    try {
      Credentials credentials = WalletUtils
          .loadCredentials(password, new File(fileName));
      SignInterface sign = credentials.getSignInterface();
      String prikey = ByteArray.toHexString(sign.getPrivateKey());
      privateKeys.add(prikey);
    } catch (IOException | CipherException e) {
      logger.error("Witness node start failed!");
      throw new TronError(e, TronError.ErrCode.WITNESS_KEYSTORE_LOAD);
    }

    this.localWitnesses.setPrivateKeys(privateKeys);
    byte[] witnessAddress = getWitnessAddress();
    this.localWitnesses.initWitnessAccountAddress(witnessAddress,
        Args.PARAMETER.isECKeyCryptoEngine());
    logger.debug("Got privateKey from keystore");
  }

  private byte[] getWitnessAddress() {
    if (!config.hasPath(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS)) {
      return null;
    }

    if (localWitnesses.getPrivateKeys().size() != 1) {
      throw new TronError(
          "LocalWitnessAccountAddress can only be set when there is only one private key",
          TronError.ErrCode.WITNESS_INIT);
    }
    byte[] witnessAddress = Commons
        .decodeFromBase58Check(config.getString(Constant.LOCAL_WITNESS_ACCOUNT_ADDRESS));
    if (witnessAddress != null) {
      logger.debug("Got localWitnessAccountAddress from config.conf");
    } else {
      throw new TronError("LocalWitnessAccountAddress format from config is incorrect",
          TronError.ErrCode.WITNESS_INIT);
    }
    return witnessAddress;
  }
}
