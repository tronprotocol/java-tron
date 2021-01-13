package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.WITNESS_EXCEPTION_STR;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;

@Slf4j(topic = "actuator")
public class WitnessCreateActuator extends AbstractActuator {

  public WitnessCreateActuator() {
    super(ContractType.WitnessCreateContract, WitnessCreateContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    try {
      final WitnessCreateContract witnessCreateContract = this.any
          .unpack(WitnessCreateContract.class);
      this.createWitness(witnessCreateContract);
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException | BalanceInsufficientException e) {
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
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    if (!this.any.is(WitnessCreateContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [WitnessCreateContract],real type[" + any
              .getClass() + "]");
    }
    final WitnessCreateContract contract;
    try {
      contract = this.any.unpack(WitnessCreateContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!TransactionUtil.validUrl(contract.getUrl().toByteArray())) {
      throw new ContractValidateException("Invalid url");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);

    if (accountCapsule == null) {
      throw new ContractValidateException("account[" + readableOwnerAddress
          + ActuatorConstant.NOT_EXIST_STR);
    }
    /* todo later
    if (ArrayUtils.isEmpty(accountCapsule.getAccountName().toByteArray())) {
      throw new ContractValidateException("accountStore name not set");
    } */

    if (witnessStore.has(ownerAddress)) {
      throw new ContractValidateException(
          WITNESS_EXCEPTION_STR + readableOwnerAddress + "] has existed");
    }

    if (accountCapsule.getBalance() < dynamicStore
        .getAccountUpgradeCost()) {
      throw new ContractValidateException("balance < AccountUpgradeCost");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(WitnessCreateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return chainBaseManager.getDynamicPropertiesStore().getAccountUpgradeCost();
  }

  private void createWitness(final WitnessCreateContract witnessCreateContract)
      throws BalanceInsufficientException {
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    WitnessStore witnessStore = chainBaseManager.getWitnessStore();
    //Create Witness by witnessCreateContract
    final WitnessCapsule witnessCapsule = new WitnessCapsule(
        witnessCreateContract.getOwnerAddress(),
        0,
        witnessCreateContract.getUrl().toStringUtf8());

    logger.debug("createWitness,address[{}]", witnessCapsule.createReadableString());
    witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    AccountCapsule accountCapsule = accountStore
        .get(witnessCapsule.createDbKey());
    accountCapsule.setIsWitness(true);
    if (dynamicStore.getAllowMultiSign() == 1) {
      accountCapsule.setDefaultWitnessPermission(dynamicStore);
    }
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);
    long cost = dynamicStore.getAccountUpgradeCost();
    Commons
        .adjustBalance(accountStore, witnessCreateContract.getOwnerAddress().toByteArray(), -cost);
    if (dynamicStore.supportBlackHoleOptimization()) {
      dynamicStore.burnTrx(cost);
    } else {
      Commons.adjustBalance(accountStore, accountStore.getBlackhole(), +cost);
    }
    dynamicStore.addTotalCreateWitnessCost(cost);
  }
}
