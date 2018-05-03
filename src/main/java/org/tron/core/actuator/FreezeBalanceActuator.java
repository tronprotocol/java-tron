package org.tron.core.actuator;

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
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class FreezeBalanceActuator extends AbstractActuator {

  FreezeBalanceActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }


  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      FreezeBalanceContract freezeBalanceContract = contract.unpack(FreezeBalanceContract.class);

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(freezeBalanceContract.getOwnerAddress().toByteArray());

      long now = System.currentTimeMillis();
      long duration = freezeBalanceContract.getFrozenDuration() * 24 * 3600 * 1000L;
      Frozen newFrozen = Frozen.newBuilder()
          .setFrozenBalance(freezeBalanceContract.getFrozenBalance())
          .setExpireTime(now + duration)
          .build();

      long newBalance = accountCapsule.getBalance() - freezeBalanceContract.getFrozenBalance();
      long newBandwidth = accountCapsule.getBandwidth() + calculateBandwidth(freezeBalanceContract);
      accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
          .addFrozen(newFrozen)
          .setBalance(newBalance)
          .setBandwidth(newBandwidth)
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

  private long calculateBandwidth(FreezeBalanceContract freezeBalanceContract) {

    return freezeBalanceContract.getFrozenBalance() / 1_000_000L
        * freezeBalanceContract.getFrozenDuration()
        * dbManager.getDynamicPropertiesStore().getBandwidthPerCoinday();
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!contract.is(FreezeBalanceContract.class)) {
        throw new ContractValidateException(
            "contract type error,expected type [FreezeBalanceContract],real type[" + contract
                .getClass() + "]");
      }

      FreezeBalanceContract freezeBalanceContract = this.contract
          .unpack(FreezeBalanceContract.class);
      ByteString ownerAddress = freezeBalanceContract.getOwnerAddress();
      if (!Wallet.addressValid(ownerAddress.toByteArray())) {
        throw new ContractValidateException("Invalidate address");
      }

      if (!dbManager.getAccountStore().has(ownerAddress.toByteArray())) {
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
        throw new ContractValidateException(
            "Account[" + readableOwnerAddress + "] not exists");
      }

      long frozenBalance = freezeBalanceContract.getFrozenBalance();
      if (frozenBalance <= 0) {
        throw new ContractValidateException("frozenBalance must be positive");
      }
      if (frozenBalance < 1_000_000L) {
        throw new ContractValidateException("frozenBalance must be more than 1TRX");
      }

      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ownerAddress.toByteArray());
      if (frozenBalance > accountCapsule.getBalance()) {
        throw new ContractValidateException("frozenBalance must be less than accountBalance");
      }

      long maxFrozenNumber = dbManager.getDynamicPropertiesStore().getMaxFrozenNumber();
      if (accountCapsule.getFrozenCount() >= maxFrozenNumber) {
        throw new ContractValidateException("max frozen number is: " + maxFrozenNumber);
      }

      long frozenDuration = freezeBalanceContract.getFrozenDuration();
      long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
      long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();

      if (!(frozenDuration >= minFrozenTime && frozenDuration <= maxFrozenTime)) {
        throw new ContractValidateException(
            "frozenDuration must be less than " + maxFrozenTime + " days "
                + "and more than " + minFrozenTime + " days");
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ContractValidateException(ex.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(FreezeBalanceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
