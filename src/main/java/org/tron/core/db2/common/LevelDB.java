package org.tron.core.db2.common;

import com.google.common.collect.Maps;
import lombok.Getter;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Snapshot;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db.common.iterator.DBIterator;

import java.util.Map;
import java.util.stream.Collectors;

public class LevelDB implements DB<byte[], byte[]> {
  @Getter
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

  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    Map<byte[], byte[]> rows = batch.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    db.updateByBatch(rows, writeOptions);
  }

  public void close() {
    db.closeDB();
  }

  public void reset() {
    db.resetDb();
  }
}
