package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;

@Slf4j(topic = "actuator")
public class DelegateResourceActuator extends AbstractActuator {

  public DelegateResourceActuator() {
    super(ContractType.DelegateResourceContract, DelegateResourceContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    final DelegateResourceContract delegateResourceContract;
    AccountStore accountStore = chainBaseManager.getAccountStore();
    try {
      delegateResourceContract = any.unpack(DelegateResourceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    AccountCapsule ownerCapsule = accountStore
        .get(delegateResourceContract.getOwnerAddress().toByteArray());

    long delegateBalance = delegateResourceContract.getBalance();
    byte[] ownerAddress = delegateResourceContract.getOwnerAddress().toByteArray();
    byte[] receiverAddress = delegateResourceContract.getReceiverAddress().toByteArray();

    // delegate resource to receiver
    switch (delegateResourceContract.getResource()) {
      case BANDWIDTH:
        delegateResource(ownerAddress, receiverAddress, true,
            delegateBalance);

        ownerCapsule.addDelegatedFrozenBalanceForBandwidth(delegateBalance);
        ownerCapsule.addFrozenBalanceForBandwidthV2(-delegateBalance);
        break;
      case ENERGY:
        delegateResource(ownerAddress, receiverAddress, false,
            delegateBalance);

        ownerCapsule.addDelegatedFrozenBalanceForEnergy(delegateBalance);
        ownerCapsule.addFrozenBalanceForEnergyV2(-delegateBalance);
        break;
      default:
        logger.debug("Resource Code Error.");
    }

    accountStore.put(ownerCapsule.createDbKey(), ownerCapsule);

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
    if (!any.is(DelegateResourceContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [DelegateResourceContract],real type["
              + any.getClass() + "]");
    }

    if (!dynamicStore.supportDR()) {
      throw new ContractValidateException("No support for resource delegate");
    }

    if (!dynamicStore.supportUnfreezeDelay()) {
      throw new ContractValidateException("Not support Delegate resource transaction,"
          + " need to be opened by the committee");
    }

    final DelegateResourceContract delegateResourceContract;
    try {
      delegateResourceContract = this.any.unpack(DelegateResourceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = delegateResourceContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule ownerCapsule = accountStore.get(ownerAddress);
    if (ownerCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ActuatorConstant.ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }

    long delegateBalance = delegateResourceContract.getBalance();
    if (delegateBalance < TRX_PRECISION) {
      throw new ContractValidateException("delegateBalance must be more than 1TRX");
    }

    switch (delegateResourceContract.getResource()) {
      case BANDWIDTH: {
        BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);
        processor.updateUsage(ownerCapsule);

        long netUsage = (long) (ownerCapsule.getNetUsage() * TRX_PRECISION * ((double)
                (dynamicStore.getTotalNetWeight()) / dynamicStore.getTotalNetLimit()));

        long ownerNetUsage = (long) (netUsage * ((double)(ownerCapsule
                .getFrozenV2BalanceForBandwidth()) /
                ownerCapsule.getAllFrozenBalanceForBandwidth()));

        if (ownerCapsule.getFrozenV2BalanceForBandwidth() - ownerNetUsage
            < delegateBalance) {
          throw new ContractValidateException(
              "delegateBalance must be less than available FreezeBandwidthV2 balance");
        }
      }
      break;
      case ENERGY: {
        EnergyProcessor processor = new EnergyProcessor(dynamicStore, accountStore);
        processor.updateUsage(ownerCapsule);

        long energyUsage = (long) (ownerCapsule.getEnergyUsage() * TRX_PRECISION * ((double)
                (dynamicStore.getTotalEnergyWeight()) / dynamicStore.getTotalEnergyCurrentLimit()));

        long ownerEnergyUsage = (long) (energyUsage * ((double)(ownerCapsule
                .getFrozenV2BalanceForEnergy()) / ownerCapsule.getAllFrozenBalanceForEnergy()));

        if (ownerCapsule.getFrozenV2BalanceForEnergy() - ownerEnergyUsage < delegateBalance) {
          throw new ContractValidateException(
              "delegateBalance must be less than available FreezeEnergyV2Balance balance");
        }
      }
      break;
      default:
        throw new ContractValidateException(
            "ResourceCode error, valid ResourceCode[BANDWIDTH、ENERGY]");
    }

    byte[] receiverAddress = delegateResourceContract.getReceiverAddress().toByteArray();

    if (ArrayUtils.isEmpty(receiverAddress) || !DecodeUtil.addressValid(receiverAddress)) {
      throw new ContractValidateException("Invalid receiverAddress");
    }


    if (Arrays.equals(receiverAddress, ownerAddress)) {
      throw new ContractValidateException(
          "receiverAddress must not be the same as ownerAddress");
    }

    AccountCapsule receiverCapsule = accountStore.get(receiverAddress);
    if (receiverCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(receiverAddress);
      throw new ContractValidateException(
          ActuatorConstant.ACCOUNT_EXCEPTION_STR
              + readableOwnerAddress + NOT_EXIST_STR);
    }

    if (receiverCapsule.getType() == AccountType.Contract) {
      throw new ContractValidateException(
          "Do not allow delegate resources to contract addresses");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(DelegateResourceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

  private void delegateResource(byte[] ownerAddress, byte[] receiverAddress, boolean isBandwidth,
                                long balance) {
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DelegatedResourceStore delegatedResourceStore = chainBaseManager.getDelegatedResourceStore();
    DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore = chainBaseManager
        .getDelegatedResourceAccountIndexStore();

    //modify DelegatedResourceStore
    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress);
    DelegatedResourceCapsule delegatedResourceCapsule = delegatedResourceStore
        .get(key);
    if (delegatedResourceCapsule == null) {
      delegatedResourceCapsule = new DelegatedResourceCapsule(
          ByteString.copyFrom(ownerAddress),
          ByteString.copyFrom(receiverAddress));
    }
    if (isBandwidth) {
      delegatedResourceCapsule.addFrozenBalanceForBandwidth(balance, 0);
    } else {
      delegatedResourceCapsule.addFrozenBalanceForEnergy(balance, 0);
    }
    delegatedResourceStore.put(key, delegatedResourceCapsule);

    //modify DelegatedResourceAccountIndexStore owner
    byte[] ownerKey = DelegatedResourceAccountIndexCapsule.createDbKeyV2(ownerAddress);
    DelegatedResourceAccountIndexCapsule ownerIndexCapsule = delegatedResourceAccountIndexStore
        .get(ownerKey);
    if (ownerIndexCapsule == null) {
      ownerIndexCapsule = new DelegatedResourceAccountIndexCapsule(
          ByteString.copyFrom(ownerAddress));
    }
    List<ByteString> toAccountsList = ownerIndexCapsule.getToAccountsList();
    if (!toAccountsList.contains(ByteString.copyFrom(receiverAddress))) {
      ownerIndexCapsule.addToAccount(ByteString.copyFrom(receiverAddress));
      delegatedResourceAccountIndexStore.put(ownerKey, ownerIndexCapsule);
    }

    //modify DelegatedResourceAccountIndexStore receiver
    byte[] receiverKey = DelegatedResourceAccountIndexCapsule.createDbKeyV2(receiverAddress);
    DelegatedResourceAccountIndexCapsule receiverIndexCapsule = delegatedResourceAccountIndexStore
        .get(receiverKey);
    if (receiverIndexCapsule == null) {
      receiverIndexCapsule = new DelegatedResourceAccountIndexCapsule(
          ByteString.copyFrom(receiverAddress));
    }
    List<ByteString> fromAccountsList = receiverIndexCapsule
        .getFromAccountsList();
    if (!fromAccountsList.contains(ByteString.copyFrom(ownerAddress))) {
      receiverIndexCapsule.addFromAccount(ByteString.copyFrom(ownerAddress));
      delegatedResourceAccountIndexStore.put(receiverKey, receiverIndexCapsule);
    }

    //modify AccountStore for receiver
    AccountCapsule receiverCapsule = accountStore.get(receiverAddress);
    if (isBandwidth) {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(balance);
    } else {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(balance);
    }
    accountStore.put(receiverCapsule.createDbKey(), receiverCapsule);
  }

}
