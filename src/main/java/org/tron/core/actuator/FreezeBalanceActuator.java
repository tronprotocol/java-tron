package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
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

    long frozenBalanceForCpu = 0;
    long frozenBalanceForBandwidth = 0;
    long expireTime = now + duration;
    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        frozenBalanceForBandwidth = freezeBalanceContract.getFrozenBalance();
      case CPU:
        frozenBalanceForCpu = freezeBalanceContract.getFrozenBalance();
    }

    byte[] receiverAddress = freezeBalanceContract.getReceiverAddress().toByteArray();

    if (receiverAddress.length == 0) {
      //If the receiver is not included in the contract, the owner will receive the resource.
      long newFrozenBalanceForCpu =
          frozenBalanceForCpu + accountCapsule.getAccountResource()
              .getFrozenBalanceForCpu()
              .getFrozenBalance();
      long newFrozenBalanceForBandwidth =
          frozenBalanceForBandwidth + accountCapsule.getFrozenBalance();

      accountCapsule.setFrozenForCpu(newFrozenBalanceForCpu, expireTime);
      accountCapsule.setFrozenForBandwidth(newFrozenBalanceForBandwidth, expireTime);
    } else {
      //If the receiver is included in the contract, the receiver will receive the resource.
      byte[] key = DelegatedResourceCapsule
          .createDbKey(freezeBalanceContract.getOwnerAddress().toByteArray(),
              freezeBalanceContract.getReceiverAddress().toByteArray());
      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(key);
      if (delegatedResourceCapsule != null) {
        delegatedResourceCapsule
            .addResource(frozenBalanceForBandwidth, frozenBalanceForCpu, expireTime);
      } else {
        delegatedResourceCapsule = new DelegatedResourceCapsule(
            freezeBalanceContract.getOwnerAddress(),
            freezeBalanceContract.getReceiverAddress(),
            frozenBalanceForCpu,
            frozenBalanceForBandwidth,
            expireTime
        );
      }
      dbManager.getDelegatedResourceStore().put(key, delegatedResourceCapsule);

      AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiverAddress);
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForCpu(frozenBalanceForCpu);
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalanceForBandwidth);
      dbManager.getAccountStore().put(receiverCapsule.createDbKey(), receiverCapsule);

      accountCapsule.addDelegatedFrozenBalanceForCpu(frozenBalanceForCpu);
      accountCapsule.addDelegatedFrozenBalanceForBandwidth(frozenBalanceForBandwidth);
    }

    accountCapsule.setBalance(newBalance);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    dbManager.getDynamicPropertiesStore()
        .addTotalNetWeight(freezeBalanceContract.getFrozenBalance() / 1000_000L);

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

    byte[] receiverAddress = freezeBalanceContract.getReceiverAddress().toByteArray();
    //If the receiver is included in the contract, the receiver will receive the resource.
    if (receiverAddress.length != 0) {
      if (Arrays.equals(receiverAddress, ownerAddress)) {
        throw new ContractValidateException(
            "receiverAddress must not be the same as ownerAddress");
      }

      if (!Wallet.addressValid(receiverAddress)) {
        throw new ContractValidateException("Invalid receiverAddress");
      }

      AccountCapsule receiverCapsule = dbManager.getAccountStore().get(receiverAddress);
      if (receiverCapsule == null) {
        String readableOwnerAddress = StringUtil.createReadableString(receiverAddress);
        throw new ContractValidateException(
            "Account[" + readableOwnerAddress + "] not exists");
      }

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
