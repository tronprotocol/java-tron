package org.tron.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Protocal.Account;

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
  /**
   * createAccount fun.
   *
   * @param address the address of Account
   * @param account the data of Account
   */

  public boolean createAccount(byte[] address, byte[] account) {
    dbSource.putData(address, account);
    logger.info("address is {},account is {}", address, account);
    return true;
  }
  /**
   * isAccountExist fun.
   *
   * @param address the address of Account
   */

  public boolean isAccountExist(byte[] address) {
    byte[] account = dbSource.getData(address);
    logger.info("address is {},account is {}", address, account);
    return null != account;
  }
  /**
   * getAccount fun.
   *
   * @param address the address of Account
   */

  public Account getAccount(byte[] address) {
    byte[] account = dbSource.getData(address);
    if (account == null || account.length == 0) {
      return null;
    }
    try {
      return Account.parseFrom(account);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

}
