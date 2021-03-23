package org.tron.tool.litefullnode.iterator;

import java.io.IOException;

public class LevelDBIterator implements DBIterator {

  private org.iq80.leveldb.DBIterator iterator;

  public LevelDBIterator(org.iq80.leveldb.DBIterator iterator) {
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
  public void next() {
    iterator.next();
  }

  @Override
  public void close() throws IOException {
    iterator.close();
  }
}
