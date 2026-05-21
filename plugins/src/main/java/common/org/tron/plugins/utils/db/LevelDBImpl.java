package org.tron.plugins.utils.db;

import com.google.common.collect.Streams;
import java.io.IOException;
import lombok.Getter;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;


public class LevelDBImpl implements DBInterface {

  private DB leveldb;

  @Getter
  private final String name;

  public LevelDBImpl(DB leveldb, String name) {
    this.leveldb = leveldb;
    this.name = name;
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
    return new LevelDBIterator(leveldb.iterator(new ReadOptions().fillCache(false)));
  }

  @Override
  public long size() throws IOException {
    try (DBIterator iterator = this.iterator()) {
      iterator.seekToFirst();
      return Streams.stream(iterator).count();
    }
  }

  @Override
  public void close() throws IOException {
    leveldb.close();
  }
}
