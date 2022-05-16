package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.OracleStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.OracleContract;
import org.tron.protos.contract.OracleContract.DelegateFeedConsentContract;

@Slf4j(topic = "actuator")

public class DelegateFeedConsentActuator extends AbstractActuator {
  public DelegateFeedConsentActuator() {
    super(ContractType.DelegateFeedConsentContract, DelegateFeedConsentContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    final OracleContract.DelegateFeedConsentContract delegateFeedConsentContract;
    final long fee = calcFee();

    OracleStore oracleStore = chainBaseManager.getOracleStore();
    try {
      delegateFeedConsentContract = any.unpack(OracleContract.DelegateFeedConsentContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, Protocol.Transaction.Result.code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    // save feeder address of the sr
    byte[] ownerAddress = delegateFeedConsentContract.getOwnerAddress().toByteArray();
    oracleStore.setFeeder(ownerAddress,
            delegateFeedConsentContract.getFeederAddress().toByteArray());
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

    final OracleContract.DelegateFeedConsentContract delegateFeedConsentContract;
    try {
      delegateFeedConsentContract = any.unpack(OracleContract.DelegateFeedConsentContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = delegateFeedConsentContract.getOwnerAddress().toByteArray();
    byte[] feederAddress = delegateFeedConsentContract.getFeederAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    // Allowed to be empty, means cancel the delegate
    if (!ArrayUtils.isEmpty(feederAddress) && !DecodeUtil.addressValid(feederAddress)) {
      throw new ContractValidateException("Invalid feederAddress");
    }

    AccountStore accountStore = chainBaseManager.getAccountStore();
    AccountCapsule account = accountStore.get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account does not exist");
    }

    WitnessCapsule witnessCapsule = chainBaseManager.getWitnessStore().get(ownerAddress);
    if (witnessCapsule == null) {
      throw new ContractValidateException(
              "Not existed witness:" + Hex.toHexString(ownerAddress));
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(OracleContract.DelegateFeedConsentContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
