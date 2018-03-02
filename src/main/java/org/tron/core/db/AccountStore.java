package org.tron.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountStore extends TronDatabase {

  private static final Logger logger = LoggerFactory.getLogger("AccountStore");

  private AccountStore(String dbName) {
    super(dbName);
  }

  private static AccountStore instance;

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AccountStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new AccountStore(dbName);
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

  public boolean createAccount(byte[] address, byte[] account) {
    dbSource.putData(address,account);
    logger.info("address is {},account is {}", address, account);
    return true;
  }

  public boolean isAccountExist(byte[] address) {
    byte[] account = dbSource.getData(address);
    logger.info("address is {},account is {}", address, account);
    return null != account;
  }

}
