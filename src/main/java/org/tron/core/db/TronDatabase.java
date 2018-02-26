package org.tron.core.db;

import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.Constant;

public abstract class TronDatabase {

  protected LevelDbDataSourceImpl dbSource;

  protected TronDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl(Constant.OUTPUT_DIR, dbName);
    dbSource.initDB();
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  public void close() {
    dbSource.closeDB();
  }

  abstract void add();

  abstract void del();

  abstract void fetch();

  void addItem(byte[] key, byte[] val) {
    dbSource.putData(key, val);
  }

}
