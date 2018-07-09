package org.tron.core.db2.common;

import org.iq80.leveldb.WriteOptions;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.db.common.iterator.DBIterator;

public class LevelDB implements DB<byte[], byte[]> {
  private LevelDbDataSourceImpl db;
  private WriteOptions writeOptions = new WriteOptions().sync(true);

  public LevelDB(String parentName, String name) {
    db = new LevelDbDataSourceImpl(parentName, name);
  }

  @Override
  public byte[] get(byte[] key) {
    return db.getData(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    db.putData(key, value);
  }

  @Override
  public void remove(byte[] key) {
    db.deleteData(key);
  }

  @Override
  public DBIterator iterator() {
    return db.iterator();
  }

  public void close() {
    db.closeDB();
  }
}
