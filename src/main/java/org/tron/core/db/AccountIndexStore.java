package org.tron.core.db;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class AccountIndexStore extends TronStoreWithRevoking<BytesCapsule> {


  @Autowired
  public AccountIndexStore(@Qualifier("account-index") String dbName) {
    super(dbName);

  }

  private static AccountIndexStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AccountIndexStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountIndexStore.class) {
        if (instance == null) {
          instance = new AccountIndexStore(dbName);
        }
      }
    }
    return instance;
  }

  public void put(AccountCapsule accountCapsule) {
    put(accountCapsule.getName().toByteArray(),
        new BytesCapsule(accountCapsule.getAddress().toByteArray()));
  }

  public byte[] get(String name)
      throws ItemNotFoundException {
    return get(ByteArray.fromString(name)).getData();
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }


  @Override
  public boolean has(byte[] key) {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      return false;
    }
    return true;
  }
}