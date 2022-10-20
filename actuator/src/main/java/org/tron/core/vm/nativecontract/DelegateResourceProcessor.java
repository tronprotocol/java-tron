package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.DelegateResourceParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

@Slf4j(topic = "VMProcessor")
public class DelegateResourceProcessor {

  public void validate(DelegateResourceParam param, Repository repo) throws ContractValidateException {
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
          ActuatorConstant.ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }
    long delegateBalance = param.getDelegateBalance();
    if (delegateBalance < TRX_PRECISION) {
      throw new ContractValidateException("delegateBalance must be more than 1TRX");
    }

    switch (param.getResourceType()) {
      case BANDWIDTH: {
        BandwidthProcessor processor = new BandwidthProcessor(ChainBaseManager.getInstance());
        processor.updateUsage(ownerCapsule);

        long netUsage = (long) (ownerCapsule.getNetUsage() * TRX_PRECISION * ((double)
            (repo.getTotalNetWeight()) / dynamicStore.getTotalNetLimit()));

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
          EnergyProcessor processor =
              new EnergyProcessor(dynamicStore, ChainBaseManager.getInstance().getAccountStore());
        processor.updateUsage(ownerCapsule);

        long energyUsage = (long) (ownerCapsule.getEnergyUsage() * TRX_PRECISION * ((double)
            (repo.getTotalEnergyWeight()) / dynamicStore.getTotalEnergyCurrentLimit()));

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
      String readableOwnerAddress = StringUtil.createReadableString(receiverAddress);
      throw new ContractValidateException(
          ActuatorConstant.ACCOUNT_EXCEPTION_STR
              + readableOwnerAddress + NOT_EXIST_STR);
    }
    if (receiverCapsule.getType() == Protocol.AccountType.Contract) {
      throw new ContractValidateException(
          "Do not allow delegate resources to contract addresses");
    }
  }

  public void execute(DelegateResourceParam param, Repository repo) {
    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(param.getOwnerAddress());
    long delegateBalance = param.getDelegateBalance();
    byte[] receiverAddress = param.getReceiverAddress();

    // delegate resource to receiver
    switch (param.getResourceType()) {
      case BANDWIDTH:
        delegateResource(ownerAddress, receiverAddress, true,
            delegateBalance, repo);

        ownerCapsule.addDelegatedFrozenBalanceForBandwidth(delegateBalance);
        ownerCapsule.addFrozenBalanceForBandwidthV2(-delegateBalance);
        break;
      case ENERGY:
        delegateResource(ownerAddress, receiverAddress, false,
            delegateBalance, repo);

        ownerCapsule.addDelegatedFrozenBalanceForEnergy(delegateBalance);
        ownerCapsule.addFrozenBalanceForEnergyV2(-delegateBalance);
        break;
      default:
        logger.debug("Resource Code Error.");
    }

    repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);
  }

  private void delegateResource(
      byte[] ownerAddress,
      byte[] receiverAddress,
      boolean isBandwidth,
      long delegateBalance,
      Repository repo) {
    //modify DelegatedResourceStore
    byte[] key = DelegatedResourceCapsule.createDbKeyV2(ownerAddress, receiverAddress);
    DelegatedResourceCapsule delegatedResourceCapsule = repo.getDelegatedResource(key);
    if (delegatedResourceCapsule == null) {
      delegatedResourceCapsule = new DelegatedResourceCapsule(
          ByteString.copyFrom(ownerAddress),
          ByteString.copyFrom(receiverAddress));
    }
    if (isBandwidth) {
      delegatedResourceCapsule.addFrozenBalanceForBandwidth(delegateBalance, 0);
    } else {
      delegatedResourceCapsule.addFrozenBalanceForEnergy(delegateBalance, 0);
    }

    //update Account for receiver
    AccountCapsule receiverCapsule = repo.getAccount(receiverAddress);
    if (isBandwidth) {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(delegateBalance);
    } else {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(delegateBalance);
    }
    repo.updateDelegatedResource(key, delegatedResourceCapsule);
    repo.updateAccount(receiverCapsule.createDbKey(), receiverCapsule);
  }
}
