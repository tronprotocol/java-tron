package org.tron.tool.litefullnode.iterator;

import java.io.IOException;
import org.rocksdb.RocksIterator;

public class RockDBIterator implements DBIterator {

  private RocksIterator iterator;

  public RockDBIterator(RocksIterator iterator) {
    this.iterator = iterator;
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
  public void next() {
    iterator.next();
  }

  @Override
  public void close() throws IOException {
    iterator.close();
  }
}
