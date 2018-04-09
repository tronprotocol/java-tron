package org.tron.core.db.common;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import lombok.Value;
import org.iq80.leveldb.DBIterator;

@Value(staticConstructor = "of")
public final class StoreIterator implements Iterator<Map.Entry<byte[], byte[]>>, Closeable {

  private DBIterator dbIterator;

  @Override
  public void close() throws IOException {
    dbIterator.close();
  }

  @Override
  public boolean hasNext() {
    return dbIterator.hasNext();
  }

  @Override
  public Entry<byte[], byte[]> next() {
    return dbIterator.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEachRemaining(Consumer<? super Entry<byte[], byte[]>> action) {
    dbIterator.forEachRemaining(action);
  }
}
