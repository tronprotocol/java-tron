package org.tron.core.actuator;

import static org.tron.common.prometheus.MetricKeys.Histogram.STAKE_HISTOGRAM;
import static org.tron.common.prometheus.MetricKeys.Histogram.UNFREEZE_CAN_WITHDRAW;
import static org.tron.common.prometheus.MetricLabels.Histogram.STAKE_CANCEL_UNFREEZE;
import static org.tron.common.prometheus.MetricLabels.Histogram.STAKE_UNFREEZE;
import static org.tron.common.prometheus.MetricLabels.Histogram.STAKE_WITHDRAW;
import static org.tron.common.prometheus.MetricLabels.STAKE_ENERGY;
import static org.tron.common.prometheus.MetricLabels.STAKE_NET;
import static org.tron.common.prometheus.MetricLabels.STAKE_RESOURCE;
import static org.tron.common.prometheus.MetricLabels.STAKE_VERSION_V2;
import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;
import static org.tron.protos.contract.Common.ResourceCode.TRON_POWER;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.tron.common.prometheus.Metrics;
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
import org.tron.protos.contract.BalanceContract.CancelAllUnfreezeV2Contract;

@Slf4j(topic = "actuator")
public class CancelAllUnfreezeV2Actuator extends AbstractActuator {

