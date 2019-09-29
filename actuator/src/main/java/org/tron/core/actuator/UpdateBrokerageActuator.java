package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;

@Slf4j(topic = "actuator")
public class UpdateBrokerageActuator extends AbstractActuator {

  UpdateBrokerageActuator(Any contract, AccountStore accountStore, WitnessStore witnessStore,
      DynamicPropertiesStore dynamicPropertiesStore, DelegationStore delegationStore) {
    super(contract, accountStore, witnessStore, dynamicPropertiesStore, delegationStore);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    final UpdateBrokerageContract updateBrokerageContract;
    final long fee = calcFee();
    try {
      updateBrokerageContract = contract.unpack(UpdateBrokerageContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    byte[] ownerAddress = updateBrokerageContract.getOwnerAddress().toByteArray();
    int brokerage = updateBrokerageContract.getBrokerage();

    delegationStore.setBrokerage(ownerAddress, brokerage);
    ret.setStatus(fee, code.SUCESS);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!dynamicStore.allowChangeDelegation()) {
      throw new ContractValidateException(
          "contract type error,unexpected type [UpdateBrokerageContract]");
    }
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountStore == null || dynamicStore == null) {
      throw new ContractValidateException("No account store or dynamic store!");
    }
    if (!this.contract.is(UpdateBrokerageContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [UpdateBrokerageContract],real type[" + contract
              .getClass() + "]");
    }
    final UpdateBrokerageContract updateBrokerageContract;
    try {
      updateBrokerageContract = contract.unpack(UpdateBrokerageContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = updateBrokerageContract.getOwnerAddress().toByteArray();
    int brokerage = updateBrokerageContract.getBrokerage();

    if (!Commons.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    if (brokerage < 0 || brokerage > 100) {
      throw new ContractValidateException("Invalid brokerage");
    }

    WitnessCapsule witnessCapsule = witnessStore.get(ownerAddress);
    if (witnessCapsule == null) {
      throw new ContractValidateException("Not exist witness:" + Hex.toHexString(ownerAddress));
    }

    AccountCapsule account = accountStore.get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account has not existed");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(UpdateBrokerageContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}