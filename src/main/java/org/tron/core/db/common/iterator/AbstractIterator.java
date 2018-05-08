package org.tron.core.db.common.iterator;

import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractIterator<T> implements Iterator<Map.Entry<byte[], T>> {

  protected Iterator<Map.Entry<byte[], byte[]>> iterator;

  AbstractIterator(Iterator<Map.Entry<byte[], byte[]>> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  protected abstract T of(byte[] value);

  @Override
  public Map.Entry<byte[], T> next() {
    Entry<byte[], byte[]> entry = iterator.next();
    return Maps.immutableEntry(entry.getKey(), of(entry.getValue()));
  }
}