  public CancelAllUnfreezeV2Actuator() {
    super(ContractType.CancelAllUnfreezeV2Contract, CancelAllUnfreezeV2Contract.class);
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
    byte[] ownerAddress;
    try {
      ownerAddress = getOwnerAddress().toByteArray();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    AccountCapsule ownerCapsule = accountStore.get(ownerAddress);
    List<UnFreezeV2> unfrozenV2List = ownerCapsule.getUnfrozenV2List();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    AtomicLong atomicWithdrawExpireBalance = new AtomicLong(0L);
    /* The triple object is defined by resource type, with left representing the pair object
    corresponding to bandwidth, middle representing the pair object corresponding to energy, and
    right representing the pair object corresponding to tron power. The pair object for each
    resource type, left represents resource weight, and right represents the number of unfreeze
    resources for that resource type. */
    Triple<Pair<AtomicLong, AtomicLong>, Pair<AtomicLong, AtomicLong>, Pair<AtomicLong, AtomicLong>>
        triple = Triple.of(
        Pair.of(new AtomicLong(0L), new AtomicLong(0L)),
        Pair.of(new AtomicLong(0L), new AtomicLong(0L)),
        Pair.of(new AtomicLong(0L), new AtomicLong(0L)));
    for (UnFreezeV2 unFreezeV2 : unfrozenV2List) {
      updateAndCalculate(triple, ownerCapsule, now, atomicWithdrawExpireBalance, unFreezeV2);
    }
    ownerCapsule.clearUnfrozenV2();
    addTotalResourceWeight(dynamicStore, triple);

    long withdrawExpireBalance = atomicWithdrawExpireBalance.get();
    if (withdrawExpireBalance > 0) {
      ownerCapsule.setBalance(ownerCapsule.getBalance() + withdrawExpireBalance);
    }

    accountStore.put(ownerCapsule.createDbKey(), ownerCapsule);
    ret.setWithdrawExpireAmount(withdrawExpireBalance);
    Map<String, Long> cancelUnfreezeV2AmountMap = new HashMap<>();
    cancelUnfreezeV2AmountMap.put(BANDWIDTH.name(), triple.getLeft().getRight().get());
    cancelUnfreezeV2AmountMap.put(ENERGY.name(), triple.getMiddle().getRight().get());
    cancelUnfreezeV2AmountMap.put(TRON_POWER.name(), triple.getRight().getRight().get());
    ret.putAllCancelUnfreezeV2AmountMap(cancelUnfreezeV2AmountMap);
    ret.setStatus(fee, code.SUCESS);
    long sum = cancelUnfreezeV2AmountMap.values()
        .stream().mapToLong(v -> v).sum();
    Metrics.histogramObserve(STAKE_HISTOGRAM, sum, STAKE_VERSION_V2,
        STAKE_CANCEL_UNFREEZE, STAKE_RESOURCE);

    Metrics.histogramObserve(UNFREEZE_CAN_WITHDRAW, -triple.getLeft().getRight().get(),
        STAKE_VERSION_V2, STAKE_CANCEL_UNFREEZE, STAKE_NET);
    Metrics.histogramObserve(UNFREEZE_CAN_WITHDRAW, -triple.getMiddle().getRight().get(),
        STAKE_VERSION_V2, STAKE_CANCEL_UNFREEZE, STAKE_ENERGY);
    logger.info("cancel all unfreezeV2 detail:{},{},{}",
        StringUtil.createReadableString(ownerCapsule.getAddress()), ownerCapsule.getType(), sum);
    return true;
  }

  private void addTotalResourceWeight(DynamicPropertiesStore dynamicStore,
      Triple<Pair<AtomicLong, AtomicLong>,
          Pair<AtomicLong, AtomicLong>,
          Pair<AtomicLong, AtomicLong>> triple) {
    dynamicStore.addTotalNetWeight(triple.getLeft().getLeft().get());
    dynamicStore.addTotalEnergyWeight(triple.getMiddle().getLeft().get());
    dynamicStore.addTotalTronPowerWeight(triple.getRight().getLeft().get());
  }

  private void updateAndCalculate(Triple<Pair<AtomicLong, AtomicLong>, Pair<AtomicLong, AtomicLong>,
      Pair<AtomicLong, AtomicLong>> triple,
      AccountCapsule ownerCapsule, long now, AtomicLong atomicLong, UnFreezeV2 unFreezeV2) {
    if (unFreezeV2.getUnfreezeExpireTime() > now) {
      updateFrozenInfoAndTotalResourceWeight(ownerCapsule, unFreezeV2, triple);
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

    if (!this.any.is(CancelAllUnfreezeV2Contract.class)) {
      throw new ContractValidateException("contract type error, expected type " +
          "[CancelAllUnfreezeV2Contract], real type[" + any.getClass() + "]");
    }

    if (!dynamicStore.supportAllowCancelAllUnfreezeV2()) {
      throw new ContractValidateException("Not support CancelAllUnfreezeV2 transaction,"
          + " need to be opened by the committee");
    }

    byte[] ownerAddress;
    try {
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

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return getCancelAllUnfreezeV2Contract().getOwnerAddress();
  }

  private CancelAllUnfreezeV2Contract getCancelAllUnfreezeV2Contract()
      throws InvalidProtocolBufferException {
    return any.unpack(CancelAllUnfreezeV2Contract.class);
  }

  @Override
  public long calcFee() {
    return 0;
  }

  public void updateFrozenInfoAndTotalResourceWeight(
      AccountCapsule accountCapsule, UnFreezeV2 unFreezeV2,
      Triple<Pair<AtomicLong, AtomicLong>, Pair<AtomicLong, AtomicLong>,
          Pair<AtomicLong, AtomicLong>> triple) {
    switch (unFreezeV2.getType()) {
      case BANDWIDTH:
        long oldNetWeight = accountCapsule.getFrozenV2BalanceWithDelegated(BANDWIDTH) / TRX_PRECISION;
        accountCapsule.addFrozenBalanceForBandwidthV2(unFreezeV2.getUnfreezeAmount());
        long newNetWeight = accountCapsule.getFrozenV2BalanceWithDelegated(BANDWIDTH) / TRX_PRECISION;
        triple.getLeft().getLeft().addAndGet(newNetWeight - oldNetWeight);
        triple.getLeft().getRight().addAndGet(unFreezeV2.getUnfreezeAmount());
        break;
      case ENERGY:
        long oldEnergyWeight = accountCapsule.getFrozenV2BalanceWithDelegated(ENERGY) / TRX_PRECISION;
        accountCapsule.addFrozenBalanceForEnergyV2(unFreezeV2.getUnfreezeAmount());
        long newEnergyWeight = accountCapsule.getFrozenV2BalanceWithDelegated(ENERGY) / TRX_PRECISION;
        triple.getMiddle().getLeft().addAndGet(newEnergyWeight - oldEnergyWeight);
        triple.getMiddle().getRight().addAndGet(unFreezeV2.getUnfreezeAmount());
        break;
      case TRON_POWER:
        long oldTPWeight = accountCapsule.getTronPowerFrozenV2Balance() / TRX_PRECISION;
        accountCapsule.addFrozenForTronPowerV2(unFreezeV2.getUnfreezeAmount());
        long newTPWeight = accountCapsule.getTronPowerFrozenV2Balance() / TRX_PRECISION;
        triple.getRight().getLeft().addAndGet(newTPWeight - oldTPWeight);
        triple.getRight().getRight().addAndGet(unFreezeV2.getUnfreezeAmount());
        break;
      default:
        break;
    }
  }
}
