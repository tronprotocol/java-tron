package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.FreezeBalanceV2Param;
import org.tron.core.vm.repository.Repository;

@Slf4j(topic = "VMProcessor")
public class FreezeBalanceV2Processor {

  public void validate(FreezeBalanceV2Param param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    if (repo.getDynamicPropertiesStore().getUnfreezeDelayDays() == 0) {
      throw new ContractValidateException("Not support FreezeV2 transaction,"
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
    long frozenBalance = param.getFrozenBalance();
    if (frozenBalance <= 0) {
      throw new ContractValidateException("FrozenBalance must be positive");
    } else if (frozenBalance < TRX_PRECISION) {
      throw new ContractValidateException("FrozenBalance must be more than 1TRX");
    } else if (frozenBalance > ownerCapsule.getBalance()) {
      throw new ContractValidateException("FrozenBalance must be less than accountBalance");
    }

    // validate frozen count of owner account
    int frozenCount = ownerCapsule.getFrozenCount();
    if (frozenCount != 0 && frozenCount != 1) {
      throw new ContractValidateException("FrozenCount must be 0 or 1");
    }

    // validate arg @resourceType
    switch (param.getResourceType()) {
      case BANDWIDTH:
      case ENERGY:
        break;
      case TRON_POWER:
        if (!repo.getDynamicPropertiesStore().supportAllowNewResourceModel()) {
          throw new ContractValidateException(
              "ResourceCode error, valid ResourceCode[BANDWIDTH、ENERGY]");
        }
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error,valid ResourceCode[BANDWIDTH、ENERGY]");
    }
  }

  public void execute(FreezeBalanceV2Param param,  Repository repo) {
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();

    byte[] ownerAddress = param.getOwnerAddress();
    long frozenBalance = param.getFrozenBalance();
    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);
    if (dynamicStore.supportAllowNewResourceModel()
        && accountCapsule.oldTronPowerIsNotInitialized()) {
      accountCapsule.initializeOldTronPower();
    }
    switch (param.getResourceType()) {
      case BANDWIDTH:
        accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
        dynamicStore.addTotalNetWeight(frozenBalance / TRX_PRECISION);
        break;
      case ENERGY:
        accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
        dynamicStore.addTotalEnergyWeight(frozenBalance / TRX_PRECISION);
        break;
      case TRON_POWER:
        accountCapsule.addFrozenForTronPowerV2(frozenBalance);
        dynamicStore.addTotalTronPowerWeight(frozenBalance / TRX_PRECISION);
        break;
      default:
        logger.debug("Resource Code Error.");
    }

    // deduce balance of owner account
    long newBalance = accountCapsule.getBalance() - frozenBalance;
    accountCapsule.setBalance(newBalance);
    repo.updateAccount(accountCapsule.createDbKey(), accountCapsule);
  }
}