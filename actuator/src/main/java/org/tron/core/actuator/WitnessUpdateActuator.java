package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;
import org.tron.core.store.AccountStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class WitnessUpdateActuator extends AbstractActuator {

  WitnessUpdateActuator(Any contract, AccountStore accountStore, WitnessStore witnessStore) {
    super(contract, accountStore, witnessStore);
  }

  private void updateWitness(final WitnessUpdateContract contract) {
    WitnessCapsule witnessCapsule = witnessStore
        .get(contract.getOwnerAddress().toByteArray());
    witnessCapsule.setUrl(contract.getUpdateUrl().toStringUtf8());
    witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final WitnessUpdateContract witnessUpdateContract = this.contract
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
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountStore == null || witnessStore == null) {
      throw new ContractValidateException("No account store or witness store!");
    }
    if (!this.contract.is(WitnessUpdateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [WitnessUpdateContract],real type[" + contract
              .getClass() + "]");
    }
    final WitnessUpdateContract contract;
    try {
      contract = this.contract.unpack(WitnessUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!Commons.addressValid(ownerAddress)) {
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
    return contract.unpack(WitnessUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
