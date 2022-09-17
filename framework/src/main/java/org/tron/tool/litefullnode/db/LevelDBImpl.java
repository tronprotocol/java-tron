package org.tron.tool.litefullnode.db;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Map;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.tron.tool.litefullnode.iterator.DBIterator;
import org.tron.tool.litefullnode.iterator.LevelDBIterator;

public class LevelDBImpl implements DBInterface {

  private DB leveldb;

  public LevelDBImpl(DB leveldb) {
    this.leveldb = leveldb;
  }

  @Override
  public byte[] get(byte[] key) {
    return leveldb.get(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    leveldb.put(key, value);
  }

  @Override
  public void delete(byte[] key) {
    leveldb.delete(key);
  }

  @Override
  public DBIterator iterator() {
    return new LevelDBIterator(leveldb.iterator());
  }

  @Override
  public long size() {
    return Streams.stream(leveldb.iterator()).count();
  }

  @Override
  public void close() throws IOException {
    leveldb.close();
  }

  @Override
  public void batch(Map<byte[], byte[]> rows) throws IOException {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    try (WriteBatch batch = leveldb.createWriteBatch()) {
      rows.forEach((key, value) -> {
        if (value == null) {
          batch.delete(key);
        } else {
          batch.put(key, value);
        }
      });
      leveldb.write(batch);
    }
  }
}
