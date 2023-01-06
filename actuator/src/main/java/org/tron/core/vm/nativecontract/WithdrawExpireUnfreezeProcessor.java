package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;

import com.google.common.math.LongMath;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.WithdrawExpireUnfreezeParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

@Slf4j(topic = "VMProcessor")
public class WithdrawExpireUnfreezeProcessor {

  public void validate(WithdrawExpireUnfreezeParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);
    if (Objects.isNull(accountCapsule)) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(ACCOUNT_EXCEPTION_STR
          + readableOwnerAddress + NOT_EXIST_STR);
    }

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    List<Protocol.Account.UnFreezeV2> unfrozenV2List = accountCapsule.getInstance()
        .getUnfrozenV2List();
    long totalWithdrawUnfreeze = getTotalWithdrawUnfreeze(unfrozenV2List, now);
    if (totalWithdrawUnfreeze < 0) {
      throw new ContractValidateException("no unFreeze balance to withdraw ");
    }
    try {
      LongMath.checkedAdd(accountCapsule.getBalance(), totalWithdrawUnfreeze);
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
  }

  private long getTotalWithdrawUnfreeze(List<Protocol.Account.UnFreezeV2> unfrozenV2List, long now) {
    return getTotalWithdrawList(unfrozenV2List, now).stream()
        .mapToLong(Protocol.Account.UnFreezeV2::getUnfreezeAmount).sum();
  }

  private List<Protocol.Account.UnFreezeV2> getTotalWithdrawList(List<Protocol.Account.UnFreezeV2> unfrozenV2List, long now) {
    return unfrozenV2List.stream().filter(unfrozenV2 -> (unfrozenV2.getUnfreezeAmount() > 0
        && unfrozenV2.getUnfreezeExpireTime() <= now)).collect(Collectors.toList());
  }

  public long execute(WithdrawExpireUnfreezeParam param, Repository repo) throws ContractExeException {
    byte[] ownerAddress = param.getOwnerAddress();
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    List<Protocol.Account.UnFreezeV2> unfrozenV2List = ownerCapsule.getInstance().getUnfrozenV2List();
    long totalWithdrawUnfreeze = getTotalWithdrawUnfreeze(unfrozenV2List, now);
    if (totalWithdrawUnfreeze <= 0) {
      return 0;
    }
    ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
        .setBalance(ownerCapsule.getBalance() + totalWithdrawUnfreeze)
        .build());
    List<Protocol.Account.UnFreezeV2> newUnFreezeList = getRemainWithdrawList(unfrozenV2List, now);
    ownerCapsule.clearUnfrozenV2();
    ownerCapsule.addAllUnfrozenV2(newUnFreezeList);
    repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);
    return totalWithdrawUnfreeze;
  }

  private List<Protocol.Account.UnFreezeV2> getRemainWithdrawList(List<Protocol.Account.UnFreezeV2> unfrozenV2List, long now) {
    return unfrozenV2List.stream()
        .filter(unfrozenV2 -> unfrozenV2.getUnfreezeExpireTime() > now)
        .collect(Collectors.toList());
  }
}
