package org.tron.plugins.utils.db;

import java.io.IOException;

import org.iq80.leveldb.DBException;
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
    return new RockDBIterator(rocksDB.newIterator(
        new org.rocksdb.ReadOptions().setFillCache(false)));
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

  @Override
  public void compactRange(byte[] begin, byte[] end) throws DBException, RocksDBException {
    rocksDB.compactRange(begin, end);
  }

  @Override
  public void compactRange() throws DBException, RocksDBException {
    rocksDB.compactRange();
  }

}
