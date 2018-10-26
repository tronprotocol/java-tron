package org.tron.common.runtime.vm.cache;

public interface CachedSource<K, V> {
//    void delete(K key);
    void put(K key, V value);
//    boolean containsKey(K key);
    V get(K key);
    void commit();
}
