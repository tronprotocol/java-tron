package org.tron.core.db;

public class TransactionStore extends TronDatabase {

  private TransactionStore(String dbName) {
    super(dbName);
  }

  @Override
  void putItem(byte[] key, Object item) {

  }

  @Override
  void deleteItem(byte[] key) {

  }

  @Override
  public Object getItem(byte[] key) {
    return null;
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
