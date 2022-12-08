package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
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
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;

@Slf4j(topic = "actuator")
public class UnDelegateResourceActuator extends AbstractActuator {

  public UnDelegateResourceActuator() {
    super(ContractType.UnDelegateResourceContract, UnDelegateResourceContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    final UnDelegateResourceContract unDelegateResourceContract;
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    DelegatedResourceStore delegatedResourceStore = chainBaseManager.getDelegatedResourceStore();
    DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore = chainBaseManager
        .getDelegatedResourceAccountIndexStore();
    try {
      unDelegateResourceContract = any.unpack(UnDelegateResourceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }


    final long unDelegateBalance = unDelegateResourceContract.getBalance();
    byte[] ownerAddress = unDelegateResourceContract.getOwnerAddress().toByteArray();
    byte[] receiverAddress = unDelegateResourceContract.getReceiverAddress().toByteArray();

    AccountCapsule receiverCapsule = accountStore.get(receiverAddress);

    long transferUsage = 0;
    // modify receiver Account
    if (receiverCapsule != null) {
      long now = chainBaseManager.getHeadSlot();
      switch (unDelegateResourceContract.getResource()) {
        case BANDWIDTH:
          BandwidthProcessor bandwidthProcessor = new BandwidthProcessor(chainBaseManager);
          bandwidthProcessor.updateUsageForDelegated(receiverCapsule);

          if (receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth()
              < unDelegateBalance) {
            // A TVM contract suicide, re-create will produce this situation
            receiverCapsule.setAcquiredDelegatedFrozenV2BalanceForBandwidth(0);
          } else {
            // calculate usage
            long unDelegateMaxUsage = (long) ((double) unDelegateBalance / TRX_PRECISION
                * ((double) (dynamicStore.getTotalNetLimit()) / dynamicStore.getTotalNetWeight()));
            transferUsage = (long) (receiverCapsule.getNetUsage()
                * ((double) (unDelegateBalance) / receiverCapsule.getAllFrozenBalanceForBandwidth()));
            transferUsage = Math.min(unDelegateMaxUsage, transferUsage);

            receiverCapsule.addAcquiredDelegatedFrozenV2BalanceForBandwidth(-unDelegateBalance);
          }

          long newNetUsage = receiverCapsule.getNetUsage() - transferUsage;
          receiverCapsule.setNetUsage(newNetUsage);
          receiverCapsule.setLatestConsumeTime(now);
          break;
        case ENERGY:
          EnergyProcessor energyProcessor = new EnergyProcessor(dynamicStore, accountStore);
          energyProcessor.updateUsage(receiverCapsule);

          if (receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy()
              < unDelegateBalance) {
            // A TVM contract receiver, re-create will produce this situation
            receiverCapsule.setAcquiredDelegatedFrozenV2BalanceForEnergy(0);
          } else {
            // calculate usage
            long unDelegateMaxUsage = (long) ((double) unDelegateBalance / TRX_PRECISION
                * ((double) (dynamicStore.getTotalEnergyCurrentLimit()) / dynamicStore.getTotalEnergyWeight()));
            transferUsage = (long) (receiverCapsule.getEnergyUsage()
                * ((double) (unDelegateBalance) / receiverCapsule.getAllFrozenBalanceForEnergy()));
            transferUsage = Math.min(unDelegateMaxUsage, transferUsage);

            receiverCapsule.addAcquiredDelegatedFrozenV2BalanceForEnergy(-unDelegateBalance);
          }

          long newEnergyUsage = receiverCapsule.getEnergyUsage() - transferUsage;
          receiverCapsule.setEnergyUsage(newEnergyUsage);
          receiverCapsule.setLatestConsumeTimeForEnergy(now);
          break;
        default:
          //this should never happen
          break;
      }
      accountStore.put(receiverCapsule.createDbKey(), receiverCapsule);
    }

    // transfer lock delegate to unlock
    delegatedResourceStore.unLockExpireResource(ownerAddress, receiverAddress,
        dynamicStore.getLatestBlockHeaderTimestamp());

    byte[] unlockKey = DelegatedResourceCapsule
        .createDbKeyV2(ownerAddress, receiverAddress, false);
    DelegatedResourceCapsule unlockResource = delegatedResourceStore
        .get(unlockKey);

    // modify owner Account
    AccountCapsule ownerCapsule = accountStore.get(ownerAddress);
    switch (unDelegateResourceContract.getResource()) {
      case BANDWIDTH: {
        unlockResource.addFrozenBalanceForBandwidth(-unDelegateBalance, 0);

        ownerCapsule.addDelegatedFrozenV2BalanceForBandwidth(-unDelegateBalance);
        ownerCapsule.addFrozenBalanceForBandwidthV2(unDelegateBalance);

        BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);

        long now = chainBaseManager.getHeadSlot();
        if (Objects.nonNull(receiverCapsule) && transferUsage > 0) {
          ownerCapsule.setNetUsage(processor.unDelegateIncrease(ownerCapsule, receiverCapsule,
              transferUsage, BANDWIDTH, now));
          ownerCapsule.setLatestConsumeTime(now);
        }
      }
      break;
      case ENERGY: {
        unlockResource.addFrozenBalanceForEnergy(-unDelegateBalance, 0);

        ownerCapsule.addDelegatedFrozenV2BalanceForEnergy(-unDelegateBalance);
        ownerCapsule.addFrozenBalanceForEnergyV2(unDelegateBalance);

        EnergyProcessor processor = new EnergyProcessor(dynamicStore, accountStore);

        long now = chainBaseManager.getHeadSlot();
        if (Objects.nonNull(receiverCapsule) && transferUsage > 0) {
          ownerCapsule.setEnergyUsage(processor.unDelegateIncrease(ownerCapsule, receiverCapsule,
              transferUsage, ENERGY, now));
          ownerCapsule.setLatestConsumeTimeForEnergy(now);
        }
      }
      break;
      default:
        //this should never happen
        break;
    }

    if (unlockResource.getFrozenBalanceForBandwidth() == 0
        && unlockResource.getFrozenBalanceForEnergy() == 0) {
      delegatedResourceStore.delete(unlockKey);
      unlockResource = null;
    } else {
      delegatedResourceStore.put(unlockKey, unlockResource);
    }

    byte[] lockKey = DelegatedResourceCapsule
        .createDbKeyV2(ownerAddress, receiverAddress, true);
    DelegatedResourceCapsule lockResource = delegatedResourceStore
        .get(lockKey);
    if (lockResource == null && unlockResource == null) {
      //modify DelegatedResourceAccountIndexStore
      delegatedResourceAccountIndexStore.unDelegateV2(ownerAddress, receiverAddress);
    }

    accountStore.put(ownerAddress, ownerCapsule);

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
    DelegatedResourceStore delegatedResourceStore = chainBaseManager.getDelegatedResourceStore();
    if (!dynamicStore.supportDR()) {
      throw new ContractValidateException("No support for resource delegate");
    }

    if (!dynamicStore.supportUnfreezeDelay()) {
      throw new ContractValidateException("Not support unDelegate resource transaction,"
          + " need to be opened by the committee");
    }

    if (!this.any.is(UnDelegateResourceContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [UnDelegateResourceContract], real type[" + any
              .getClass() + "]");
    }
    final UnDelegateResourceContract unDelegateResourceContract;
    try {
      unDelegateResourceContract = this.any.unpack(UnDelegateResourceContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = unDelegateResourceContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    AccountCapsule ownerCapsule = accountStore.get(ownerAddress);
    if (ownerCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] does not exist");
    }

    byte[] receiverAddress = unDelegateResourceContract.getReceiverAddress().toByteArray();
    if (ArrayUtils.isEmpty(receiverAddress) || !DecodeUtil.addressValid(receiverAddress)) {
      throw new ContractValidateException("Invalid receiverAddress");
    }
    if (Arrays.equals(receiverAddress, ownerAddress)) {
      throw new ContractValidateException(
          "receiverAddress must not be the same as ownerAddress");
    }

    // TVM contract suicide can result in no receiving account
    // AccountCapsule receiverCapsule = accountStore.get(receiverAddress);
    // if (receiverCapsule == null) {
    //   String readableReceiverAddress = StringUtil.createReadableString(receiverAddress);
    //   throw new ContractValidateException(
    //       "Receiver Account[" + readableReceiverAddress + "] does not exist");
    // }

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress, false);
    DelegatedResourceCapsule unlockResourceCapsule = delegatedResourceStore.get(key);
    byte[] lockKey = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress, true);
    DelegatedResourceCapsule lockResourceCapsule = delegatedResourceStore.get(lockKey);
    if (unlockResourceCapsule == null && lockResourceCapsule == null) {
      throw new ContractValidateException(
          "delegated Resource does not exist");
    }

    long unDelegateBalance = unDelegateResourceContract.getBalance();
    if (unDelegateBalance <= 0) {
      throw new ContractValidateException("unDelegateBalance must be more than 0 TRX");
    }
    switch (unDelegateResourceContract.getResource()) {
      case BANDWIDTH: {
        long delegateBalance = 0;
        if (unlockResourceCapsule != null) {
          delegateBalance += unlockResourceCapsule.getFrozenBalanceForBandwidth();
        }
        if (lockResourceCapsule != null
            && lockResourceCapsule.getExpireTimeForBandwidth() < now) {
          delegateBalance += lockResourceCapsule.getFrozenBalanceForBandwidth();
        }
        if (delegateBalance < unDelegateBalance) {
          throw new ContractValidateException(
              "insufficient delegatedFrozenBalance(BANDWIDTH), request="
                  + unDelegateBalance + ", unlock_balance=" + delegateBalance);
        }
      }
      break;
      case ENERGY: {
        long delegateBalance = 0;
        if (unlockResourceCapsule != null) {
          delegateBalance += unlockResourceCapsule.getFrozenBalanceForEnergy();
        }
        if (lockResourceCapsule != null
            && lockResourceCapsule.getExpireTimeForEnergy() < now) {
          delegateBalance += lockResourceCapsule.getFrozenBalanceForEnergy();
        }
        if (delegateBalance < unDelegateBalance) {
          throw new ContractValidateException("insufficient delegateFrozenBalance(Energy), request="
              + unDelegateBalance + ", unlock_balance=" + delegateBalance);
        }
      }
      break;
      default:
        throw new ContractValidateException(
            "ResourceCode error.valid ResourceCode[BANDWIDTH、Energy]");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UnDelegateResourceContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
