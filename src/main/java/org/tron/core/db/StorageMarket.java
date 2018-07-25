package org.tron.core.db;

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

}
