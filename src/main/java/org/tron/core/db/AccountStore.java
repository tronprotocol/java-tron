package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.AccountCapsule;

public class AccountStore extends TronDatabase<AccountCapsule> {

  private static final Logger logger = LoggerFactory.getLogger("AccountStore");

  private AccountStore(String dbName) {
    super(dbName);
  }

  @Override
  void putItem(byte[] key, AccountCapsule item) {

  }

  @Override
  void deleteItem(byte[] key) {

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
  public AccountCapsule getItem(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
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
        .map(key -> getItem(key))
        .collect(Collectors.toList());
  }
}
