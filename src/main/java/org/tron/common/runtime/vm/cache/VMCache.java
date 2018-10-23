package org.tron.common.runtime.vm.cache;

public interface VMCache<K, V> {



  void delete(K key);
  void put(K key, V value);
  void commit();

}
