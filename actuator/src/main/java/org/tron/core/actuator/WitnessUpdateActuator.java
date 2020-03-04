package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;

@Slf4j(topic = "actuator")
public class WitnessUpdateActuator extends AbstractActuator {

  public WitnessUpdateActuator() {
    super(ContractType.WitnessUpdateContract, WitnessUpdateContract.class);
  }

  private void updateWitness(final WitnessUpdateContract contract) {
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    WitnessCapsule witnessCapsule = witnessStore
        .get(contract.getOwnerAddress().toByteArray());
    witnessCapsule.setUrl(contract.getUpdateUrl().toStringUtf8());
    witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    try {
      final WitnessUpdateContract witnessUpdateContract = this.any
          .unpack(WitnessUpdateContract.class);
      this.updateWitness(witnessUpdateContract);
      ret.setStatus(fee, code.SUCESS);
    } catch (final InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or witness store!");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    if (!this.any.is(WitnessUpdateContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [WitnessUpdateContract],real type[" + any
              .getClass() + "]");
    }
    final WitnessUpdateContract contract;
    try {
      contract = this.any.unpack(WitnessUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!accountStore.has(ownerAddress)) {
      throw new ContractValidateException("account does not exist");
    }

    if (!TransactionUtil.validUrl(contract.getUpdateUrl().toByteArray())) {
      throw new ContractValidateException("Invalid url");
    }

    if (!witnessStore.has(ownerAddress)) {
      throw new ContractValidateException("Witness does not exist");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(WitnessUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
