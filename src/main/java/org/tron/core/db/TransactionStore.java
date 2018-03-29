package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.TransactionCapsule;

@Slf4j
public class TransactionStore extends TronStoreWithRevoking<TransactionCapsule> {

  private TransactionStore(String dbName) {
    super(dbName);
  }

  @Override
  public TransactionCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionCapsule(value);
  }


  @Override
  public boolean has(byte[] key) {
    byte[] transaction = dbSource.getData(key);
    logger.info("address is {}, transaction is {}", key, transaction);
    return null != transaction;
  }

  /**
   * get total transaction.
   */
  public long getTotalTransactions() {
    return dbSource.getTotal();
  }

  private static TransactionStore instance;


  /**
   * create Fun.
   */
  public static TransactionStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new TransactionStore(dbName);
        }
      }
    }
    return instance;
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findTransactionByHash(byte[] trxHash) {
    return dbSource.getData(trxHash);
  }

}
