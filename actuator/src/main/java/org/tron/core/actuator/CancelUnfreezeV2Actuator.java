package org.tron.core.actuator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account.UnFreezeV2;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.CancelUnfreezeV2Contract;

@Slf4j(topic = "actuator")
public class CancelUnfreezeV2Actuator extends AbstractActuator {

  public CancelUnfreezeV2Actuator() {
    super(ContractType.CancelUnfreezeV2Contract, CancelUnfreezeV2Contract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }
    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    final CancelUnfreezeV2Contract cancelUnfreezeV2Contract;
    byte[] ownerAddress;
    try {
      cancelUnfreezeV2Contract = getCancelUnfreezeV2Contract();
      ownerAddress = getOwnerAddress().toByteArray();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    List<Integer> indexList = cancelUnfreezeV2Contract.getIndexList()
        .stream().sorted().collect(Collectors.toList());
    AccountCapsule ownerCapsule = accountStore.get(ownerAddress);
    List<UnFreezeV2> unfrozenV2List = ownerCapsule.getUnfrozenV2List();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    AtomicLong atomicWithdrawExpireBalance = new AtomicLong(0L);
    AtomicLong atomicCancelBalance = new AtomicLong(0L);
    Triple<AtomicLong, AtomicLong, AtomicLong> triple =
        Triple.of(new AtomicLong(0L), new AtomicLong(0L), new AtomicLong(0L));
    List<UnFreezeV2> newUnFreezeV2List = null;
    if (indexList.isEmpty()) {
      for (UnFreezeV2 unFreezeV2 : unfrozenV2List) {
        updateAndCalculate(triple, ownerCapsule, now, atomicWithdrawExpireBalance,
            atomicCancelBalance, unFreezeV2);
      }
    } else {
      indexList.forEach(index -> {
        UnFreezeV2 unFreezeV2 = unfrozenV2List.get(index);
        updateAndCalculate(triple, ownerCapsule, now, atomicWithdrawExpireBalance,
            atomicCancelBalance, unFreezeV2);
      });
      newUnFreezeV2List = unfrozenV2List.stream()
          .filter(o -> !indexList.contains(unfrozenV2List.indexOf(o))).collect(Collectors.toList());
    }
    ownerCapsule.clearUnfrozenV2();
    ownerCapsule.addAllUnfrozenV2(newUnFreezeV2List);
    addTotalResourceWeight(dynamicStore, triple);

    long withdrawExpireBalance = atomicWithdrawExpireBalance.get();
    if (withdrawExpireBalance > 0) {
      ownerCapsule.setBalance(ownerCapsule.getBalance() + withdrawExpireBalance);
    }

    accountStore.put(ownerCapsule.createDbKey(), ownerCapsule);
    ret.setWithdrawExpireAmount(withdrawExpireBalance);
    ret.setCancelUnfreezeV2Amount(atomicCancelBalance.get());
    ret.setStatus(fee, code.SUCESS);
    return true;
  }

  private void addTotalResourceWeight(DynamicPropertiesStore dynamicStore,
                                      Triple<AtomicLong, AtomicLong, AtomicLong> triple) {
    dynamicStore.addTotalNetWeight(triple.getLeft().get());
    dynamicStore.addTotalEnergyWeight(triple.getMiddle().get());
    dynamicStore.addTotalTronPowerWeight(triple.getRight().get());
  }

  private void updateAndCalculate(Triple<AtomicLong, AtomicLong, AtomicLong> triple,
      AccountCapsule ownerCapsule, long now, AtomicLong atomicLong, AtomicLong cancelBalance,
      UnFreezeV2 unFreezeV2) {
    if (unFreezeV2.getUnfreezeExpireTime() > now) {
      updateFrozenInfoAndTotalResourceWeight(ownerCapsule, unFreezeV2, triple);
      cancelBalance.addAndGet(unFreezeV2.getUnfreezeAmount());
    } else {
      atomicLong.addAndGet(unFreezeV2.getUnfreezeAmount());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (Objects.isNull(this.any)) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }

    if (Objects.isNull(chainBaseManager)) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }

    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();

    if (!this.any.is(CancelUnfreezeV2Contract.class)) {
      throw new ContractValidateException("contract type error, expected type " +
          "[CancelUnfreezeV2Contract], real type[" + any.getClass() + "]");
    }

    if (!dynamicStore.supportAllowCancelUnfreezeV2()) {
      throw new ContractValidateException("Not support CancelUnfreezeV2 transaction,"
          + " need to be opened by the committee");
    }

    final CancelUnfreezeV2Contract cancelUnfreezeV2Contract;
    byte[] ownerAddress;
    try {
      cancelUnfreezeV2Contract = getCancelUnfreezeV2Contract();
      ownerAddress = getOwnerAddress().toByteArray();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }
    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
    if (Objects.isNull(accountCapsule)) {
      throw new ContractValidateException(ACCOUNT_EXCEPTION_STR
          + readableOwnerAddress + NOT_EXIST_STR);
    }

    List<UnFreezeV2> unfrozenV2List = accountCapsule.getUnfrozenV2List();
    if (unfrozenV2List.isEmpty()) {
      throw new ContractValidateException("No unfreezeV2 list to cancel");
    }

    List<Integer> indexList = cancelUnfreezeV2Contract.getIndexList();
    if (indexList.size() > unfrozenV2List.size()) {
      throw new ContractValidateException(
          "The size[" + indexList.size() + "] of the index cannot exceed the size["
              + unfrozenV2List.size() + "] of unfreezeV2!");
    }

    for (Integer i : indexList) {
      int maxIndex = unfrozenV2List.size() - 1;
      if (i < 0 || i > maxIndex) {
        throw new ContractValidateException(
            "The input index[" + i + "] cannot be less than 0 and cannot be "
                + "greater than the maximum index[" + maxIndex + "] of unfreezeV2!");
      }
    }
    Set<Integer> set = new HashSet<>();
    List<Integer> dps = indexList.stream().filter(n -> !set.add(n)).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(dps)) {
      throw new ContractValidateException("The element" + dps + " in the index list is duplicated");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return getCancelUnfreezeV2Contract().getOwnerAddress();
  }

