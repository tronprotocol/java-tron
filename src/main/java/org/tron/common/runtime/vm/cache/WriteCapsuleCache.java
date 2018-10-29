package org.tron.common.runtime.vm.cache;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;

@Slf4j(topic = "vm_write_cache")
public class WriteCapsuleCache<V extends ProtoCapsule> implements CachedSource<byte[], V> {
  private CachedSource<byte[], V> backingSource;
  private ByteArrayMap<V> writeCache;

  public WriteCapsuleCache(CachedSource<byte[], V> backingSource) {
    this.backingSource = backingSource;
    this.writeCache = new ByteArrayMap<>();
  }

  @Override
  public void put(byte[] key, V value) {
    writeCache.put(key, value);
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
      this.backingSource.put(key, value);
    });
  }
}
