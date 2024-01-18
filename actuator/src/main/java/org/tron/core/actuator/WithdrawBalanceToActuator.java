package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;

import com.google.common.math.LongMath;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.service.MortgageService;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceToContract;

@Slf4j(topic = "actuator")
public class WithdrawBalanceToActuator extends AbstractActuator {

  public WithdrawBalanceToActuator() {
    super(ContractType.WithdrawBalanceToContract, WithdrawBalanceToContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    final WithdrawBalanceToContract withdrawBalanceToContract;
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    MortgageService mortgageService = chainBaseManager.getMortgageService();
    try {
      withdrawBalanceToContract = any.unpack(WithdrawBalanceToContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    mortgageService.withdrawReward(withdrawBalanceToContract.getOwnerAddress()
        .toByteArray());

    AccountCapsule ownerCapsule = accountStore.
        get(withdrawBalanceToContract.getOwnerAddress().toByteArray());

    AccountCapsule receiverCapsule = accountStore.
        get(withdrawBalanceToContract.getReceiverAddress().toByteArray());
    long receiverBalance = receiverCapsule.getBalance();
    long allowance = ownerCapsule.getAllowance();

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
        .setAllowance(0L)
        .setLatestWithdrawTime(now)
        .build());
    receiverCapsule.setInstance(receiverCapsule.getInstance().toBuilder()
        .setBalance(receiverBalance + allowance)
        .setLatestWithdrawToTime(now)
        .build());
    accountStore.put(ownerCapsule.createDbKey(), ownerCapsule);
    accountStore.put(receiverCapsule.createDbKey(), receiverCapsule);
    ret.setWithdrawAmount(allowance);
    ret.setStatus(fee, code.SUCESS);

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
    MortgageService mortgageService = chainBaseManager.getMortgageService();
    if (!this.any.is(WithdrawBalanceToContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [WithdrawBalanceToContract], real type[" + any
              .getClass() + "]");
    }
    final WithdrawBalanceToContract withdrawBalanceToContract;
    try {
      withdrawBalanceToContract = this.any.unpack(WithdrawBalanceToContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = withdrawBalanceToContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid owner address");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }

    byte[] receiverAddress = withdrawBalanceToContract.getReceiverAddress().toByteArray();
    if (!DecodeUtil.addressValid(receiverAddress)) {
      throw new ContractValidateException("Invalid receiver address");
    }

    AccountCapsule receiverCapsule = accountStore.get(receiverAddress);
    if (receiverCapsule == null) {
      String readableReceiverAddress = StringUtil.createReadableString(receiverAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableReceiverAddress + NOT_EXIST_STR);
    }

    boolean isGP = CommonParameter.getInstance()
        .getGenesisBlock().getWitnesses().stream().anyMatch(witness ->
            Arrays.equals(ownerAddress, witness.getAddress()));
    if (isGP) {
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress
              + "] is a guard representative and is not allowed to withdraw Balance");
    }

    long latestWithdrawTime = accountCapsule.getLatestWithdrawTime();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    long witnessAllowanceFrozenTime = dynamicStore.getWitnessAllowanceFrozenTime() * FROZEN_PERIOD;

    if (now - latestWithdrawTime < witnessAllowanceFrozenTime) {
      throw new ContractValidateException("The last withdraw time is "
          + latestWithdrawTime + ", less than 24 hours");
    }

    if (accountCapsule.getAllowance() <= 0 &&
        mortgageService.queryReward(ownerAddress) <= 0) {
      throw new ContractValidateException("witnessAccount does not have any reward");
    }
    try {
      LongMath.checkedAdd(accountCapsule.getBalance(), accountCapsule.getAllowance());
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(WithdrawBalanceToContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
