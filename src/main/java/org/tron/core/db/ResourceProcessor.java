package org.tron.core.db;

import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.AdaptiveResourceLimitConstants;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TooBigTransactionResultException;

abstract class ResourceProcessor {

  protected Manager dbManager;
  protected long precision;
  protected long windowSize;
  protected long averageWindowSize;

  public ResourceProcessor(Manager manager) {
    this.dbManager = manager;
    this.precision = ChainConstant.PRECISION;
    this.windowSize = ChainConstant.WINDOW_SIZE_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
    this.averageWindowSize =
        AdaptiveResourceLimitConstants.PERIODS_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  abstract void updateUsage(AccountCapsule accountCapsule);

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

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long getUsage(long usage) {
    return usage * windowSize / precision;
  }

  private long getUsage(long usage, long windowSize) {
    return usage * windowSize / precision;
  }

  protected boolean consumeFee(AccountCapsule accountCapsule, long fee) {
    try {
      long latestOperationTime = dbManager.getHeadBlockTimeStamp();
      accountCapsule.setLatestOperationTime(latestOperationTime);
      dbManager.adjustBalance(accountCapsule, -fee);
      dbManager.adjustBalance(this.dbManager.getAccountStore().getBlackhole().createDbKey(), +fee);
      return true;
    } catch (BalanceInsufficientException e) {
      return false;
    }
  }
}
