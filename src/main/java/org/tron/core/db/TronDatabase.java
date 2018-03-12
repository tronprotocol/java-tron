package org.tron.core.db;

import java.util.Objects;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.config.args.Args;
import org.tron.core.db.AbstractRevokingStore.RevokingTuple;

public abstract class TronDatabase<T> {

  protected LevelDbDataSourceImpl dbSource;

  protected TronDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(), dbName);
    dbSource.initDB();
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key);

  public abstract boolean has(byte[] key);

  /**
   * This should be called just after an object is created
   */
  void onCreate(byte[] key) {
    RevokingStore.getInstance().onCreate(new RevokingTuple(dbSource, key), null);
  }

  /**
   * This should be called just before an object is modified
   */
  void onModify(byte[] key, byte[] value) {
    RevokingStore.getInstance().onModify(new RevokingTuple(dbSource, key), value);
  }

  /**
   * This should be called just before an object is removed.
   */
  void onDelete(byte[] key) {
    byte[] value;
    if (Objects.nonNull(value = dbSource.getData(key))) {
      RevokingStore.getInstance().onRemove(new RevokingTuple(dbSource, key), value);
    }
  }

}
