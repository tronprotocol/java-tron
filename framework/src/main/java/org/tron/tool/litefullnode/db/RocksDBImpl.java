package org.tron.tool.litefullnode.db;

import com.google.common.collect.Streams;
import java.io.IOException;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.tron.tool.litefullnode.iterator.DBIterator;
import org.tron.tool.litefullnode.iterator.RockDBIterator;

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
}
