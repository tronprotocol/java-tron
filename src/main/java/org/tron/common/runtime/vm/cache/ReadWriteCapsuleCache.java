package org.tron.common.runtime.vm.cache;


import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

public class ReadWriteCapsuleCache<V extends ProtoCapsule> implements CachedSource<byte[], V> {
  private ByteArrayMap<V>  writeCapsuleCache;
  private ByteArrayMap<V> readCapsuleCache;
  private TronStoreWithRevoking<V> store;

  public ReadWriteCapsuleCache(TronStoreWithRevoking<V> store) {
    this.store = store;
    writeCapsuleCache = new ByteArrayMap<>();
    readCapsuleCache = new ByteArrayMap<>();
  }

  @Override
  public void delete(byte[] key) {
    writeCapsuleCache.put(key, null);
  }

  @Override
  public void put(byte[] key, V value) {
    writeCapsuleCache.put(key, value);
  }

  @Override
  public boolean containsKey(byte[] key) {
    return writeCapsuleCache.containsKey(key) || readCapsuleCache.containsKey(key);
  }

  @Override
  public V get(byte[] key) {
    if(writeCapsuleCache.containsKey(key)) {
      return writeCapsuleCache.get(key);
    }

    if (readCapsuleCache.containsKey(key)) {
      return readCapsuleCache.get(key);
    } else {
      V value;
      try {
        value = this.store.get(key);
      } catch (ItemNotFoundException | BadItemException e) {
        value = null;
      }
      // Ensure each key should be visit once, though value is null
      readCapsuleCache.put(key, value);
    }
    return readCapsuleCache.get(key);
  }

  @Override
  public void commit() {
    writeCapsuleCache.forEach((key, value) -> {
      if (value == null) {
        this.store.delete(key);
      } else {
        this.store.put(key, value);
      }
    });
  }
}
