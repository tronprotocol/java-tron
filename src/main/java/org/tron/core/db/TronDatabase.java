package org.tron.core.db;

import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.config.args.Args;

public abstract class TronDatabase<T> {

  protected LevelDbDataSourceImpl dbSource;

  protected TronDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(), dbName);
    dbSource.initDB();
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  public void close() {
    dbSource.closeDB();
  }

  abstract void putItem(byte[] key, T item);

  abstract void deleteItem(byte[] key);

  public abstract T getItem(byte[] key);

}
