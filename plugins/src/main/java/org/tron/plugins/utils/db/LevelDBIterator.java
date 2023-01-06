package org.tron.plugins.utils.db;

import java.io.IOException;
import java.util.Map;

public class LevelDBIterator implements DBIterator {

  private final org.iq80.leveldb.DBIterator iterator;

  public LevelDBIterator(org.iq80.leveldb.DBIterator iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean valid() {
    return iterator.hasNext();
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
    return iterator.hasNext();
  }

  @Override
  public byte[] getKey() {
    return iterator.peekNext().getKey();
  }

  @Override
  public byte[] getValue() {
    return iterator.peekNext().getValue();
  }

  @Override
  public Map.Entry<byte[], byte[]> next() {
    return iterator.next();
  }

  @Override
  public void close() throws IOException {
    iterator.close();
  }
}
