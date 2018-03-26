package org.tron.core.db;

import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.config.args.Args;

public abstract class TronDatabase<T> {

  protected LevelDbDataSourceImpl dbSource;

  protected TronDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(), dbName);
    dbSource.initDB();
  }

  protected TronDatabase() {
    throw new IllegalStateException("This constructor is not allowed");
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

}
