package org.tron.core.db;

import static org.tron.common.math.Maths.min;
import static org.tron.common.math.Maths.round;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.core.config.Parameter.ChainConstant.WINDOW_SIZE_PRECISION;

import java.math.BigInteger;
import org.tron.common.math.StrictMathWrapper;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.AdaptiveResourceLimitConstants;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TooBigTransactionException;
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
      throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException, TooBigTransactionException;

  protected long increase(long lastUsage, long usage, long lastTime, long now) {
    return increase(lastUsage, usage, lastTime, now, windowSize);
  }

  protected long increase(long lastUsage, long usage, long lastTime, long now, long windowSize) {
    long averageLastUsage;
    long averageUsage;
    if (hardenCalculation()) {
      BigInteger biPrecision = BigInteger.valueOf(precision);
      BigInteger biWindowSize = BigInteger.valueOf(windowSize);
      averageLastUsage = divideCeilExact(
          BigInteger.valueOf(lastUsage).multiply(biPrecision), biWindowSize);
      averageUsage = divideCeilExact(
          BigInteger.valueOf(usage).multiply(biPrecision), biWindowSize);
    } else {
      averageLastUsage = divideCeil(lastUsage * precision, windowSize);
      averageUsage = divideCeil(usage * precision, windowSize);
    }

    if (lastTime != now) {
      assert now > lastTime;
      if (lastTime + windowSize > now) {
        long delta = now - lastTime;
        double decay = (windowSize - delta) / (double) windowSize;
        averageLastUsage = round(averageLastUsage * decay,
            this.disableJavaLangMath());
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
    long averageLastUsage;
    long averageUsage;
    if (hardenCalculation()) {
      BigInteger biPrecision = BigInteger.valueOf(this.precision);
      averageLastUsage = divideCeilExact(
          BigInteger.valueOf(lastUsage).multiply(biPrecision),
          BigInteger.valueOf(oldWindowSize));
      averageUsage = divideCeilExact(
          BigInteger.valueOf(usage).multiply(biPrecision),
          BigInteger.valueOf(this.windowSize));
    } else {
      averageLastUsage = divideCeil(lastUsage * this.precision, oldWindowSize);
      averageUsage = divideCeil(usage * this.precision, this.windowSize);
    }

    if (lastTime != now) {
      if (lastTime + oldWindowSize > now) {
        long delta = now - lastTime;
        double decay = (oldWindowSize - delta) / (double) oldWindowSize;
        averageLastUsage = round(averageLastUsage * decay,
            this.disableJavaLangMath());
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
    long averageLastUsage;
    long averageUsage;
    if (hardenCalculation()) {
      BigInteger biPrecision = BigInteger.valueOf(this.precision);
      averageLastUsage = divideCeilExact(
          BigInteger.valueOf(lastUsage).multiply(biPrecision),
          BigInteger.valueOf(oldWindowSize));
      averageUsage = divideCeilExact(
          BigInteger.valueOf(usage).multiply(biPrecision),
          BigInteger.valueOf(this.windowSize));
    } else {
      averageLastUsage = divideCeil(lastUsage * this.precision, oldWindowSize);
      averageUsage = divideCeil(usage * this.precision, this.windowSize);
    }

    if (lastTime != now) {
      if (lastTime + oldWindowSize > now) {
        long delta = now - lastTime;
        double decay = (oldWindowSize - delta) / (double) oldWindowSize;
        averageLastUsage = round(averageLastUsage * decay,
            this.disableJavaLangMath());
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
    long newWindowSize;
    if (hardenCalculation()) {
      BigInteger biNewWindowSize = BigInteger.valueOf(remainUsage)
          .multiply(BigInteger.valueOf(remainWindowSize))
          .add(BigInteger.valueOf(usage)
              .multiply(BigInteger.valueOf(this.windowSize))
              .multiply(BigInteger.valueOf(WINDOW_SIZE_PRECISION)));
      newWindowSize = divideCeilExact(biNewWindowSize, BigInteger.valueOf(newUsage));
    } else {
      newWindowSize = divideCeil(
          remainUsage * remainWindowSize + usage * this.windowSize * WINDOW_SIZE_PRECISION,
          newUsage);
    }
    newWindowSize = min(newWindowSize, this.windowSize * WINDOW_SIZE_PRECISION,
        this.disableJavaLangMath());
    accountCapsule.setNewWindowSizeV2(resourceCode, newWindowSize);
    return newUsage;
  }

  public void unDelegateIncrease(AccountCapsule owner, final AccountCapsule receiver,
      long transferUsage, ResourceCode resourceCode, long now) {
    if (dynamicPropertiesStore.supportAllowCancelAllUnfreezeV2()) {
      unDelegateIncreaseV2(owner, receiver, transferUsage, resourceCode, now);
      return;
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
      owner.setUsage(resourceCode, 0);
      owner.setLatestTime(resourceCode, now);
      return;
    }
    // calculate new windowSize
    long newOwnerWindowSize = getNewWindowSize(ownerUsage, remainOwnerWindowSize, transferUsage,
        remainReceiverWindowSize, newOwnerUsage);
    owner.setNewWindowSize(resourceCode, newOwnerWindowSize);
    owner.setUsage(resourceCode, newOwnerUsage);
    owner.setLatestTime(resourceCode, now);
  }

  public void unDelegateIncreaseV2(AccountCapsule owner, final AccountCapsule receiver,
      long transferUsage, ResourceCode resourceCode, long now) {
    long lastOwnerTime = owner.getLastConsumeTime(resourceCode);
    long ownerUsage = owner.getUsage(resourceCode);
    // Update itself first
    ownerUsage = increase(owner, resourceCode, ownerUsage, 0, lastOwnerTime, now);
    long newOwnerUsage = ownerUsage + transferUsage;
    // mean ownerUsage == 0 and transferUsage == 0
    if (newOwnerUsage == 0) {
      owner.setNewWindowSizeV2(resourceCode, this.windowSize * WINDOW_SIZE_PRECISION);
      owner.setUsage(resourceCode, 0);
      owner.setLatestTime(resourceCode, now);
      return;
    }

    long remainOwnerWindowSizeV2 = owner.getWindowSizeV2(resourceCode);
    long remainReceiverWindowSizeV2 = receiver.getWindowSizeV2(resourceCode);
    remainOwnerWindowSizeV2 = remainOwnerWindowSizeV2 < 0 ? 0 : remainOwnerWindowSizeV2;
    remainReceiverWindowSizeV2 = remainReceiverWindowSizeV2 < 0 ? 0 : remainReceiverWindowSizeV2;

    // calculate new windowSize
    long newOwnerWindowSize;
    if (hardenCalculation()) {
      BigInteger bi = BigInteger.valueOf(ownerUsage)
          .multiply(BigInteger.valueOf(remainOwnerWindowSizeV2))
          .add(BigInteger.valueOf(transferUsage)
              .multiply(BigInteger.valueOf(remainReceiverWindowSizeV2)));
      newOwnerWindowSize = divideCeilExact(bi, BigInteger.valueOf(newOwnerUsage));
    } else {
      newOwnerWindowSize = divideCeil(
          ownerUsage * remainOwnerWindowSizeV2 + transferUsage * remainReceiverWindowSizeV2,
          newOwnerUsage);
    }
    newOwnerWindowSize = min(newOwnerWindowSize, this.windowSize * WINDOW_SIZE_PRECISION,
        this.disableJavaLangMath());
    owner.setNewWindowSizeV2(resourceCode, newOwnerWindowSize);
    owner.setUsage(resourceCode, newOwnerUsage);
    owner.setLatestTime(resourceCode, now);
  }

  private long getNewWindowSize(long lastUsage, long lastWindowSize, long usage,
      long windowSize, long newUsage) {
    if (hardenCalculation()) {
      BigInteger bi = BigInteger.valueOf(lastUsage).multiply(BigInteger.valueOf(lastWindowSize))
          .add(BigInteger.valueOf(usage).multiply(BigInteger.valueOf(windowSize)));
      return bi.divide(BigInteger.valueOf(newUsage)).longValueExact();
    }
    return (lastUsage * lastWindowSize + usage * windowSize) / newUsage;
  }

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long divideCeilExact(BigInteger numerator, BigInteger denominator) {
    BigInteger[] divRem = numerator.divideAndRemainder(denominator);
    long result = divRem[0].longValueExact();
    if (divRem[1].signum() > 0) {
      result = StrictMathWrapper.addExact(result, 1);
    }
    return result;
  }

  private long getUsage(long usage, long windowSize) {
    if (hardenCalculation()) {
      return BigInteger.valueOf(usage).multiply(BigInteger.valueOf(windowSize))
          .divide(BigInteger.valueOf(precision)).longValueExact();
    }
    return usage * windowSize / precision;
  }

  private long getUsage(long oldUsage, long oldWindowSize, long newUsage, long newWindowSize) {
    if (hardenCalculation()) {
      BigInteger bi = BigInteger.valueOf(oldUsage).multiply(BigInteger.valueOf(oldWindowSize))
          .add(BigInteger.valueOf(newUsage).multiply(BigInteger.valueOf(newWindowSize)));
      return bi.divide(BigInteger.valueOf(precision)).longValueExact();
    }
    return (oldUsage * oldWindowSize + newUsage * newWindowSize) / precision;
  }

  protected boolean consumeFeeForBandwidth(AccountCapsule accountCapsule, long fee) {
    try {
      long latestOperationTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
      accountCapsule.setLatestOperationTime(latestOperationTime);
      Commons.adjustBalance(accountStore, accountCapsule, -fee,
          this.disableJavaLangMath());
      if (dynamicPropertiesStore.supportTransactionFeePool()) {
        dynamicPropertiesStore.addTransactionFeePool(fee);
      } else if (dynamicPropertiesStore.supportBlackHoleOptimization()) {
        dynamicPropertiesStore.burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), +fee,
            this.disableJavaLangMath());
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
      Commons.adjustBalance(accountStore, accountCapsule, -fee,
          this.disableJavaLangMath());
      if (dynamicPropertiesStore.supportBlackHoleOptimization()) {
        dynamicPropertiesStore.burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), +fee,
            this.disableJavaLangMath());
      }

      return true;
    } catch (BalanceInsufficientException e) {
      return false;
    }
  }

  protected boolean disableJavaLangMath() {
    return dynamicPropertiesStore.disableJavaLangMath();
  }

  protected boolean hardenCalculation() {
    return dynamicPropertiesStore.allowHardenResourceCalculation();
  }

  protected long calculateGlobalLimitV1(long frozeBalance,
      long totalLimit, long totalWeight) {
    long weight = frozeBalance / TRX_PRECISION;
    return BigInteger.valueOf(weight)
        .multiply(BigInteger.valueOf(totalLimit))
        .divide(BigInteger.valueOf(totalWeight))
        .longValueExact();
  }

  /**
   * Hardened replacement of legacy V2 formula
   * {@code (long)(((double) frozeBalance / TRX_PRECISION)
   *               * ((double) totalLimit / totalWeight))}.
   *
   * <p>Preserves V2 semantics: equivalent to
   * {@code (frozeBalance * totalLimit) / (TRX_PRECISION * totalWeight)} with
   * a single integer truncation at the end. Critically, fractional weight
   * (i.e. {@code frozeBalance < TRX_PRECISION}) is preserved through the
   * multiplication and only truncated at the final divide, so small balances
   * yield the same proportional result as the double-arithmetic path.
   */
  protected long calculateGlobalLimitV2(long frozeBalance,
      long totalLimit, long totalWeight) {
    return BigInteger.valueOf(frozeBalance)
        .multiply(BigInteger.valueOf(totalLimit))
        .divide(BigInteger.valueOf(TRX_PRECISION)
            .multiply(BigInteger.valueOf(totalWeight)))
        .longValueExact();
  }
}
