package org.tron.core.db;

import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;

@Slf4j
public class AccountStore extends TronStoreWithRevoking<AccountCapsule> {

  private static Map<String, String> assertsAddress = new HashMap<String, String>();

  private AccountStore(String dbName) {
    super(dbName);
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AccountStore create(String dbName) {
    return new AccountStore(dbName);
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

  /**
   * get all accounts.
   */
  public List<AccountCapsule> getAllAccounts() {
    return dbSource.allValues().stream().map(bytes ->
        new AccountCapsule(bytes)
    ).collect(Collectors.toList());
  }

  /**
   * Max TRX account.
   */
  public AccountCapsule getSun() {
    byte[] data = dbSource.getData((ByteArray.fromHexString(assertsAddress.get("Sun"))));
    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  /**
   * Min TRX account.
   */
  public AccountCapsule getBlackhole() {
    byte[] data = dbSource.getData((ByteArray.fromHexString(assertsAddress.get("Blackhole"))));
    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getZion() {
    byte[] data = dbSource.getData((ByteArray.fromHexString(assertsAddress.get("Zion"))));
    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      String address = obj.get("address").unwrapped().toString();
      assertsAddress.put(accountName, address);
    }
  }
}
