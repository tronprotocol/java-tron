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
