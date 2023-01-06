package org.tron.core.vm.nativecontract;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.UnDelegateResourceParam;
import org.tron.core.vm.repository.Repository;

import java.util.Arrays;
import java.util.Objects;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

@Slf4j(topic = "VMProcessor")
public class UnDelegateResourceProcessor {

  public void validate(UnDelegateResourceParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    if (!dynamicStore.supportDR()) {
      throw new ContractValidateException("No support for resource delegate");
    }
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
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

    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress, false);
    DelegatedResourceCapsule delegatedResourceCapsule = repo.getDelegatedResource(key);
    if (delegatedResourceCapsule == null) {
      throw new ContractValidateException(
          "delegated Resource does not exist");
    }

    long unDelegateBalance = param.getUnDelegateBalance();
    if (unDelegateBalance <= 0) {
      throw new ContractValidateException("unDelegateBalance must be more than 0 TRX");
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
          throw new ContractValidateException("insufficient delegateFrozenBalance(ENERGY), request="
              + unDelegateBalance + ", balance=" + delegatedResourceCapsule.getFrozenBalanceForEnergy());
        }
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error.valid ResourceCode[BANDWIDTH、ENERGY]");
    }
  }

  public void execute(UnDelegateResourceParam param, Repository repo) {
    byte[] ownerAddress = param.getOwnerAddress();
    byte[] receiverAddress = param.getReceiverAddress();
    long unDelegateBalance = param.getUnDelegateBalance();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    AccountCapsule receiverCapsule = repo.getAccount(receiverAddress);
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    long now = repo.getHeadSlot();

    long transferUsage = 0;
    // modify receiver Account
    if (receiverCapsule != null) {
      switch (param.getResourceType()) {
        case BANDWIDTH:
          BandwidthProcessor bandwidthProcessor = new BandwidthProcessor(ChainBaseManager.getInstance());
          bandwidthProcessor.updateUsageForDelegated(receiverCapsule);

          if (receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForBandwidth()
              < unDelegateBalance) {
            // A TVM contract suicide, re-create will produce this situation
            receiverCapsule.setAcquiredDelegatedFrozenV2BalanceForBandwidth(0);
          } else {
            // calculate usage
            long unDelegateMaxUsage = (long) ((double) unDelegateBalance / TRX_PRECISION
                * dynamicStore.getTotalNetLimit() / repo.getTotalNetWeight());
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
          EnergyProcessor energyProcessor =
              new EnergyProcessor(dynamicStore, ChainBaseManager.getInstance().getAccountStore());
          energyProcessor.updateUsage(receiverCapsule);

          if (receiverCapsule.getAcquiredDelegatedFrozenV2BalanceForEnergy()
              < unDelegateBalance) {
            // A TVM contract receiver, re-create will produce this situation
            receiverCapsule.setAcquiredDelegatedFrozenV2BalanceForEnergy(0);
          } else {
            // calculate usage
            long unDelegateMaxUsage = (long) ((double) unDelegateBalance / TRX_PRECISION
                * dynamicStore.getTotalEnergyCurrentLimit() / repo.getTotalEnergyWeight());
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
      repo.updateAccount(receiverCapsule.createDbKey(), receiverCapsule);
    }

    // modify owner Account
    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress, false);
    DelegatedResourceCapsule delegatedResourceCapsule = repo.getDelegatedResource(key);
    switch (param.getResourceType()) {
      case BANDWIDTH: {
        delegatedResourceCapsule.addFrozenBalanceForBandwidth(-unDelegateBalance, 0);

        ownerCapsule.addDelegatedFrozenV2BalanceForBandwidth(-unDelegateBalance);
        ownerCapsule.addFrozenBalanceForBandwidthV2(unDelegateBalance);

        BandwidthProcessor processor = new BandwidthProcessor(ChainBaseManager.getInstance());
        if (Objects.nonNull(receiverCapsule) && transferUsage > 0) {
          ownerCapsule.setNetUsage(processor.unDelegateIncrease(ownerCapsule, receiverCapsule,
              transferUsage, BANDWIDTH, now));
          ownerCapsule.setLatestConsumeTime(now);
        }
      }
      break;
      case ENERGY: {
        delegatedResourceCapsule.addFrozenBalanceForEnergy(-unDelegateBalance, 0);

        ownerCapsule.addDelegatedFrozenV2BalanceForEnergy(-unDelegateBalance);
        ownerCapsule.addFrozenBalanceForEnergyV2(unDelegateBalance);

        EnergyProcessor processor =
            new EnergyProcessor(dynamicStore, ChainBaseManager.getInstance().getAccountStore());
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

    if (delegatedResourceCapsule.getFrozenBalanceForBandwidth() == 0
        && delegatedResourceCapsule.getFrozenBalanceForEnergy() == 0) {
      //modify DelegatedResourceAccountIndex
      byte[] fromKey = Bytes.concat(
          DelegatedResourceAccountIndexStore.getV2_FROM_PREFIX(), ownerAddress, receiverAddress);
      repo.updateDelegatedResourceAccountIndex(
          fromKey, new DelegatedResourceAccountIndexCapsule(new byte[0]));
      byte[] toKey = Bytes.concat(
          DelegatedResourceAccountIndexStore.getV2_TO_PREFIX(), receiverAddress, ownerAddress);
      repo.updateDelegatedResourceAccountIndex(
          toKey, new DelegatedResourceAccountIndexCapsule(new byte[0]));
    }

    repo.updateDelegatedResource(key, delegatedResourceCapsule);
    repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);
  }
}
