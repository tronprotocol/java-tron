package org.tron.core.actuator;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.entity.Dec;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.OraclePrevoteCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StableMarketStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.OracleContract;
import org.tron.protos.contract.OracleContract.OracleExchangeRateVoteContract;


@Slf4j(topic = "actuator")
public class OracleExchangeRateVoteActuator extends AbstractActuator {

  public OracleExchangeRateVoteActuator() {
    super(ContractType.OracleExchangeRateVoteContract, OracleExchangeRateVoteContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    final OracleContract.OracleExchangeRateVoteContract oracleExchangeRateVoteContract;
    final long fee = calcFee();

    StableMarketStore stableMarketStore = chainBaseManager.getStableMarketStore();
    try {
      oracleExchangeRateVoteContract =
          any.unpack(OracleContract.OracleExchangeRateVoteContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, Protocol.Transaction.Result.code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    byte[] srAddress = oracleExchangeRateVoteContract.getSrAddress().toByteArray();

    if (!oracleExchangeRateVoteContract.getPreVoteHash().isEmpty()) {
      // save preVoteInfo
      stableMarketStore.setPrevote(srAddress, new OraclePrevoteCapsule(
          dynamicStore.getLatestBlockHeaderNumber(),
          oracleExchangeRateVoteContract.getPreVoteHash().toByteArray())
      );
    }

    if (oracleExchangeRateVoteContract.hasVote()) {
      // save vote
      stableMarketStore.setVote(srAddress, oracleExchangeRateVoteContract.getVote());
    }

    ret.setStatus(fee, Protocol.Transaction.Result.code.SUCESS);
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();

    final long votePeriod = dynamicStore.getOracleVotePeriod();
    if (votePeriod == 0) {
      throw new ContractValidateException("oracle voting closed");
    }

    final OracleContract.OracleExchangeRateVoteContract oracleExchangeRateVoteContract;
    try {
      oracleExchangeRateVoteContract =
          any.unpack(OracleContract.OracleExchangeRateVoteContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = oracleExchangeRateVoteContract.getOwnerAddress().toByteArray();
    byte[] srAddress = oracleExchangeRateVoteContract.getSrAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid owner address");
    }
    if (!DecodeUtil.addressValid(srAddress)) {
      throw new ContractValidateException("Invalid sr address");
    }

    // TODO replace witnessStore with currently active sr lists
    // check if it is the current sr
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    WitnessCapsule witnessCapsule = witnessStore.get(srAddress);
    if (witnessCapsule == null) {
      throw new ContractValidateException("Not existed witness");
    }

    // check feeder address
    StableMarketStore stableMarketStore = chainBaseManager.getStableMarketStore();
    if (!Arrays.equals(ownerAddress, srAddress)
        && !Arrays.equals(ownerAddress, stableMarketStore.getFeeder(srAddress))) {
      throw new ContractValidateException("Invalid feeder address");
    }


    // check vote with prevote hash of store
    if (oracleExchangeRateVoteContract.hasVote()) {
      String exchangeRateStr =
          oracleExchangeRateVoteContract.getVote().getExchangeRates();

      // check vote exchange rates
      if (!exchangeRateStr.isEmpty()) {
        Map<String, Dec> exchangeRateMap;
        try {
          exchangeRateMap = StableMarketStore.parseExchangeRateTuples(exchangeRateStr);
        } catch (RuntimeException e) {
          logger.debug(e.getMessage(), e);
          throw new ContractValidateException(
              "parse exchange rate string error: " + e.getMessage());
        }

        Map<String, Dec> supportAssets = stableMarketStore.getAllTobinTax();
        if (supportAssets == null) {
          throw new ContractValidateException("asset whitelist is empty");
        }
        // check all assets are in the vote whitelist
        for (Map.Entry<String, Dec> exchangeRate : exchangeRateMap.entrySet()) {
          if (!supportAssets.containsKey(exchangeRate.getKey())) {
            throw new ContractValidateException("unknown vote asset");
          }
        }
      }

      OraclePrevoteCapsule prevote = stableMarketStore.getPrevote(ownerAddress);
      if (prevote == null) {
        throw new ContractValidateException("cannot find prevote");
      }

      long latestBlockNum = dynamicStore.getLatestBlockHeaderNumber();
      // Check prevote is submitted proper period
      if (latestBlockNum / votePeriod - prevote.getInstance().getBlockNum() / votePeriod != 1) {
        throw new ContractValidateException("vote info and prevote mismatch");
      }

      // verify vote with prevote hash
      String salt = oracleExchangeRateVoteContract.getVote().getSalt();
      byte[] voteData = (salt + exchangeRateStr).getBytes();
      voteData = Bytes.concat(voteData, srAddress);
      byte[] hash = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), voteData);
      if (Arrays.equals(prevote.getInstance().getHash().toByteArray(), hash)) {
        throw new ContractValidateException("prevote hash verification failed");

      }
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(OracleContract.OracleExchangeRateVoteContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
