package org.tron.utils.db;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class RocksDBImpl implements DBInterface {

  private org.rocksdb.RocksDB rocksDB;

  public RocksDBImpl(org.rocksdb.RocksDB rocksDB) {
    this.rocksDB = rocksDB;
  }

  @Override
  public byte[] get(byte[] key) {
    try {
      return rocksDB.get(key);
    } catch (RocksDBException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    try {
      rocksDB.put(key, value);
    } catch (RocksDBException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void delete(byte[] key) {
    try {
      rocksDB.delete(key);
    } catch (RocksDBException e) {
      e.printStackTrace();
    }
  }

  @Override
  public DBIterator iterator() {
    return new RockDBIterator(rocksDB.newIterator());
  }

  @Override
  public long size() {
    RocksIterator iterator = rocksDB.newIterator();
    long size = 0;
    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
      size++;
    }
    iterator.close();
    return size;
  }

  @Override
  public void close() throws IOException {
    rocksDB.close();
  }

  /**
   * Level Files Size(MB)
   * --------------------
   *   0        5       10
   *   1      134      254
   *   2     1311     2559
   *   3     1976     4005
   *   4        0        0
   *   5        0        0
   *   6        0        0
   */
  @Override
  public List<String> getStats() {
    String stat = null;
    try {
      stat = rocksDB.getProperty("rocksdb.levelstats");
    } catch (RocksDBException e) {
      System.out.println("get status failed, err: " + e.getMessage());
    }
    String[] stats = stat.split("\n");
    return Arrays.stream(stats).skip(2).collect(Collectors.toList());
  }
}
