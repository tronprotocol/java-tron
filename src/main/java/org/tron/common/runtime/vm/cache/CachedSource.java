package org.tron.common.runtime.vm.cache;

public interface CachedSource<K, V> {
    void put(K key, V value);
    V get(K key);
    void commit();
}
