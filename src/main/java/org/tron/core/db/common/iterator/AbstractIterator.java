package org.tron.core.db.common.iterator;

import java.util.Iterator;
import java.util.Map;

public abstract class AbstractIterator<T> implements Iterator<T> {

  protected Iterator<Map.Entry<byte[], byte[]>> iterator;

  AbstractIterator(Iterator<Map.Entry<byte[], byte[]>> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }
}
