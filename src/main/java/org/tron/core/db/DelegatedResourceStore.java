package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.DELEGATED_RESOURCE;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.DelegatedResourceStoreTrie;

@Component
public class DelegatedResourceStore extends TronStoreWithRevoking<DelegatedResourceCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private DelegatedResourceStoreTrie delegatedResourceStoreTrie;

  @Autowired
  public DelegatedResourceStore(@Value("DelegatedResource") String dbName) {
    super(dbName);
  }

  @Override
  public DelegatedResourceCapsule get(byte[] key) {

    byte[] value = getValue(key);
    return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceCapsule(value);
  }

  @Override
  public void put(byte[] key, DelegatedResourceCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), DELEGATED_RESOURCE);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, DELEGATED_RESOURCE);
  }

  public byte[] getValue(byte[] key) {
    byte[] value = delegatedResourceStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  @Override
  public void close() {
    super.close();
    delegatedResourceStoreTrie.close();
  }
}