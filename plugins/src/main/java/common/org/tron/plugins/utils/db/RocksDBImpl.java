package org.tron.plugins.utils.db;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.DBUtils;

public class RocksDBImpl implements DBInterface {

  private final RocksDB rocksDB;

  @Getter
  private final String name;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private Options options = null;

  public RocksDBImpl(Path path, String name) throws RocksDBException {
    try {
      this.options = DBUtils.newDefaultRocksDbOptions(false, name);
      this.name = name;
      this.rocksDB = RocksDB.open(options, path.toString());
    } catch (RocksDBException e) {
      if (this.options != null) {
        this.options.close();
      }
      throw e;
    }
  }

  @Override
  public byte[] get(byte[] key) {
    throwIfClosed();
    try {
      return rocksDB.get(key);
    } catch (RocksDBException e) {
      throw new RuntimeException(name, e);
    }
  }

  @Override
  public void put(byte[] key, byte[] value) {
    throwIfClosed();
    try {
      rocksDB.put(key, value);
    } catch (RocksDBException e) {
      throw new RuntimeException(name, e);
    }
  }

  @Override
  public void delete(byte[] key) {
    throwIfClosed();
    try {
      rocksDB.delete(key);
    } catch (RocksDBException e) {
      throw new RuntimeException(name, e);
    }
  }

  @Override
  public DBIterator iterator() {
    throwIfClosed();
    ReadOptions readOptions = new ReadOptions().setFillCache(false);
    return new RockDBIterator(rocksDB.newIterator(readOptions), readOptions);
  }

  @Override
  public long size() throws IOException {
    throwIfClosed();
    try (DBIterator iterator = this.iterator()) {
      iterator.seekToFirst();
      return Streams.stream(iterator).count();
    }
  }

  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      if (this.options != null) {
        this.options.close();
      }
      rocksDB.close();
    }
  }

  private void throwIfClosed() {
    if (closed.get()) {
      throw new IllegalStateException("db " + name + " has been closed");
    }
  }
}
