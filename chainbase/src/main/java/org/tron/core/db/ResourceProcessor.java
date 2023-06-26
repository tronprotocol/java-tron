package org.tron.core.db;

import static java.lang.Math.ceil;
import static java.lang.Math.round;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.WINDOW_SIZE_PRECISION;

import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.AdaptiveResourceLimitConstants;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.contract.Common.ResourceCode;

abstract class ResourceProcessor {

  protected DynamicPropertiesStore dynamicPropertiesStore;
  protected AccountStore accountStore;
  protected long precision;
  protected long windowSize;
  protected long averageWindowSize;

  protected ResourceProcessor(DynamicPropertiesStore dynamicPropertiesStore,
      AccountStore accountStore) {
    this.dynamicPropertiesStore = dynamicPropertiesStore;
    this.accountStore = accountStore;
    this.precision = ChainConstant.PRECISION;
    this.windowSize = ChainConstant.WINDOW_SIZE_MS / BLOCK_PRODUCED_INTERVAL;
    this.averageWindowSize =
        AdaptiveResourceLimitConstants.PERIODS_MS / BLOCK_PRODUCED_INTERVAL;
  }

  abstract void consume(TransactionCapsule trx, TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException;

  protected long increase(long lastUsage, long usage, long lastTime, long now) {
    return increase(lastUsage, usage, lastTime, now, windowSize);
  }

  protected long increase(long lastUsage, long usage, long lastTime, long now, long windowSize) {
    long averageLastUsage = divideCeil(lastUsage * precision, windowSize);
    long averageUsage = divideCeil(usage * precision, windowSize);

    if (lastTime != now) {
      assert now > lastTime;
      if (lastTime + windowSize > now) {
        long delta = now - lastTime;
        double decay = (windowSize - delta) / (double) windowSize;
        averageLastUsage = Math.round(averageLastUsage * decay);
      } else {
        averageLastUsage = 0;
      }
    }
    averageLastUsage += averageUsage;
    return getUsage(averageLastUsage, windowSize);
  }

  public long recovery(AccountCapsule accountCapsule, ResourceCode resourceCode,
      long lastUsage, long lastTime, long now) {
    long oldWindowSize = accountCapsule.getWindowSize(resourceCode);
    return increase(lastUsage, 0, lastTime, now, oldWindowSize);
  }

  public long increase(AccountCapsule accountCapsule, ResourceCode resourceCode,
      long lastUsage, long usage, long lastTime, long now) {
    if (dynamicPropertiesStore.supportAllowCancelAllUnfreezeV2()) {
      return increaseV2(accountCapsule, resourceCode, lastUsage, usage, lastTime, now);
    }
    long oldWindowSize = accountCapsule.getWindowSize(resourceCode);
    long averageLastUsage = divideCeil(lastUsage * this.precision, oldWindowSize);
    long averageUsage = divideCeil(usage * this.precision, this.windowSize);

    if (lastTime != now) {
      if (lastTime + oldWindowSize > now) {
        long delta = now - lastTime;
        double decay = (oldWindowSize - delta) / (double) oldWindowSize;
        averageLastUsage = Math.round(averageLastUsage * decay);
      } else {
        averageLastUsage = 0;
      }
    }

    long newUsage = getUsage(averageLastUsage, oldWindowSize, averageUsage, this.windowSize);
    if (dynamicPropertiesStore.supportUnfreezeDelay()) {
      long remainUsage = getUsage(averageLastUsage, oldWindowSize);
      if (remainUsage == 0) {
        accountCapsule.setNewWindowSize(resourceCode, this.windowSize);
        return newUsage;
      }
      long remainWindowSize = oldWindowSize - (now - lastTime);
      long newWindowSize = getNewWindowSize(remainUsage, remainWindowSize, usage,
          windowSize, newUsage);
      accountCapsule.setNewWindowSize(resourceCode, newWindowSize);
    }
    return newUsage;
  }

  public long increaseV2(AccountCapsule accountCapsule, ResourceCode resourceCode,
      long lastUsage, long usage, long lastTime, long now) {
    long oldWindowSizeV2 = accountCapsule.getWindowSizeV2(resourceCode);
    long oldWindowSize = accountCapsule.getWindowSize(resourceCode);
    long averageLastUsage = divideCeil(lastUsage * this.precision, oldWindowSize);
    long averageUsage = divideCeil(usage * this.precision, this.windowSize);

    if (lastTime != now) {
      if (lastTime + oldWindowSize > now) {
        long delta = now - lastTime;
        double decay = (oldWindowSize - delta) / (double) oldWindowSize;
        averageLastUsage = Math.round(averageLastUsage * decay);
      } else {
        averageLastUsage = 0;
      }
    }

    long newUsage = getUsage(averageLastUsage, oldWindowSize, averageUsage, this.windowSize);
    long remainUsage = getUsage(averageLastUsage, oldWindowSize);
    if (remainUsage == 0) {
      accountCapsule.setNewWindowSizeV2(resourceCode, this.windowSize * WINDOW_SIZE_PRECISION);
      return newUsage;
    }

    long remainWindowSize = oldWindowSizeV2 - (now - lastTime) * WINDOW_SIZE_PRECISION;
    long newWindowSize = divideCeil(
            remainUsage * remainWindowSize + usage * this.windowSize * WINDOW_SIZE_PRECISION, newUsage);
    newWindowSize = Math.min(newWindowSize, this.windowSize * WINDOW_SIZE_PRECISION);
    accountCapsule.setNewWindowSizeV2(resourceCode, newWindowSize);
    return newUsage;
  }

  public long unDelegateIncrease(AccountCapsule owner, final AccountCapsule receiver,
      long transferUsage, ResourceCode resourceCode, long now) {
    if (dynamicPropertiesStore.supportAllowCancelAllUnfreezeV2()) {
      return unDelegateIncreaseV2(owner, receiver, transferUsage, resourceCode, now);
    }
    long lastOwnerTime = owner.getLastConsumeTime(resourceCode);
    long ownerUsage = owner.getUsage(resourceCode);
    // Update itself first
    ownerUsage = increase(owner, resourceCode, ownerUsage, 0, lastOwnerTime, now);

    long remainOwnerWindowSize = owner.getWindowSize(resourceCode);
    long remainReceiverWindowSize = receiver.getWindowSize(resourceCode);
    remainOwnerWindowSize = remainOwnerWindowSize < 0 ? 0 : remainOwnerWindowSize;
    remainReceiverWindowSize = remainReceiverWindowSize < 0 ? 0 : remainReceiverWindowSize;

    long newOwnerUsage = ownerUsage + transferUsage;
    // mean ownerUsage == 0 and transferUsage == 0
    if (newOwnerUsage == 0) {
      owner.setNewWindowSize(resourceCode, this.windowSize);
      return newOwnerUsage;
    }
    // calculate new windowSize
    long newOwnerWindowSize = getNewWindowSize(ownerUsage, remainOwnerWindowSize, transferUsage,
        remainReceiverWindowSize, newOwnerUsage);
    owner.setNewWindowSize(resourceCode, newOwnerWindowSize);
    return newOwnerUsage;
  }

  public long unDelegateIncreaseV2(AccountCapsule owner, final AccountCapsule receiver,
      long transferUsage, ResourceCode resourceCode, long now) {
    long lastOwnerTime = owner.getLastConsumeTime(resourceCode);
    long ownerUsage = owner.getUsage(resourceCode);
    // Update itself first
    ownerUsage = increase(owner, resourceCode, ownerUsage, 0, lastOwnerTime, now);
    long newOwnerUsage = ownerUsage + transferUsage;
    // mean ownerUsage == 0 and transferUsage == 0
    if (newOwnerUsage == 0) {
      owner.setNewWindowSizeV2(resourceCode, this.windowSize * WINDOW_SIZE_PRECISION);
      return newOwnerUsage;
    }

    long remainOwnerWindowSizeV2 = owner.getWindowSizeV2(resourceCode);
    long remainReceiverWindowSizeV2 = receiver.getWindowSizeV2(resourceCode);
    remainOwnerWindowSizeV2 = remainOwnerWindowSizeV2 < 0 ? 0 : remainOwnerWindowSizeV2;
    remainReceiverWindowSizeV2 = remainReceiverWindowSizeV2 < 0 ? 0 : remainReceiverWindowSizeV2;

    // calculate new windowSize
    long newOwnerWindowSize =
        divideCeil(
            ownerUsage * remainOwnerWindowSizeV2 + transferUsage * remainReceiverWindowSizeV2,
            newOwnerUsage);
    newOwnerWindowSize = Math.min(newOwnerWindowSize, this.windowSize * WINDOW_SIZE_PRECISION);
    owner.setNewWindowSizeV2(resourceCode, newOwnerWindowSize);
    return newOwnerUsage;
  }

  private long getNewWindowSize(long lastUsage, long lastWindowSize, long usage,
      long windowSize, long newUsage) {
    return (lastUsage * lastWindowSize + usage * windowSize) / newUsage;
  }

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long getUsage(long usage, long windowSize) {
    return usage * windowSize / precision;
  }

  private long getUsage(long oldUsage, long oldWindowSize, long newUsage, long newWindowSize) {
    return (oldUsage * oldWindowSize + newUsage * newWindowSize) / precision;
  }

  protected boolean consumeFeeForBandwidth(AccountCapsule accountCapsule, long fee) {
    try {
      long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
      accountCapsule.setLatestOperationTime(latestOperationTime);
      Commons.adjustBalance(accountStore, accountCapsule, -fee);
      if (dynamicPropertiesStore.supportTransactionFeePool()) {
        dynamicPropertiesStore.addTransactionFeePool(fee);
      } else if (dynamicPropertiesStore.supportBlackHoleOptimization()) {
        dynamicPropertiesStore.burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), +fee);
      }

      return true;
    } catch (BalanceInsufficientException e) {
      return false;
    }
  }

  protected boolean consumeFeeForNewAccount(AccountCapsule accountCapsule, long fee) {
    try {
      long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
      accountCapsule.setLatestOperationTime(latestOperationTime);
      Commons.adjustBalance(accountStore, accountCapsule, -fee);
      if (dynamicPropertiesStore.supportBlackHoleOptimization()) {
        dynamicPropertiesStore.burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), +fee);
      }

      return true;
    } catch (BalanceInsufficientException e) {
      return false;
    }
  }
}
