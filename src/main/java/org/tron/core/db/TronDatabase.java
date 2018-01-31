package org.tron.core.db;

import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;

public abstract class TronDatabase {

  public LevelDbDataSourceImpl dbSource;

  protected TronDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl("database", dbName);
    dbSource.initDB();
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  abstract void add();

  abstract void del();

  abstract void fetch();

}
