package org.tron.core.db.common.iterator;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractIterator<T> implements Iterator<Map.Entry<byte[], T>> {

  protected Iterator<Map.Entry<byte[], byte[]>> iterator;
  private TypeToken<T> typeToken = new TypeToken<T>(getClass()) {};

  public AbstractIterator(Iterator<Map.Entry<byte[], byte[]>> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  protected T of(byte[] value) {
    try {
      @SuppressWarnings("unchecked")
      T t = (T) typeToken.getRawType().getConstructor(byte[].class).newInstance(value);
      return t;
    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map.Entry<byte[], T> next() {
    Entry<byte[], byte[]> entry = iterator.next();
    return Maps.immutableEntry(entry.getKey(), of(entry.getValue()));
  }
}
