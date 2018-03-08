package org.tron.core.db;

import org.tron.core.capsule.TransactionCapsule;

public class TransactionStore extends TronDatabase<TransactionCapsule> {

  private TransactionStore(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionCapsule item) {

  }


  @Override
  public void delete(byte[] key) {

  }

  @Override
  public TransactionCapsule get(byte[] key) {
    return null;
  }


  @Override
  public boolean has(byte[] key) {
    return false;
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
