package org.tron.common.runtime.vm.cache;

import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;

public class WriteCapsuleCache<V extends ProtoCapsule> implements CachedSource<byte[], V>{
    CachedSource<byte[], V> backingSource;
    ByteArrayMap<V> writeCache;

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

    }
}
