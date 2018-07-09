package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.StorageCapsule;

@Slf4j
public class StorageStore extends TronStoreWithRevoking<StorageCapsule> {

  private StorageStore(String dbName) {
    super(dbName);
  }

  @Override
  public StorageCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new StorageCapsule(value);
  }


  @Override
  public boolean has(byte[] key) {
    byte[] transaction = dbSource.getData(key);
    return null != transaction;
  }

  /**
   * get total storages.
   */
  public long getTotalStorages() {
    return dbSource.getTotal();
  }

  private static StorageStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  /**
   * create Fun.
   */
  public static StorageStore create(String dbName) {
    if (instance == null) {
      synchronized (StorageStore.class) {
        if (instance == null) {
          instance = new StorageStore(dbName);
        }
      }
    }
    return instance;
  }

  /**
   * find a storage  by it's key.
   */
  public byte[] findStorageByKey(byte[] key) {
    return dbSource.getData(key);
  }

}
