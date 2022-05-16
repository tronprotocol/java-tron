package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.WitnessContract;
import org.tron.protos.contract.WitnessContract.UnjailWitnessContract;

import java.util.Objects;

@Slf4j(topic = "actuator")
public class UnjailWitnessActuator extends AbstractActuator {

  public UnjailWitnessActuator() {
    super(ContractType.UnjailWitnessContract, UnjailWitnessContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    try {
      final UnjailWitnessContract contract = this.any.unpack(UnjailWitnessContract.class);
      WitnessStore witnessStore = chainBaseManager.getWitnessStore();
      WitnessCapsule witnessCapsule = witnessStore.get(contract.getWitnessAddress().toByteArray());
      witnessCapsule.setJailedHeight(0);
      witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    } catch (final InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, Protocol.Transaction.Result.code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!this.any.is(UnjailWitnessContract.class)) {
      throw new ContractValidateException(
              "contract type error, expected type [UnjailWitnessContract], real type[" + any
                      .getClass() + "]");
    }
    final UnjailWitnessContract contract;
    try {
      contract = this.any.unpack(UnjailWitnessContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("invalidate ownerAddress");
    }
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException("ownerAddress account does not exist");
    }
    byte[] witnessAddress = contract.getWitnessAddress().toByteArray();
    if (!DecodeUtil.addressValid(witnessAddress)) {
      throw new ContractValidateException("Invalid witness address");
    }
    WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
    if (witnessCapsule == null) {
      throw new ContractValidateException("Witness does not exist");
    }
    if (witnessCapsule.getJailedHeight() == 0) {
      throw new ContractValidateException("Witness already unjailed");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(WitnessContract.UnjailWitnessContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
