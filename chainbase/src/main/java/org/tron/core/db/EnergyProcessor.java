package org.tron.core.db;

import static java.lang.Long.max;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.AdaptiveResourceLimitConstants;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account.AccountResource;

@Slf4j(topic = "DB")
public class EnergyProcessor extends ResourceProcessor {

  public EnergyProcessor(DynamicPropertiesStore dynamicPropertiesStore, AccountStore accountStore) {
    super(dynamicPropertiesStore, accountStore);
  }

  public static long getHeadSlot(DynamicPropertiesStore dynamicPropertiesStore) {
    return (dynamicPropertiesStore.getLatestBlockHeaderTimestamp() -
        Long.parseLong(CommonParameter.getInstance()
            .getGenesisBlock().getTimestamp()))
        / BLOCK_PRODUCED_INTERVAL;
  }

  @Override
  public void updateUsage(AccountCapsule accountCapsule) {
    long now = getHeadSlot();
    updateUsage(accountCapsule, now);
  }

  private void updateUsage(AccountCapsule accountCapsule, long now) {
    AccountResource accountResource = accountCapsule.getAccountResource();

    long oldEnergyUsage = accountResource.getEnergyUsage();
    long latestConsumeTime = accountResource.getLatestConsumeTimeForEnergy();

    accountCapsule.setEnergyUsage(increase(oldEnergyUsage, 0, latestConsumeTime, now));
  }

  public void updateTotalEnergyAverageUsage() {
    long now = getHeadSlot();
    long blockEnergyUsage = dynamicPropertiesStore.getBlockEnergyUsage();
    long totalEnergyAverageUsage = dynamicPropertiesStore
        .getTotalEnergyAverageUsage();
    long totalEnergyAverageTime = dynamicPropertiesStore.getTotalEnergyAverageTime();

    long newPublicEnergyAverageUsage = increase(totalEnergyAverageUsage, blockEnergyUsage,
        totalEnergyAverageTime, now, averageWindowSize);

    dynamicPropertiesStore.saveTotalEnergyAverageUsage(newPublicEnergyAverageUsage);
    dynamicPropertiesStore.saveTotalEnergyAverageTime(now);
  }

  public void updateAdaptiveTotalEnergyLimit() {
    long totalEnergyAverageUsage = dynamicPropertiesStore
        .getTotalEnergyAverageUsage();
    long targetTotalEnergyLimit = dynamicPropertiesStore.getTotalEnergyTargetLimit();
    long totalEnergyCurrentLimit = dynamicPropertiesStore
        .getTotalEnergyCurrentLimit();
    long totalEnergyLimit = dynamicPropertiesStore.getTotalEnergyLimit();

    long result;
    if (totalEnergyAverageUsage > targetTotalEnergyLimit) {
      result = totalEnergyCurrentLimit * AdaptiveResourceLimitConstants.CONTRACT_RATE_NUMERATOR
          / AdaptiveResourceLimitConstants.CONTRACT_RATE_DENOMINATOR;
      // logger.info(totalEnergyAverageUsage + ">" + targetTotalEnergyLimit + "\n" + result);
    } else {
      result = totalEnergyCurrentLimit * AdaptiveResourceLimitConstants.EXPAND_RATE_NUMERATOR
          / AdaptiveResourceLimitConstants.EXPAND_RATE_DENOMINATOR;
      // logger.info(totalEnergyAverageUsage + "<" + targetTotalEnergyLimit + "\n" + result);
    }

    result = Math.min(
        Math.max(result, totalEnergyLimit),
        totalEnergyLimit * dynamicPropertiesStore.getAdaptiveResourceLimitMultiplier()
    );

    dynamicPropertiesStore.saveTotalEnergyCurrentLimit(result);
    logger.debug(
        "adjust totalEnergyCurrentLimit, old[" + totalEnergyCurrentLimit + "], new[" + result
            + "]");
  }

  @Override
  public void consume(TransactionCapsule trx,
      TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException {
    throw new RuntimeException("Not support");
  }

  public boolean useEnergy(AccountCapsule accountCapsule, long energy, long now) {

    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(accountCapsule);

    long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

    if (energy > (energyLimit - newEnergyUsage)) {
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    newEnergyUsage = increase(newEnergyUsage, energy, latestConsumeTime, now);
    accountCapsule.setEnergyUsage(newEnergyUsage);
    accountCapsule.setLatestOperationTime(latestOperationTime);
    accountCapsule.setLatestConsumeTimeForEnergy(latestConsumeTime);

    accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    if (dynamicPropertiesStore.getAllowAdaptiveEnergy() == 1) {
      long blockEnergyUsage = dynamicPropertiesStore.getBlockEnergyUsage() + energy;
      dynamicPropertiesStore.saveBlockEnergyUsage(blockEnergyUsage);
    }

    return true;
  }

  public long calculateGlobalEnergyLimit(AccountCapsule accountCapsule) {
    long frozeBalance = accountCapsule.getAllFrozenBalanceForEnergy();
    if (frozeBalance < TRX_PRECISION) {
      return 0;
    }

    long energyWeight = frozeBalance / TRX_PRECISION;
    long totalEnergyLimit = dynamicPropertiesStore.getTotalEnergyCurrentLimit();
    long totalEnergyWeight = dynamicPropertiesStore.getTotalEnergyWeight();

    assert totalEnergyWeight > 0;

    return (long) (energyWeight * ((double) totalEnergyLimit / totalEnergyWeight));
  }

  public long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule) {
    long now = getHeadSlot();
    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(accountCapsule);

    long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

    return max(energyLimit - newEnergyUsage, 0); // us
  }

  private long getHeadSlot() {
    return getHeadSlot(dynamicPropertiesStore);
  }


}


