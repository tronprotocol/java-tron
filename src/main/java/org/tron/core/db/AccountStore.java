package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;

@Slf4j
public class AccountStore extends TronStoreWithRevoking<AccountCapsule> {

  private static final String ACCOUNT_SUN_ADDRESS
      = "4948c2e8a756d9437037dcd8c7e0c73d560ca38d";

  private static final String ACCOUNT_BLACKHOLE_ADDRESS
      = "548794500882809695a8a687866e76d4271a146a";

  private static final String ACCOUNT_ZION_ADDRESS
      = "55ddae14564f82d5b94c7a131b5fcfd31ad6515a";

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
    byte[] data = dbSource.getData(
        ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_SUN_ADDRESS))
            .toByteArray());

    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  /**
   * Min TRX account.
   */
  public AccountCapsule getBlackhole() {
    byte[] data = dbSource.getData(
        ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_BLACKHOLE_ADDRESS))
            .toByteArray());

    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getZion() {
    byte[] data = dbSource.getData(
        ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ZION_ADDRESS))
            .toByteArray());

    AccountCapsule accountCapsule = new AccountCapsule(data);
    return accountCapsule;
  }
}
