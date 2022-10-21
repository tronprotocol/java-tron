package org.tron.utils.db;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.iq80.leveldb.DB;

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

  /**
   *                                Compactions
   * Level  Files Size(MB) Time(sec) Read(MB) Write(MB)
   * --------------------------------------------------
   *   1        2        2         0        0         2
   *   2        1        1         0        0         1
   */
  @Override
  public List<String> getStats() {
    String stat = leveldb.getProperty("leveldb.stats");
    String[] stats = stat.split("\n");
    return Arrays.stream(stats).skip(3).collect(Collectors.toList());
  }
}
