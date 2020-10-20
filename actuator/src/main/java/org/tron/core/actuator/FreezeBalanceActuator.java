package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;

@Slf4j(topic = "actuator")
public class FreezeBalanceActuator extends AbstractActuator {

  public FreezeBalanceActuator() {
    super(ContractType.FreezeBalanceContract, FreezeBalanceContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    final FreezeBalanceContract freezeBalanceContract;
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    try {
      freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    AccountCapsule accountCapsule = accountStore
        .get(freezeBalanceContract.getOwnerAddress().toByteArray());

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    long duration = freezeBalanceContract.getFrozenDuration() * FROZEN_PERIOD;

    long newBalance = accountCapsule.getBalance() - freezeBalanceContract.getFrozenBalance();

    long frozenBalance = freezeBalanceContract.getFrozenBalance();
    long expireTime = now + duration;
    byte[] ownerAddress = freezeBalanceContract.getOwnerAddress().toByteArray();
    byte[] receiverAddress = freezeBalanceContract.getReceiverAddress().toByteArray();

    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        if (!ArrayUtils.isEmpty(receiverAddress)
            && dynamicStore.supportDR()) {
          delegateResource(ownerAddress, receiverAddress, true,
              frozenBalance, expireTime);
          accountCapsule.addDelegatedFrozenBalanceForBandwidth(frozenBalance);
        } else {
          long newFrozenBalanceForBandwidth =
              frozenBalance + accountCapsule.getFrozenBalance();
          accountCapsule.setFrozenForBandwidth(newFrozenBalanceForBandwidth, expireTime);
        }
        dynamicStore
            .addTotalNetWeight(frozenBalance / TRX_PRECISION);
        break;
      case ENERGY:
        if (!ArrayUtils.isEmpty(receiverAddress)
            && dynamicStore.supportDR()) {
          delegateResource(ownerAddress, receiverAddress, false,
              frozenBalance, expireTime);
          accountCapsule.addDelegatedFrozenBalanceForEnergy(frozenBalance);
        } else {
          long newFrozenBalanceForEnergy =
              frozenBalance + accountCapsule.getAccountResource()
                  .getFrozenBalanceForEnergy()
                  .getFrozenBalance();
          accountCapsule.setFrozenForEnergy(newFrozenBalanceForEnergy, expireTime);
        }
        dynamicStore
            .addTotalEnergyWeight(frozenBalance / TRX_PRECISION);
        break;
      default:
        logger.debug("Resource Code Error.");
    }

    accountCapsule.setBalance(newBalance);
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);

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
    if (!any.is(FreezeBalanceContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [FreezeBalanceContract],real type[" + any
              .getClass() + "]");
    }

    final FreezeBalanceContract freezeBalanceContract;
    try {
      freezeBalanceContract = this.any.unpack(FreezeBalanceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = freezeBalanceContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ActuatorConstant.ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }

    long frozenBalance = freezeBalanceContract.getFrozenBalance();
    if (frozenBalance <= 0) {
      throw new ContractValidateException("frozenBalance must be positive");
    }
    if (frozenBalance < TRX_PRECISION) {
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
    long minFrozenTime = dynamicStore.getMinFrozenTime();
    long maxFrozenTime = dynamicStore.getMaxFrozenTime();

    boolean needCheckFrozeTime = CommonParameter.getInstance()
        .getCheckFrozenTime() == 1;//for test
    if (needCheckFrozeTime && !(frozenDuration >= minFrozenTime
        && frozenDuration <= maxFrozenTime)) {
      throw new ContractValidateException(
          "frozenDuration must be less than " + maxFrozenTime + " days "
              + "and more than " + minFrozenTime + " days");
    }

    switch (freezeBalanceContract.getResource()) {
      case BANDWIDTH:
        break;
      case ENERGY:
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error,valid ResourceCode[BANDWIDTH、ENERGY]");
    }

    //todo：need version control and config for delegating resource
    byte[] receiverAddress = freezeBalanceContract.getReceiverAddress().toByteArray();
    //If the receiver is included in the contract, the receiver will receive the resource.
    if (!ArrayUtils.isEmpty(receiverAddress) && dynamicStore.supportDR()) {
      if (Arrays.equals(receiverAddress, ownerAddress)) {
        throw new ContractValidateException(
            "receiverAddress must not be the same as ownerAddress");
      }

      if (!DecodeUtil.addressValid(receiverAddress)) {
        throw new ContractValidateException("Invalid receiverAddress");
      }

      AccountCapsule receiverCapsule = accountStore.get(receiverAddress);
      if (receiverCapsule == null) {
        String readableOwnerAddress = StringUtil.createReadableString(receiverAddress);
        throw new ContractValidateException(
            ActuatorConstant.ACCOUNT_EXCEPTION_STR
                + readableOwnerAddress + NOT_EXIST_STR);
      }

      if (dynamicStore.getAllowTvmConstantinople() == 1
          && receiverCapsule.getType() == AccountType.Contract) {
        throw new ContractValidateException(
            "Do not allow delegate resources to contract addresses");

      }

    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(FreezeBalanceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

  private void delegateResource(byte[] ownerAddress, byte[] receiverAddress, boolean isBandwidth,
      long balance, long expireTime) {
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DelegatedResourceStore delegatedResourceStore = chainBaseManager.getDelegatedResourceStore();
    DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore = chainBaseManager
        .getDelegatedResourceAccountIndexStore();
    byte[] key = DelegatedResourceCapsule.createDbKey(ownerAddress, receiverAddress);
    //modify DelegatedResourceStore
    DelegatedResourceCapsule delegatedResourceCapsule = delegatedResourceStore
        .get(key);
    if (delegatedResourceCapsule != null) {
      if (isBandwidth) {
        delegatedResourceCapsule.addFrozenBalanceForBandwidth(balance, expireTime);
      } else {
        delegatedResourceCapsule.addFrozenBalanceForEnergy(balance, expireTime);
      }
    } else {
      delegatedResourceCapsule = new DelegatedResourceCapsule(
          ByteString.copyFrom(ownerAddress),
          ByteString.copyFrom(receiverAddress));
      if (isBandwidth) {
        delegatedResourceCapsule.setFrozenBalanceForBandwidth(balance, expireTime);
      } else {
        delegatedResourceCapsule.setFrozenBalanceForEnergy(balance, expireTime);
      }

    }
    delegatedResourceStore.put(key, delegatedResourceCapsule);

    //modify DelegatedResourceAccountIndexStore
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule = delegatedResourceAccountIndexStore
          .get(ownerAddress);
      if (delegatedResourceAccountIndexCapsule == null) {
        delegatedResourceAccountIndexCapsule = new DelegatedResourceAccountIndexCapsule(
            ByteString.copyFrom(ownerAddress));
      }
      List<ByteString> toAccountsList = delegatedResourceAccountIndexCapsule.getToAccountsList();
      if (!toAccountsList.contains(ByteString.copyFrom(receiverAddress))) {
        delegatedResourceAccountIndexCapsule.addToAccount(ByteString.copyFrom(receiverAddress));
      }
      delegatedResourceAccountIndexStore
          .put(ownerAddress, delegatedResourceAccountIndexCapsule);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule = delegatedResourceAccountIndexStore
          .get(receiverAddress);
      if (delegatedResourceAccountIndexCapsule == null) {
        delegatedResourceAccountIndexCapsule = new DelegatedResourceAccountIndexCapsule(
            ByteString.copyFrom(receiverAddress));
      }
      List<ByteString> fromAccountsList = delegatedResourceAccountIndexCapsule
          .getFromAccountsList();
      if (!fromAccountsList.contains(ByteString.copyFrom(ownerAddress))) {
        delegatedResourceAccountIndexCapsule.addFromAccount(ByteString.copyFrom(ownerAddress));
      }
      delegatedResourceAccountIndexStore
          .put(receiverAddress, delegatedResourceAccountIndexCapsule);
    }

    //modify AccountStore
    AccountCapsule receiverCapsule = accountStore.get(receiverAddress);
    if (isBandwidth) {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(balance);
    } else {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(balance);
    }

    accountStore.put(receiverCapsule.createDbKey(), receiverCapsule);
  }

}
