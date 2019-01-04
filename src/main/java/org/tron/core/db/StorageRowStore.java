package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.STORAGE;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.StorageRowStoreTrie;

@Slf4j(topic = "DB")
@Component
public class StorageRowStore extends TronStoreWithRevoking<StorageRowCapsule> {

  private static StorageRowStore instance;

  @Autowired
  private StorageRowStoreTrie storageRowStoreTrie;

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private StorageRowStore(@Value("storage-row") String dbName) {
    super(dbName);
  }

  @Override
  public StorageRowCapsule get(byte[] key) {
    StorageRowCapsule row = getValue(key);
    row.setRowKey(key);
    return row;
  }

  @Override
  public void put(byte[] key, StorageRowCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getAllData(), STORAGE);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, STORAGE);
  }

  @Override
  public void close() {
    super.close();
    storageRowStoreTrie.close();
  }

  public StorageRowCapsule getValue(byte[] key) {
    byte[] value = storageRowStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      return getUnchecked(key);
    }
    return new StorageRowCapsule(key, value);
  }

}
