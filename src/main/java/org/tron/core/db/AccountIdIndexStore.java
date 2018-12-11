package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.ACCOUNT_ID_INDEX;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.AccountIdIndexStoreTrie;

//todo ： need Compatibility test
@Component
public class AccountIdIndexStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private AccountIdIndexStoreTrie accountIdIndexStoreTrie;

  @Autowired
  public AccountIdIndexStore(@Value("accountid-index") String dbName) {
    super(dbName);
  }

  public void put(AccountCapsule accountCapsule) {
    byte[] lowerCaseAccountId = getLowerCaseAccountId(accountCapsule.getAccountId().toByteArray());
    super.put(lowerCaseAccountId, new BytesCapsule(accountCapsule.getAddress().toByteArray()));
    fastSyncCallBack.callBack(lowerCaseAccountId, accountCapsule.getAddress().toByteArray(),
        ACCOUNT_ID_INDEX);
  }

  public byte[] get(ByteString name) {
    BytesCapsule bytesCapsule = get(name.toByteArray());
    if (Objects.nonNull(bytesCapsule)) {
      return bytesCapsule.getData();
    }
    return null;
  }

  public byte[] getOnSolidity(ByteString name) {
    BytesCapsule bytesCapsule = getOnSolidity(name.toByteArray());
    if (Objects.nonNull(bytesCapsule)) {
      return bytesCapsule.getData();
    }
    return null;
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] lowerCaseKey = getLowerCaseAccountId(key);
    byte[] value = getValue(lowerCaseKey);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }

  @Override
  public BytesCapsule getOnSolidity(byte[] key) {
    byte[] lowerCaseKey = getLowerCaseAccountId(key);
    byte[] value = getSolidityValue(lowerCaseKey);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] lowerCaseKey = getLowerCaseAccountId(key);
    byte[] value = getValue(lowerCaseKey);
    return !ArrayUtils.isEmpty(value);
  }

  private static byte[] getLowerCaseAccountId(byte[] bsAccountId) {
    return ByteString
        .copyFromUtf8(ByteString.copyFrom(bsAccountId).toStringUtf8().toLowerCase()).toByteArray();
  }

  private byte[] getValue(byte[] key) {
    byte[] value = accountIdIndexStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  private byte[] getSolidityValue(byte[] key) {
    byte[] value = accountIdIndexStoreTrie.getSolidityValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUncheckedOnSolidity(key);
    }
    return value;
  }

}