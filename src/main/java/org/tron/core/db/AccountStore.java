package org.tron.core.db;

import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.common.iterator.AccountIterator;

@Slf4j
@Component
public class AccountStore extends TronStoreWithRevoking<AccountCapsule> {

  private static Map<String, byte[]> assertsAddress =
      new HashMap<String, byte[]>(); // key = name , value = address
  private static AccountStore instance;

  @Autowired
  private AccountStore(@Qualifier("account") String dbName) {
    super(dbName);
  }

  public static void destroy() {
    instance = null;
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
    return null != account;
  }

  @Override
  public void put(byte[] key, AccountCapsule item) {
    if (indexHelper != null) {
      indexHelper.update(item.getInstance());
    }
    super.put(key, item);
  }

  /**
   * get all accounts.
   */
  public List<AccountCapsule> getAllAccounts() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new AccountCapsule(bytes))
        .collect(Collectors.toList());
  }

  /**
   * Max TRX account.
   */
  public AccountCapsule getSun() {
    byte[] data = dbSource.getData(assertsAddress.get("Sun"));
    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  /**
   * Min TRX account.
   */
  public AccountCapsule getBlackhole() {
    byte[] data = dbSource.getData(assertsAddress.get("Blackhole"));
    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getZion() {
    byte[] data = dbSource.getData(assertsAddress.get("Zion"));
    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Wallet.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public Iterator<AccountCapsule> iterator() {
    return new AccountIterator(dbSource.iterator());
  }
}
