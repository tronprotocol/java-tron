package org.tron.common.runtime.vm.cache;

import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;


public class ReadWriteCapsuleCache<V extends ProtoCapsule> implements CachedSource<byte[], V>{
    private ByteArrayMap<V> cache;
    private TronStoreWithRevoking<V> store;

    public ReadWriteCapsuleCache(ByteArrayMap<V> cache, TronStoreWithRevoking<V> store) {
        this.cache = cache;
        this.store = store;
    }

    @Override
    public void delete(byte[] key) {
        cache.put(key, null);
    }

    @Override
    public void put(byte[] key, V value) {
        cache.put(key, value);
    }

    @Override
    public boolean containsKey(byte[] key) {
        if (cache.containsKey(key)) {
            return true;
        }
        V v;
        try {
            v = store.get(key);
            cache.put(key, v);
        } catch (ItemNotFoundException | BadItemException e) {
            v = null;
        }
        return v == null;
    }

    @Override
    public V get(byte[] key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        } else {
            V v;
            try {
                v = store.get(key);
            } catch (ItemNotFoundException | BadItemException e) {
                v = null;
            }
            // cached null
            cache.put(key, v);
            return v;
        }
    }

    @Override
    public void commit() {

    }
}

