package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.VMConstant;
import org.tron.core.vm.nativecontract.param.CancelAllUnfreezeV2Param;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

@Slf4j(topic = "VMProcessor")
public class CancelAllUnfreezeV2Processor {

  public void validate(CancelAllUnfreezeV2Param param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);
    if (Objects.isNull(accountCapsule)) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }
  }

  public Map<String, Long> execute(CancelAllUnfreezeV2Param param, Repository repo) throws ContractExeException {
    Map<String, Long> result = new HashMap<>();
    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    long now = repo.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    long withdrawExpireBalance = 0L;
    for (Protocol.Account.UnFreezeV2 unFreezeV2: ownerCapsule.getUnfrozenV2List()) {
      if (unFreezeV2.getUnfreezeExpireTime() > now) {
        String resourceName = unFreezeV2.getType().name();
        result.put(resourceName, result.getOrDefault(resourceName, 0L) + unFreezeV2.getUnfreezeAmount());

        updateFrozenInfoAndTotalResourceWeight(ownerCapsule, unFreezeV2, repo);
      } else {
        // withdraw
        withdrawExpireBalance += unFreezeV2.getUnfreezeAmount();
      }
    }
    if (withdrawExpireBalance > 0) {
      ownerCapsule.setBalance(ownerCapsule.getBalance() + withdrawExpireBalance);
    }
    ownerCapsule.clearUnfrozenV2();

    repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);

    result.put(VMConstant.WITHDRAW_EXPIRE_BALANCE, withdrawExpireBalance);
    return result;
  }

  public void updateFrozenInfoAndTotalResourceWeight(
      AccountCapsule accountCapsule, Protocol.Account.UnFreezeV2 unFreezeV2, Repository repo) {
    switch (unFreezeV2.getType()) {
      case BANDWIDTH:
        long oldNetWeight = accountCapsule.getFrozenV2BalanceWithDelegated(BANDWIDTH) / TRX_PRECISION;
        accountCapsule.addFrozenBalanceForBandwidthV2(unFreezeV2.getUnfreezeAmount());
        long newNetWeight = accountCapsule.getFrozenV2BalanceWithDelegated(BANDWIDTH) / TRX_PRECISION;
        repo.addTotalNetWeight(newNetWeight - oldNetWeight);
        break;
      case ENERGY:
        long oldEnergyWeight = accountCapsule.getFrozenV2BalanceWithDelegated(ENERGY) / TRX_PRECISION;
        accountCapsule.addFrozenBalanceForEnergyV2(unFreezeV2.getUnfreezeAmount());
        long newEnergyWeight = accountCapsule.getFrozenV2BalanceWithDelegated(ENERGY) / TRX_PRECISION;
        repo.addTotalEnergyWeight(newEnergyWeight - oldEnergyWeight);
        break;
      case TRON_POWER:
        long oldTPWeight = accountCapsule.getTronPowerFrozenV2Balance() / TRX_PRECISION;
        accountCapsule.addFrozenForTronPowerV2(unFreezeV2.getUnfreezeAmount());
        long newTPWeight = accountCapsule.getTronPowerFrozenV2Balance() / TRX_PRECISION;
        repo.addTotalTronPowerWeight(newTPWeight - oldTPWeight);
        break;
      default:
        // this should never happen
        break;
    }
  }
}