  private CancelUnfreezeV2Contract getCancelUnfreezeV2Contract()
      throws InvalidProtocolBufferException {
    return any.unpack(CancelUnfreezeV2Contract.class);
  }

  @Override
  public long calcFee() {
    return 0;
  }

  public void updateFrozenInfoAndTotalResourceWeight(
      AccountCapsule accountCapsule, UnFreezeV2 unFreezeV2,
      Triple<AtomicLong, AtomicLong, AtomicLong> triple) {
    switch (unFreezeV2.getType()) {
      case BANDWIDTH:
        long oldNetWeight = accountCapsule.getFrozenV2BalanceWithDelegated(BANDWIDTH) / TRX_PRECISION;
        accountCapsule.addFrozenBalanceForBandwidthV2(unFreezeV2.getUnfreezeAmount());
        long newNetWeight = accountCapsule.getFrozenV2BalanceWithDelegated(BANDWIDTH) / TRX_PRECISION;
        triple.getLeft().addAndGet(newNetWeight - oldNetWeight);
        break;
      case ENERGY:
        long oldEnergyWeight = accountCapsule.getFrozenV2BalanceWithDelegated(ENERGY) / TRX_PRECISION;
        accountCapsule.addFrozenBalanceForEnergyV2(unFreezeV2.getUnfreezeAmount());
        long newEnergyWeight = accountCapsule.getFrozenV2BalanceWithDelegated(ENERGY) / TRX_PRECISION;
        triple.getMiddle().addAndGet(newEnergyWeight - oldEnergyWeight);
        break;
      case TRON_POWER:
        long oldTPWeight = accountCapsule.getTronPowerFrozenV2Balance() / TRX_PRECISION;
        accountCapsule.addFrozenForTronPowerV2(unFreezeV2.getUnfreezeAmount());
        long newTPWeight = accountCapsule.getTronPowerFrozenV2Balance() / TRX_PRECISION;
        triple.getRight().addAndGet(newTPWeight - oldTPWeight);
        break;
      default:
        break;
    }
  }
}
