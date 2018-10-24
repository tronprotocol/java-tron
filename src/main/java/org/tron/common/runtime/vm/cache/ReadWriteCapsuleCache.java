package org.tron.common.runtime.vm.cache;


import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j(topic = "cache")
public class ReadWriteCapsuleCache<V extends ProtoCapsule> implements CachedSource<byte[], V> {
  private ByteArrayMap<V> writeCache;
  private ByteArrayMap<V> readCache;
  private TronStoreWithRevoking<V> store;

  public ReadWriteCapsuleCache(TronStoreWithRevoking<V> store) {
    this.store = store;
    writeCache = new ByteArrayMap<>();
    readCache = new ByteArrayMap<>();
  }

  @Override
  public void put(byte[] key, V value) {
    writeCache.put(key, value);
  }

  @Override
  public V get(byte[] key) {
    if(writeCache.containsKey(key)) {
      return writeCache.get(key);
    }

    if (readCache.containsKey(key)) {
      return readCache.get(key);
    } else {
      V value;
      try {
        value = this.store.get(key);
      } catch (ItemNotFoundException | BadItemException e) {
        value = null;
      }
      // Ensure each key should be visit once, though value is null
      readCache.put(key, value);
    }
    return readCache.get(key);
  }

  @Override
  public void commit() {
    writeCache.forEach((key, value) -> {
      logger.warn("read cache commit, key" + Hex.toHexString(key) + " value:" + value);
      if (value == null) {
        this.store.delete(key);
      } else {
        this.store.put(key, value);
      }
    });
  }
}
