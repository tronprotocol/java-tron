package org.tron.tool.litefullnode.db;

import com.google.common.primitives.Bytes;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.tron.tool.litefullnode.iterator.DBIterator;
import org.tron.tool.litefullnode.iterator.RockDBIterator;

public class RocksDBImpl extends TronDB {

  private final org.rocksdb.RocksDB rocksDB;
  private final String name;
  private final String sourceDir;
  private TronDB tmp;

  private final ReadOptions readOptions;

  public RocksDBImpl(String sourceDir,  org.rocksdb.RocksDB rocksDB, String name) {
    this.rocksDB = rocksDB;
    this.name = name;
    this.sourceDir = sourceDir;
    this.readOptions = new ReadOptions().setFillCache(false);
    Path path = Paths.get(sourceDir, name);
    DB_MAP.put(path.toString(), this);
  }

  @Override
  public byte[] get(byte[] key) {
    try {
      if ("tmp".equalsIgnoreCase(name)) {
        return rocksDB.get(key);
      }

      if (tmp == null) {
        Path path = Paths.get(sourceDir, "tmp");
        tmp = DB_MAP.get(path.toString());
      }

      byte[] value;
      byte[] valueFromTmp = tmp.get(Bytes.concat(simpleEncode(name), key));
      if (isEmptyBytes(valueFromTmp)) {
        value = rocksDB.get(key);
      } else {
        value = valueFromTmp.length == 1
            ? null : Arrays.copyOfRange(valueFromTmp, 1, valueFromTmp.length);
      }
      return value;
    } catch (RocksDBException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public byte[] getDir(byte[] key) {
    try {
      return rocksDB.get(readOptions, key);
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
    try (ReadOptions options = new ReadOptions().setFillCache(false)) {
      return new RockDBIterator(rocksDB.newIterator(options));
    }
  }

  @Override
  public void close() throws IOException {
    this.readOptions.close();
    rocksDB.close();
  }
}
