package org.tron.core.db;

public class TransactionStore extends TronDatabase {

  private TransactionStore(String dbName) {
    super(dbName);
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
   * find a transaction  by it's hash.
   */
  public byte[] findTransactionByHash(byte[] trxHash) {
    return dbSource.getData(trxHash);
  }

  @Override
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

  }
}
