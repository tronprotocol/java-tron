package org.tron.core.db2.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConcurrentHashDB implements DB<byte[], byte[]> {

  private Map<Key, byte[]> db = new ConcurrentHashMap<>();


  @Override
  public byte[] get(byte[] bytes) {
    return db.get(Key.of(bytes));
  }

  @Override
  public void put(byte[] bytes, byte[] bytes2) {
    db.put(Key.of(bytes), bytes2);
  }

  @Override
  public long size() {
    return db.size();
  }

  @Override
  public boolean isEmpty() {
    return db.isEmpty();
  }

  @Override
  public void remove(byte[] bytes) {
    db.remove(Key.of(bytes));
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return new Iterator<Entry<byte[], byte[]>>() {
      @Override
      public boolean hasNext() {
        return db.entrySet().iterator().hasNext();
      }

      @Override
      public Entry<byte[], byte[]> next() {
        return transferEntry(db.entrySet().iterator().next());
      }

      @Override
      public void remove() {
        db.entrySet().iterator().remove();
      }

      @Override
      public void forEachRemaining(Consumer<? super Entry<byte[], byte[]>> action) {
        Consumer<Entry<Key, byte[]>> consumer = new Consumer<Entry<Key, byte[]>>() {
          @Override
          public void accept(Entry<Key, byte[]> keyEntry) {
            action.accept(transferEntry(keyEntry));
          }
        };
        db.entrySet().iterator().forEachRemaining(consumer);
      }
    };
  }

  private Entry<byte[], byte[]> transferEntry(Entry<Key, byte[]> entry) {
    Entry<byte[], byte[]> e = new Entry<byte[], byte[]>() {
      @Override
      public byte[] getKey() {
        return entry.getKey().getBytes();
      }

      @Override
      public byte[] getValue() {
        return entry.getValue();
      }

      @Override
      public byte[] setValue(byte[] value) {
        return entry.setValue(value);
      }
    };
    return e;
  }

}
