package org.tron.plugins.utils.db;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksIterator;

public class RockDBIterator implements DBIterator {

  private final RocksIterator iterator;
  private final ReadOptions readOptions;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public RockDBIterator(RocksIterator iterator, ReadOptions readOptions) {
    this.iterator = iterator;
    this.readOptions = readOptions;
  }

  @Override
  public boolean valid() {
    return iterator.isValid();
  }

  @Override
  public void seek(byte[] key) {
    iterator.seek(key);
  }

  @Override
  public void seekToFirst() {
    iterator.seekToFirst();
  }

  @Override
  public void seekToLast() {
    iterator.seekToLast();
  }

  @Override
  public boolean hasNext() {
    return iterator.isValid();
  }

  @Override
  public byte[] getKey() {
    return iterator.key();
  }

  @Override
  public byte[] getValue() {
    return iterator.value();
  }

  @Override
  public Map.Entry<byte[], byte[]> next() {
    byte[] key = iterator.key();
    byte[] value = iterator.value();
    iterator.next();
    return new Map.Entry<byte[], byte[]>() {
      @Override
      public byte[] getKey() {
        return key;
      }

      @Override
      public byte[] getValue() {
        return value;
      }

      @Override
      public byte[] setValue(byte[] value) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      readOptions.close();
      iterator.close();
    }
  }
}
