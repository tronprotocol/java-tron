package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.ExpireFreezeV2BalanceParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common;

@Slf4j(topic = "VMProcessor")
public class ExpireFreezeV2BalanceProcessor {

  public void validate(ExpireFreezeV2BalanceParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    if (ownerCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] does not exist");
    }
  }

  public long execute(ExpireFreezeV2BalanceParam param,  Repository repo) {
    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    Common.ResourceCode resourceType = param.getResourceType();

    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    List<Protocol.Account.UnFreezeV2> unfrozenV2List = ownerCapsule.getInstance().getUnfrozenV2List();
    List<Protocol.Account.UnFreezeV2> expireFreezeV2List = unfrozenV2List.stream()
        .filter(unFreezeV2 -> unFreezeV2.getType().equals(resourceType))
        .filter(unFreezeV2 -> unFreezeV2.getUnfreezeAmount() > 0)
        .filter(unFreezeV2 -> unFreezeV2.getUnfreezeExpireTime() <= now)
        .collect(Collectors.toList());
    return expireFreezeV2List.stream().mapToLong(Protocol.Account.UnFreezeV2::getUnfreezeAmount).sum();
  }
}
