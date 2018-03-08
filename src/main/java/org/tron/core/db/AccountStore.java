package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol.Account;

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

  /**
   * get account by address.
   */
  public AccountCapsule getAccount(ByteString address) {
    logger.info("address is {} ", address);

    byte[] value = dbSource.getData(address.toByteArray());
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
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

  /**
   * save account.
   */
  public void putAccount(ByteString address, AccountCapsule account) {
    logger.info("address is {} ", address);

    dbSource.putData(address.toByteArray(), account.getData());
  }

  public void putAccount(AccountCapsule accountCapsule) {
    dbSource.putData(accountCapsule.getAddress().toByteArray(), accountCapsule.getData());
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

  public boolean createAccount(byte[] address, AccountCapsule account) {
    dbSource.putData(address, account.getData());
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
   * get all accounts.
   */
  public List<AccountCapsule> getAllAccounts() {
    return dbSource.allKeys().stream()
        .map(key -> getAccount(ByteString.copyFrom(key)))
        .collect(Collectors.toList());
  }
}
