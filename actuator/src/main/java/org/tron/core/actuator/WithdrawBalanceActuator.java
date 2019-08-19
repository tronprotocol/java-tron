package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;

import com.google.common.math.LongMath;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DBConfig;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class WithdrawBalanceActuator extends AbstractActuator {

  WithdrawBalanceActuator(Any contract, AccountStore accountStore, DynamicPropertiesStore dynamicPropertiesStore, WitnessStore witnessStore) {
    super(contract,accountStore, dynamicPropertiesStore, witnessStore);
  }


  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    final WithdrawBalanceContract withdrawBalanceContract;
    try {
      withdrawBalanceContract = contract.unpack(WithdrawBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    AccountCapsule accountCapsule = accountStore.
        get(withdrawBalanceContract.getOwnerAddress().toByteArray());
    long oldBalance = accountCapsule.getBalance();
    long allowance = accountCapsule.getAllowance();

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
        .setBalance(oldBalance + allowance)
        .setAllowance(0L)
        .setLatestWithdrawTime(now)
        .build());
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    ret.setWithdrawAmount(allowance);
    ret.setStatus(fee, code.SUCESS);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountStore == null || dynamicStore == null) {
      throw new ContractValidateException("No account store or dynamic store!");
    }
    if (!this.contract.is(WithdrawBalanceContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [WithdrawBalanceContract],real type[" + contract
              .getClass() + "]");
    }
    final WithdrawBalanceContract withdrawBalanceContract;
    try {
      withdrawBalanceContract = this.contract.unpack(WithdrawBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = withdrawBalanceContract.getOwnerAddress().toByteArray();
    if (!Commons.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule =
        accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] not exists");
    }

    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
    if (!witnessStore.has(ownerAddress)) {
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] is not a witnessAccount");
    }

    boolean isGP = DBConfig.getGenesisBlock().getWitnesses().stream().anyMatch(witness ->
        Arrays.equals(ownerAddress, witness.getAddress()));
    if (isGP) {
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress
              + "] is a guard representative and is not allowed to withdraw Balance");
    }

    long latestWithdrawTime = accountCapsule.getLatestWithdrawTime();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    long witnessAllowanceFrozenTime = dynamicStore.getWitnessAllowanceFrozenTime() * 86_400_000L;

    if (now - latestWithdrawTime < witnessAllowanceFrozenTime) {
      throw new ContractValidateException("The last withdraw time is "
          + latestWithdrawTime + ",less than 24 hours");
    }

    if (accountCapsule.getAllowance() <= 0) {
      throw new ContractValidateException("witnessAccount does not have any allowance");
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
    return contract.unpack(WithdrawBalanceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
