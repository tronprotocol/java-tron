package org.tron.common.storage;

import java.util.Map;
import org.rocksdb.WriteOptions;


public interface BatchSourceInterRocks<K, V> extends SourceInterRocks<K, V> {


  void updateByBatch(Map<K, V> rows);

  void updateByBatch(Map<K, V> rows, WriteOptions writeOptions);

  void putData(K key, V val);

  V getData(K key);


  void deleteData(K key);

  boolean flush();

}
