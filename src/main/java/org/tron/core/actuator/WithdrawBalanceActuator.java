package org.tron.core.actuator;

import com.google.common.math.LongMath;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WithdrawBalanceActuator extends AbstractActuator {

  WithdrawBalanceActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }


  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      WithdrawBalanceContract withdrawBalanceContract = contract
          .unpack(WithdrawBalanceContract.class);

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(withdrawBalanceContract.getOwnerAddress().toByteArray());
      long oldBalance = accountCapsule.getBalance();
      long allowance = accountCapsule.getAllowance();

      long now = System.currentTimeMillis();
      accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
          .setBalance(oldBalance + allowance)
          .setAllowance(0L)
          .setLatestWithdrawTime(now)
          .build());
      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!contract.is(WithdrawBalanceContract.class)) {
        throw new ContractValidateException(
            "contract type error,expected type [WithdrawBalanceContract],real type[" + contract
                .getClass() + "]");
      }

      WithdrawBalanceContract withdrawBalanceContract = this.contract
          .unpack(WithdrawBalanceContract.class);
      ByteString ownerAddress = withdrawBalanceContract.getOwnerAddress();
      if (!Wallet.addressValid(ownerAddress.toByteArray())) {
        throw new ContractValidateException("Invalidate address");
      }

      if (!dbManager.getAccountStore().has(ownerAddress.toByteArray())) {
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
        throw new ContractValidateException(
            "Account[" + readableOwnerAddress + "] not exists");
      }

      if (!dbManager.getWitnessStore().has(ownerAddress.toByteArray())) {
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
        throw new ContractValidateException(
            "Account[" + readableOwnerAddress + "] is not a witnessAccount");
      }

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ownerAddress.toByteArray());

      long LatestWithdrawTime = accountCapsule.getLatestWithdrawTime();
      long now = System.currentTimeMillis();
      long witnessAllowanceFrozenTime =
          dbManager.getDynamicPropertiesStore().getWitnessAllowanceFrozenTime() * 24 * 3600 * 1000L;

      if (now - LatestWithdrawTime < witnessAllowanceFrozenTime) {
        throw new ContractValidateException("The last withdraw time is less than 24 hours");
      }

      if (accountCapsule.getAllowance() <= 0) {
        throw new ContractValidateException("witnessAccount does not have any allowance");
      }

      LongMath.checkedAdd(accountCapsule.getBalance(), accountCapsule.getAllowance());

    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ContractValidateException(ex.getMessage());
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
