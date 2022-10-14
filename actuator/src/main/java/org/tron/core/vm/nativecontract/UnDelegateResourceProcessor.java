package org.tron.core.vm.nativecontract;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.UnDelegateResourceParam;
import org.tron.core.vm.repository.Repository;

import java.util.Arrays;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

@Slf4j(topic = "VMProcessor")
public class UnDelegateResourceProcessor {

  public void validate(UnDelegateResourceParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    if (!dynamicStore.supportDR()) {
      throw new ContractValidateException("No support for resource delegate");
    }
    if (dynamicStore.getUnfreezeDelayDays() == 0) {
      throw new ContractValidateException("Not support Delegate resource transaction,"
          + " need to be opened by the committee");
    }
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    if (ownerCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] does not exist");
    }

    byte[] receiverAddress = param.getReceiverAddress();
    if (ArrayUtils.isEmpty(receiverAddress) || !DecodeUtil.addressValid(receiverAddress)) {
      throw new ContractValidateException("Invalid receiverAddress");
    }
    if (Arrays.equals(receiverAddress, ownerAddress)) {
      throw new ContractValidateException(
          "receiverAddress must not be the same as ownerAddress");
    }
    AccountCapsule receiverCapsule = repo.getAccount(receiverAddress);
    if (receiverCapsule == null) {
     String readableReceiverAddress = StringUtil.createReadableString(receiverAddress);
     throw new ContractValidateException(
         "Receiver Account[" + readableReceiverAddress + "] does not exist");
    }

    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress);
    DelegatedResourceCapsule delegatedResourceCapsule = repo.getDelegatedResource(key);
    if (delegatedResourceCapsule == null) {
      throw new ContractValidateException(
          "delegated Resource does not exist");
    }

    long unDelegateBalance = param.getUnDelegateBalance();
    if (unDelegateBalance < TRX_PRECISION) {
      throw new ContractValidateException("unDelegateBalance must be more than 1TRX");
    }
    switch (param.getResourceType()) {
      case BANDWIDTH:
        if (delegatedResourceCapsule.getFrozenBalanceForBandwidth() < unDelegateBalance) {
          throw new ContractValidateException("insufficient delegatedFrozenBalance(BANDWIDTH), request="
              + unDelegateBalance + ", balance=" + delegatedResourceCapsule.getFrozenBalanceForBandwidth());
        }
        break;
      case ENERGY:
        if (delegatedResourceCapsule.getFrozenBalanceForEnergy() < unDelegateBalance) {
          throw new ContractValidateException("insufficient delegateFrozenBalance(Energy), request="
              + unDelegateBalance + ", balance=" + delegatedResourceCapsule.getFrozenBalanceForEnergy());
        }
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error.valid ResourceCode[BANDWIDTH、Energy]");
    }
  }

  public void execute(UnDelegateResourceParam param,  Repository repo) {
    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(param.getOwnerAddress());
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    long unDelegateBalance = param.getUnDelegateBalance();
    byte[] receiverAddress = param.getReceiverAddress();
    AccountCapsule receiverCapsule = repo.getAccount(receiverAddress);

    long transferUsage = 0;
    // modify receiver Account
    if (receiverCapsule != null) {
      switch (param.getResourceType()) {
        case BANDWIDTH:
          BandwidthProcessor bandwidthProcessor = new BandwidthProcessor(ChainBaseManager.getInstance());
          // todo update usage time?
          bandwidthProcessor.updateUsage(receiverCapsule);

          if (receiverCapsule.getAcquiredDelegatedFrozenBalanceForBandwidth()
              < unDelegateBalance) {
            // A TVM contract suicide, re-create will produce this situation
            receiverCapsule.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
          } else {
            // calculate usage
            long unDelegateMaxUsage = (long) (unDelegateBalance / TRX_PRECISION
                * ((double) (dynamicStore.getTotalNetLimit()) / dynamicStore.getTotalNetWeight()));
            transferUsage = (long) (receiverCapsule.getNetUsage()
                * ((double) (unDelegateBalance) / receiverCapsule.getAllFrozenBalanceForBandwidth()));
            transferUsage = Math.min(unDelegateMaxUsage, transferUsage);

            receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(-unDelegateBalance);
          }

          long newNetUsage = receiverCapsule.getNetUsage() - transferUsage;
          receiverCapsule.setNetUsage(newNetUsage);
          break;
        case ENERGY:
          EnergyProcessor energyProcessor =
              new EnergyProcessor(dynamicStore, ChainBaseManager.getInstance().getAccountStore());
          energyProcessor.updateUsage(receiverCapsule);

          if (receiverCapsule.getAcquiredDelegatedFrozenBalanceForEnergy()
              < unDelegateBalance) {
            // A TVM contract receiver, re-create will produce this situation
            receiverCapsule.setAcquiredDelegatedFrozenBalanceForEnergy(0);
          } else {
            // calculate usage
            long unDelegateMaxUsage = (long) (unDelegateBalance / TRX_PRECISION
                * ((double) (dynamicStore.getTotalEnergyCurrentLimit()) / dynamicStore.getTotalEnergyWeight()));
            transferUsage = (long) (receiverCapsule.getEnergyUsage()
                * ((double) (unDelegateBalance) / receiverCapsule.getAllFrozenBalanceForEnergy()));
            transferUsage = Math.min(unDelegateMaxUsage, transferUsage);

            receiverCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(-unDelegateBalance);
          }

          long newEnergyUsage = receiverCapsule.getEnergyUsage() - transferUsage;
          receiverCapsule.setEnergyUsage(newEnergyUsage);
          break;
        default:
          //this should never happen
          break;
      }
      repo.updateAccount(receiverCapsule.createDbKey(), receiverCapsule);
    }

    // modify owner Account
    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress);
    DelegatedResourceCapsule delegatedResourceCapsule = repo.getDelegatedResource(key);
    switch (param.getResourceType()) {
      case BANDWIDTH: {
        delegatedResourceCapsule.addFrozenBalanceForBandwidth(-unDelegateBalance, 0);

        ownerCapsule.addDelegatedFrozenBalanceForBandwidth(-unDelegateBalance);
        ownerCapsule.addFrozenBalanceForBandwidthV2(unDelegateBalance);

        BandwidthProcessor processor = new BandwidthProcessor(ChainBaseManager.getInstance());
        processor.updateUsage(ownerCapsule);
        long newNetUsage = ownerCapsule.getNetUsage() + transferUsage;
        ownerCapsule.setNetUsage(newNetUsage);
        ownerCapsule.setLatestConsumeTime(ChainBaseManager.getInstance().getHeadSlot());
      }
      break;
      case ENERGY: {
        delegatedResourceCapsule.addFrozenBalanceForEnergy(-unDelegateBalance, 0);

        ownerCapsule.addDelegatedFrozenBalanceForEnergy(-unDelegateBalance);
        ownerCapsule.addFrozenBalanceForEnergyV2(unDelegateBalance);

        EnergyProcessor processor =
            new EnergyProcessor(dynamicStore, ChainBaseManager.getInstance().getAccountStore());
        processor.updateUsage(ownerCapsule);
        long newEnergyUsage = ownerCapsule.getEnergyUsage() + transferUsage;
        ownerCapsule.setEnergyUsage(newEnergyUsage);
        ownerCapsule.setLatestConsumeTimeForEnergy(ChainBaseManager.getInstance().getHeadSlot());
      }
      break;
      default:
        //this should never happen
        break;
    }
    repo.updateDelegatedResource(key, delegatedResourceCapsule);
    repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);
  }
}
