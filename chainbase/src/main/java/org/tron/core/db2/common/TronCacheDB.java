package org.tron.core.db2.common;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "DB")
public class TronCacheDB implements DB<byte[], byte[]>, Flusher {

  private final String name;
  private final Map<WrappedByteArray, WrappedByteArray> db;

  public TronCacheDB(String name) {
    this.name = name;
    this.db = new ConcurrentHashMap<>();
  }

  @Override
  public byte[] get(byte[] key) {
    WrappedByteArray wrappedValue = this.db.get(WrappedByteArray.copyOf(key));
    return wrappedValue == null ? null : wrappedValue.getBytes();
  }

  @Override
  public void put(byte[] key, byte[] value) {
    if (key == null || value == null) {
      return;
    }
    this.put(WrappedByteArray.copyOf(key), WrappedByteArray.copyOf(value));
  }

  private void put(WrappedByteArray key, WrappedByteArray value) {
    this.db.put(key, value);
  }

  @Override
  public long size() {
    return this.db.size();
  }

  @Override
  public boolean isEmpty() {
    return this.size() == 0;
  }

  @Override
  public void remove(byte[] key) {
    if (key == null) {
      return;
    }
    this.db.remove(WrappedByteArray.copyOf(key));
  }

  @Override
  public String getDbName() {
    return name;
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    return Iterators.transform(this.db.entrySet().iterator(),
        e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()));
  }

  @Override
  public synchronized void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach(this::put);
  }

  @Override
  public void close() {
    this.reset();
  }

  @Override
  public void reset() {
    this.db.clear();
  }

  @Override
  public TronCacheDB newInstance() {
    return new TronCacheDB(name);
  }

  @Override
  public void stat() {
  }
}

