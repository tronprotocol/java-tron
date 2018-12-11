package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.ACCOUNT_INDEX;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.AccountIndexStoreTrie;

@Component
public class AccountIndexStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private AccountIndexStoreTrie accountIndexStoreTrie;

  @Autowired
  public AccountIndexStore(@Value("account-index") String dbName) {
    super(dbName);
  }

  public void put(AccountCapsule accountCapsule) {
    put(accountCapsule.getAccountName().toByteArray(),
        new BytesCapsule(accountCapsule.getAddress().toByteArray()));
    fastSyncCallBack.callBack(accountCapsule.getAccountName().toByteArray(),
        accountCapsule.getAddress().toByteArray(), ACCOUNT_INDEX);
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
    byte[] value = getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] value = getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      return false;
    }
    return true;
  }

  private byte[] getValue(byte[] key) {
    byte[] value = accountIndexStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }
}