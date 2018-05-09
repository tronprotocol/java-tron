package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;

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
    put(accountCapsule.getAccountName().toByteArray(),
        new BytesCapsule(accountCapsule.getAddress().toByteArray()));
  }

  public byte[] get(ByteString name) {
    BytesCapsule bytesCapsule = get(name.toByteArray());
    if (Objects.nonNull(bytesCapsule)) {
      return bytesCapsule.getData();
    }
    return null;
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