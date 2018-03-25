package org.tron.core.db;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db.AbstractRevokingStore.RevokingTuple;

@Slf4j
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> extends TronDatabase<T> {

  private RevokingDatabase revokingDatabase;

  protected TronStoreWithRevoking(String dbName) {
    this(dbName, RevokingStore.getInstance());
  }

  // only for unit test
  protected TronStoreWithRevoking(String dbName, RevokingDatabase revokingDatabase) {
    super(dbName);
    this.revokingDatabase = revokingDatabase;
    revokingDatabase.enable();
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

  /**
   * This should be called just after an object is created
   */
  private void onCreate(byte[] key) {
    revokingDatabase.onCreate(new RevokingTuple(dbSource, key), null);
  }

  /**
   * This should be called just before an object is modified
   */
  private void onModify(byte[] key, byte[] value) {
    revokingDatabase.onModify(new RevokingTuple(dbSource, key), value);
  }

  /**
   * This should be called just before an object is removed.
   */
  private void onDelete(byte[] key) {
    byte[] value;
    if (Objects.nonNull(value = dbSource.getData(key))) {
      revokingDatabase.onRemove(new RevokingTuple(dbSource, key), value);
    }
  }
}
