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
import org.tron.protos.Protocol.Account.AccountResource;
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
    final FreezeBalanceContract freezeBalanceContract;
    try {
      freezeBalanceContract = contract.unpack(FreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(freezeBalanceContract.getOwnerAddress().toByteArray());

    long now = dbManager.getHeadBlockTimeStamp();
    long duration = freezeBalanceContract.getFrozenDuration() * 86_400_000;

    long newBalance = accountCapsule.getBalance() - freezeBalanceContract.getFrozenBalance();

    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        long currentFrozenBalance = accountCapsule.getFrozenBalance();
        long newFrozenBalance = freezeBalanceContract.getFrozenBalance() + currentFrozenBalance;

        Frozen newFrozen = Frozen.newBuilder()
            .setFrozenBalance(newFrozenBalance)
            .setExpireTime(now + duration)
            .build();

        long frozenCount = accountCapsule.getFrozenCount();
        if (frozenCount == 0) {
          accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
              .addFrozen(newFrozen)
              .setBalance(newBalance)
              .build());
        } else {
          accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
              .setFrozen(0, newFrozen)
              .setBalance(newBalance)
              .build()
          );
        }
        dbManager.getDynamicPropertiesStore()
            .addTotalNetWeight(freezeBalanceContract.getFrozenBalance() / 1000_000L);
        break;
      case CPU:
        long currentFrozenBalanceForCpu = accountCapsule.getAccountResource()
            .getFrozenBalanceForCpu()
            .getFrozenBalance();
        long newFrozenBalanceForCpu =
            freezeBalanceContract.getFrozenBalance() + currentFrozenBalanceForCpu;

        Frozen newFrozenForCpu = Frozen.newBuilder()
            .setFrozenBalance(newFrozenBalanceForCpu)
            .setExpireTime(now + duration)
            .build();

        AccountResource newAccountResource = accountCapsule.getAccountResource().toBuilder()
            .setFrozenBalanceForCpu(newFrozenForCpu).build();

        accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
            .setAccountResource(newAccountResource)
            .setBalance(newBalance)
            .build());
        dbManager.getDynamicPropertiesStore()
            .addTotalCpuWeight(freezeBalanceContract.getFrozenBalance() / 1000_000L);
        break;
    }

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ret.setStatus(fee, code.SUCESS);

    return true;
  }


  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!contract.is(FreezeBalanceContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [FreezeBalanceContract],real type[" + contract
              .getClass() + "]");
    }

    final FreezeBalanceContract freezeBalanceContract;
    try {
      freezeBalanceContract = this.contract.unpack(FreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = freezeBalanceContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    if (accountCapsule == null) {
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

    int frozenCount = accountCapsule.getFrozenCount();
    if (!(frozenCount == 0 || frozenCount == 1)) {
      throw new ContractValidateException("frozenCount must be 0 or 1");
    }
    if (frozenBalance > accountCapsule.getBalance()) {
      throw new ContractValidateException("frozenBalance must be less than accountBalance");
    }

//    long maxFrozenNumber = dbManager.getDynamicPropertiesStore().getMaxFrozenNumber();
//    if (accountCapsule.getFrozenCount() >= maxFrozenNumber) {
//      throw new ContractValidateException("max frozen number is: " + maxFrozenNumber);
//    }

    long frozenDuration = freezeBalanceContract.getFrozenDuration();
    long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
    long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();

    if (!(frozenDuration >= minFrozenTime && frozenDuration <= maxFrozenTime)) {
      throw new ContractValidateException(
          "frozenDuration must be less than " + maxFrozenTime + " days "
              + "and more than " + minFrozenTime + " days");
    }

    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        break;
      case CPU:
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error,valid ResourceCode[BANDWIDTH、CPU]");
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
