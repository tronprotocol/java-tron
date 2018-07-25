package org.tron.core.db;

import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.Parameter.ChainConstant;

public class StorageMarket {

  private Manager dbManager;
  private long supply = 1_000_000_000_000_000L;

  public StorageMarket(Manager manager) {
    this.dbManager = manager;
  }

  private long exchange_to_supply(boolean isBuy, long quant) {
    long balance = isBuy ? dbManager.getDynamicPropertiesStore().getTotalStoragePool() :
        dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    balance += quant;

    if (isBuy) {
      dbManager.getDynamicPropertiesStore().saveTotalStoragePool(balance);
    } else {
      dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(balance);
    }

    double issuedSupply = -supply * (1.0 - Math.pow(1.0 + quant / balance, 0.0005));
    long out = (long) issuedSupply;
    supply += out;

    return out;
  }

  private long exchange_from_supply(boolean isBuy, long supplyQuant) {
    long balance = isBuy ? dbManager.getDynamicPropertiesStore().getTotalStorageReserved() :
        dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    supply -= supplyQuant;

    double exchangeBalance = balance * (Math.pow(1.0 + supplyQuant / supply, 2000.0) - 1.0);
    long out = (long) exchangeBalance;
    long newBalance = balance - out;

    if (isBuy) {
      dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newBalance);
    } else {
      dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newBalance);
    }

    return out;
  }

  public long exchange(long from, boolean isBuy) {
    long relay = exchange_to_supply(isBuy, from);
    return exchange_from_supply(isBuy, relay);
  }

  public long payTax(long duration, long limit) {
    // todo: Support for change by the committee
    double ratePerYear = dbManager.getDynamicPropertiesStore().getStorageExchangeTaxRate() / 100.0;
    double millisecondPerYear = (double) ChainConstant.MS_PER_YEAR;
    double feeRate = duration / millisecondPerYear * ratePerYear;
    long storageTax = (long) (limit * feeRate);

    long tax = exchange(storageTax, false);

    long newTotalTax = dbManager.getDynamicPropertiesStore().getTotalStorageTax() + tax;
    long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() - tax;
    long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
        + storageTax;
    dbManager.getDynamicPropertiesStore().saveTotalStorageTax(newTotalTax);
    dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalPool);
    dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newTotalReserved);

    return storageTax;
  }

  public void buyStorage(AccountCapsule accountCapsule, long quant) {
    long now = dbManager.getHeadBlockTimeStamp();

    long latestExchangeStorageTime = accountCapsule.getLatestExchangeStorageTime();
    long currentStorageLimit = accountCapsule.getStorageLimit();

    long duration = latestExchangeStorageTime - now;
    long storageTax = payTax(duration, currentStorageLimit);

    long newBalance = accountCapsule.getBalance() - quant;

    long storageBought = exchange(quant, true);
    long newStorageLimit = currentStorageLimit - storageTax + storageBought;

    accountCapsule.setStorageLimit(newStorageLimit);
    accountCapsule.setBalance(newBalance);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() + quant;
    long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
        - storageBought;
    dbManager.getDynamicPropertiesStore().saveTotalStorageTax(newTotalPool);
    dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalReserved);

  }

  public void sellStorage(AccountCapsule accountCapsule, long bytes) {
    long now = dbManager.getHeadBlockTimeStamp();

    long latestExchangeStorageTime = accountCapsule.getLatestExchangeStorageTime();
    long currentStorageLimit = accountCapsule.getStorageLimit();

    long duration = latestExchangeStorageTime - now;
    long storageTax = payTax(duration, currentStorageLimit);

    long quant = exchange(bytes, false);
    long newBalance = accountCapsule.getBalance() + quant;

    long newStorageLimit = currentStorageLimit - storageTax - bytes;

    accountCapsule.setStorageLimit(newStorageLimit);
    accountCapsule.setBalance(newBalance);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() - quant;
    long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
        + bytes;
    dbManager.getDynamicPropertiesStore().saveTotalStorageTax(newTotalPool);
    dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalReserved);

  }

}
