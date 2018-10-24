package org.tron.common.runtime.vm.cache;

import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;

public class WriteCapsuleCache<V extends ProtoCapsule> implements CachedSource<byte[], V> {
  private CachedSource<byte[], V> backingSource;
  private ByteArrayMap<V> writeCache;

  public WriteCapsuleCache(CachedSource<byte[], V> backingSource) {
    this.backingSource = backingSource;
    this.writeCache = new ByteArrayMap<>();
  }

  @Override
  public void delete(byte[] key) {
    writeCache.put(key, null);
  }

  @Override
  public void put(byte[] key, V value) {
    writeCache.put(key, value);
  }

  @Override
  public boolean containsKey(byte[] key) {
    return writeCache.containsKey(key) || backingSource.containsKey(key);
  }

  @Override
  public V get(byte[] key) {
    if (writeCache.containsKey(key)) {
      return writeCache.get(key);
    }
    return backingSource.get(key);
  }

  @Override
  public void commit() {
    writeCache.forEach((key, value) -> {
      if (value == null) {
        this.backingSource.delete(key);
      } else {
        this.backingSource.put(key, value);
      }
    });
  }
}
