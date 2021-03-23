package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;

@Slf4j(topic = "DB")
public class StorageMarket {

  private static final long MS_PER_YEAR = 365 * 24 * 3600 * 1000L;
  private static final String NEW_TOTAL_RESERVED = "  newTotalReserved: ";
  private static final String NEW_STORAGE_LIMIT = "  newStorageLimit: ";
  private static final String NEW_TOTAL_POOL = "newTotalPool: ";
  private AccountStore accountStore;
  private DynamicPropertiesStore dynamicPropertiesStore;
  private long supply = 1_000_000_000_000_000L;


  public StorageMarket(AccountStore accountStore, DynamicPropertiesStore dynamicPropertiesStore) {
    this.accountStore = accountStore;
    this.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  private long exchangeToSupply(boolean isTRX, long quant) {
    logger.info("isTRX: " + isTRX);
    long balance = isTRX ? dynamicPropertiesStore.getTotalStoragePool() :
        dynamicPropertiesStore.getTotalStorageReserved();
    logger.info("balance: " + balance);
    long newBalance = balance + quant;
    logger.info("balance + quant: " + (balance + quant));

//    if (isTRX) {
//      dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newBalance);
//    } else {
//      dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newBalance);
//    }

    double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
    logger.info("issuedSupply: " + issuedSupply);
    long out = (long) issuedSupply;
    supply += out;

    return out;
  }

  private long exchangeToSupply2(boolean isTRX, long quant) {
    logger.info("isTRX: " + isTRX);
    long balance = isTRX ? dynamicPropertiesStore.getTotalStoragePool() :
        dynamicPropertiesStore.getTotalStorageReserved();
    logger.info("balance: " + balance);
    long newBalance = balance - quant;
    logger.info("balance - quant: " + (balance - quant));

//    if (isTRX) {
//      dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newBalance);
//    } else {
//      dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newBalance);
//    }

    double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
    logger.info("issuedSupply: " + issuedSupply);
    long out = (long) issuedSupply;
    supply += out;

    return out;
  }

  private long exchange_from_supply(boolean isTRX, long supplyQuant) {
    long balance = isTRX ? dynamicPropertiesStore.getTotalStoragePool() :
        dynamicPropertiesStore.getTotalStorageReserved();
    supply -= supplyQuant;

    double exchangeBalance =
        balance * (Math.pow(1.0 + (double) supplyQuant / supply, 2000.0) - 1.0);
    logger.info("exchangeBalance: " + exchangeBalance);
    long out = (long) exchangeBalance;

    if (isTRX) {
      out = Math.round(exchangeBalance / 100000) * 100000;
      logger.info("---out: " + out);
    }

    return out;
  }

  public long exchange(long from, boolean isTRX) {
    long relay = exchangeToSupply(isTRX, from);
    return exchange_from_supply(!isTRX, relay);
  }

  public long calculateTax(long duration, long limit) {
    // todo: Support for change by the committee
    double ratePerYear = dynamicPropertiesStore.getStorageExchangeTaxRate() / 100.0;
    double millisecondPerYear = (double) MS_PER_YEAR;
    double feeRate = duration / millisecondPerYear * ratePerYear;
    long storageTax = (long) (limit * feeRate);
    logger.info("storageTax: " + storageTax);
    return storageTax;
  }


  public long tryPayTax(long duration, long limit) {
    long storageTax = calculateTax(duration, limit);
    long tax = exchange(storageTax, false);
    logger.info("tax: " + tax);

    long newTotalTax = dynamicPropertiesStore.getTotalStorageTax() + tax;
    long newTotalPool = dynamicPropertiesStore.getTotalStoragePool() - tax;
    long newTotalReserved = dynamicPropertiesStore.getTotalStorageReserved()
        + storageTax;
    logger.info("reserved: " + dynamicPropertiesStore.getTotalStorageReserved());
    boolean eq = dynamicPropertiesStore.getTotalStorageReserved()
        == 128L * 1024 * 1024 * 1024;
    logger.info("reserved == 128GB: " + eq);
    logger.info("newTotalTax: " + newTotalTax + "  newTotalPool: " + newTotalPool
        + NEW_TOTAL_RESERVED + newTotalReserved);

    return storageTax;
  }

  public long payTax(long duration, long limit) {
    long storageTax = calculateTax(duration, limit);
    long tax = exchange(storageTax, false);
    logger.info("tax: " + tax);

    long newTotalTax = dynamicPropertiesStore.getTotalStorageTax() + tax;
    long newTotalPool = dynamicPropertiesStore.getTotalStoragePool() - tax;
    long newTotalReserved = dynamicPropertiesStore.getTotalStorageReserved()
        + storageTax;
    logger.info("reserved: " + dynamicPropertiesStore.getTotalStorageReserved());
    boolean eq = dynamicPropertiesStore.getTotalStorageReserved()
        == 128L * 1024 * 1024 * 1024;
    logger.info("reserved == 128GB: " + eq);
    logger.info("newTotalTax: " + newTotalTax + "  newTotalPool: " + newTotalPool
        + NEW_TOTAL_RESERVED + newTotalReserved);
    dynamicPropertiesStore.saveTotalStorageTax(newTotalTax);
    dynamicPropertiesStore.saveTotalStoragePool(newTotalPool);
    dynamicPropertiesStore.saveTotalStorageReserved(newTotalReserved);

    return storageTax;
  }

  public long tryBuyStorageBytes(long storageBought) {
    long relay = exchangeToSupply2(false, storageBought);
    return exchange_from_supply(true, relay);
  }

  public long tryBuyStorage(long quant) {
    return exchange(quant, true);
  }

  public long trySellStorage(long bytes) {
    return exchange(bytes, false);
  }

  public AccountCapsule buyStorageBytes(AccountCapsule accountCapsule, long storageBought) {
    long now = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    long currentStorageLimit = accountCapsule.getStorageLimit();

    long relay = exchangeToSupply2(false, storageBought);
    long quant = exchange_from_supply(true, relay);

    long newBalance = accountCapsule.getBalance() - quant;
    logger.info("newBalance: " + newBalance);

    long newStorageLimit = currentStorageLimit + storageBought;
    logger.info(
        "storageBought: " + storageBought + NEW_STORAGE_LIMIT
            + newStorageLimit);

    accountCapsule.setLatestExchangeStorageTime(now);
    accountCapsule.setStorageLimit(newStorageLimit);
    accountCapsule.setBalance(newBalance);
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    long newTotalPool = dynamicPropertiesStore.getTotalStoragePool() + quant;
    long newTotalReserved = dynamicPropertiesStore.getTotalStorageReserved()
        - storageBought;
    logger.info(NEW_TOTAL_POOL + newTotalPool + NEW_TOTAL_RESERVED + newTotalReserved);
    dynamicPropertiesStore.saveTotalStoragePool(newTotalPool);
    dynamicPropertiesStore.saveTotalStorageReserved(newTotalReserved);
    return accountCapsule;
  }


  public void buyStorage(AccountCapsule accountCapsule, long quant) {
    long now = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    long currentStorageLimit = accountCapsule.getStorageLimit();

    long newBalance = accountCapsule.getBalance() - quant;
    logger.info("newBalance: " + newBalance);

    long storageBought = exchange(quant, true);
    long newStorageLimit = currentStorageLimit + storageBought;
    logger.info(
        "storageBought: " + storageBought + NEW_STORAGE_LIMIT
            + newStorageLimit);

    accountCapsule.setLatestExchangeStorageTime(now);
    accountCapsule.setStorageLimit(newStorageLimit);
    accountCapsule.setBalance(newBalance);
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    long newTotalPool = dynamicPropertiesStore.getTotalStoragePool() + quant;
    long newTotalReserved = dynamicPropertiesStore.getTotalStorageReserved()
        - storageBought;
    logger.info(NEW_TOTAL_POOL + newTotalPool + NEW_TOTAL_RESERVED + newTotalReserved);
    dynamicPropertiesStore.saveTotalStoragePool(newTotalPool);
    dynamicPropertiesStore.saveTotalStorageReserved(newTotalReserved);

  }

  public void sellStorage(AccountCapsule accountCapsule, long bytes) {
    long now = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    long currentStorageLimit = accountCapsule.getStorageLimit();

    long quant = exchange(bytes, false);
    long newBalance = accountCapsule.getBalance() + quant;

    long newStorageLimit = currentStorageLimit - bytes;
    logger.info("quant: " + quant + NEW_STORAGE_LIMIT + newStorageLimit);

    accountCapsule.setLatestExchangeStorageTime(now);
    accountCapsule.setStorageLimit(newStorageLimit);
    accountCapsule.setBalance(newBalance);
    accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    long newTotalPool = dynamicPropertiesStore.getTotalStoragePool() - quant;
    long newTotalReserved = dynamicPropertiesStore.getTotalStorageReserved()
        + bytes;
    logger.info(NEW_TOTAL_POOL + newTotalPool + NEW_TOTAL_RESERVED + newTotalReserved);
    dynamicPropertiesStore.saveTotalStoragePool(newTotalPool);
    dynamicPropertiesStore.saveTotalStorageReserved(newTotalReserved);

  }

  public long getAccountLeftStorageInByteFromBought(AccountCapsule accountCapsule) {
    return accountCapsule.getStorageLimit() - accountCapsule.getStorageUsage();
  }
}
