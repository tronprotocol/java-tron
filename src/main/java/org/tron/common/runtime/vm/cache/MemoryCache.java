package org.tron.common.runtime.vm.cache;

import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;

public class MemoryCache<V extends ProtoCapsule> implements CachedSource<byte[], V>{
  private ByteArrayMap<V> cache;
  public MemoryCache() {
    cache = new ByteArrayMap<>();
  }

  @Override
  public void put(byte[] key, V value) {
    cache.put(key, value);
  }

  @Override
  public V get(byte[] key) {
    return cache.get(key);
  }

  @Override
  public void commit() {
    // do nothing
  }
}
