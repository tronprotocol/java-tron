package org.tron.core.db2.common;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.db.common.iterator.DBIterator;

public class LevelDB implements DB<byte[], byte[]>, Flusher {

  @Getter
  private LevelDbDataSourceImpl db;
  private WriteOptionsWrapper writeOptions = WriteOptionsWrapper.getInstance()
      .sync(CommonParameter.getInstance().getStorage().isDbSync());

  public LevelDB(LevelDbDataSourceImpl db) {
    this.db = db;
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
  public long size() {
    return db.getTotal();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public void remove(byte[] key) {
    db.deleteData(key);
  }

  @Override
  public String getDbName() {
    return db.getDBName();
  }

  @Override
  public DBIterator iterator() {
    return db.iterator();
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    Map<byte[], byte[]> rows = batch.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll);
    db.updateByBatch(rows, writeOptions);
  }

  @Override
  public void close() {
    db.closeDB();
  }

  @Override
  public void reset() {
    db.resetDb();
  }

  @Override
  public LevelDB newInstance() {
    return new LevelDB(db.newInstance());
  }
}
