package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.ProtoCapsule;

@Slf4j
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> extends TronDatabase<T> {

  protected TronStoreWithRevoking(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, T item) {
    logger.info("Address is {}, " + item.getClass().getSimpleName() + " is {}", key, item);

    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(value)) {
      onModify(key, value);
    }

    dbSource.putData(key, item.getData());

    if (ArrayUtils.isEmpty(value)) {
      onCreate(key);
    }
  }

  @Override
  public void delete(byte[] key) {
    onDelete(key);
    dbSource.deleteData(key);
  }
}
