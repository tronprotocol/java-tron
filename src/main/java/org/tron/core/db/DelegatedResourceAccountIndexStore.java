package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.DELEGATED_RESOURCE_ACCOUNT_INDEX;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.DelegatedResourceAccountStoreTrie;

@Component
public class DelegatedResourceAccountIndexStore extends
    TronStoreWithRevoking<DelegatedResourceAccountIndexCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private DelegatedResourceAccountStoreTrie delegatedResourceAccountStoreTrie;

  @Autowired
  public DelegatedResourceAccountIndexStore(@Value("DelegatedResourceAccountIndex") String dbName) {
    super(dbName);
  }

  @Override
  public DelegatedResourceAccountIndexCapsule get(byte[] key) {

    byte[] value = getValue(key);
    return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceAccountIndexCapsule(value);
  }

  @Override
  public void put(byte[] key, DelegatedResourceAccountIndexCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), DELEGATED_RESOURCE_ACCOUNT_INDEX);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, DELEGATED_RESOURCE_ACCOUNT_INDEX);
  }

  public byte[] getValue(byte[] key) {
    byte[] value = delegatedResourceAccountStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  @Override
  public void close() {
    super.close();
    delegatedResourceAccountStoreTrie.close();
  }
}