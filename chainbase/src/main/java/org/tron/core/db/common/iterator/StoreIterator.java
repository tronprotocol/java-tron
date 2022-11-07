package org.tron.core.db.common.iterator;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DBIterator;

@Slf4j(topic = "DB")
public final class StoreIterator implements org.tron.core.db.common.iterator.DBIterator {

  private DBIterator dbIterator;
  private boolean first = true;

  private volatile boolean valid = true;

  public StoreIterator(DBIterator dbIterator) {
    this.dbIterator = dbIterator;
  }

  @Override
  public void close() throws IOException {
    dbIterator.close();
  }

  @Override
  public boolean hasNext() {
    if (!valid) {
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
        dbIterator.close();
        valid = false;
      }
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
    }

    return hasNext;
  }

  @Override
  public Entry<byte[], byte[]> next() {
    if (!valid) {
      throw new NoSuchElementException();
    }
    return dbIterator.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
