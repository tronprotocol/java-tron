package org.tron.core.db.common.iterator;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DBIterator;


@Slf4j(topic = "DB")
public final class StoreIterator implements org.tron.core.db.common.iterator.DBIterator {

  private final DBIterator dbIterator;
  private boolean first = true;

  private final AtomicBoolean close = new AtomicBoolean(false);

  public StoreIterator(DBIterator dbIterator) {
    this.dbIterator = dbIterator;
  }

  @Override
  public void close() throws IOException {
    if (close.compareAndSet(false, true)) {
      dbIterator.close();
    }
  }

  @Override
  public boolean hasNext() {
    if (close.get()) {
      return false;
    }

    boolean hasNext = false;
    // true is first item
    try {
      if (first) {
        dbIterator.seekToFirst();
        first = false;
      }

      if (!(hasNext = dbIterator.hasNext())) { // false is last item
        close();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }

    return hasNext;
  }

  @Override
  public Entry<byte[], byte[]> next() {
    if (close.get()) {
      throw new NoSuchElementException();
    }
    return dbIterator.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void seek(byte[] key) {
    checkState();
    dbIterator.seek(key);
    this.first = false;
  }

  @Override
  public void seekToFirst() {
    checkState();
    dbIterator.seekToFirst();
    this.first = false;
  }

  @Override
  public void seekToLast() {
    checkState();
    dbIterator.seekToLast();
    this.first = false;
  }

  @Override
  public boolean valid() {
    checkState();
    return dbIterator.hasNext();
  }

  @Override
  public byte[] getKey() {
    checkState();
    checkValid();
    return dbIterator.peekNext().getKey();
  }

  @Override
  public byte[] getValue() {
    checkState();
    checkValid();
    return dbIterator.peekNext().getValue();
  }

  @Override
  public void checkState() {
    if (close.get()) {
      throw new IllegalStateException("iterator has been closed");
    }
  }
}

