package org.tron.core.db.common.iterator;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksIterator;


@Slf4j
public final class RockStoreIterator implements DBIterator {

  private RocksIterator dbIterator;
  private boolean first = true;

  public RockStoreIterator(RocksIterator dbIterator) {
    this.dbIterator = dbIterator;
  }

  @Override
  public void close() throws IOException {
    dbIterator.close();
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = false;
    // true is first item
    try {
      if (first) {
        dbIterator.seekToFirst();
        first = false;
      }
      if (!(hasNext = dbIterator.isValid())) { // false is last item
        dbIterator.close();
      }
    } catch (Exception e) {
      System.out.println("e:" + e);
      try {
        dbIterator.close();
      } catch (Exception e1) {
        System.out.println("e1:" + e1);
      }
    }
    return hasNext;
  }

  @Override
  public Entry<byte[], byte[]> next() {
    if (!dbIterator.isValid()) {
      throw new NoSuchElementException();
    }
    byte[] key = dbIterator.key();
    byte[] value = dbIterator.value();
    dbIterator.next();
    return new Entry<byte[], byte[]>() {
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
}