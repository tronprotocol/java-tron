package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;

public class AccountStore extends TronDatabase<AccountCapsule> {

  private static final Logger logger = LoggerFactory.getLogger("AccountStore");
  private static AccountStore instance;


  private AccountStore(String dbName) {
    super(dbName);
  }

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
  public void put(byte[] key, AccountCapsule item) {
    logger.info("address is {} ", key);

    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(value)) {
      onModify(key, value);
    }

    logger.info("address is {} ", ByteArray.toHexString(key));
    dbSource.putData(key, item.getData());

    if (ArrayUtils.isEmpty(value)) {
      onCreate(key);
    }
  }

  @Override
  public void delete(byte[] key) {
    // This should be called just before an object is removed.
    onDelete(key);
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  /**
   * isAccountExist fun.
   *
   * @param key the address of Account
   */
  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    logger.info("address is {},account is {}", key, account);
    return null != account;
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
   * get all accounts.
   */
  public List<AccountCapsule> getAllAccounts() {
    return dbSource.allValues().stream().map(bytes ->
        new AccountCapsule(bytes)
    ).collect(Collectors.toList());
  }
}
