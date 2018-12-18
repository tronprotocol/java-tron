package org.tron.common.storage;


import org.rocksdb.WriteOptions;

public interface SourceInterRocks<K, V> {

  void putData(K key, V val);

  void putData(K k, V v, WriteOptions options);

  V getData(K key);

  void deleteData(K key);

  void deleteData(K k, WriteOptions options);

  boolean flush();
}
