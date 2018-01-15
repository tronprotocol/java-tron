package org.tron.storage;

import java.util.Map;


public interface BatchSourceInter<K, V> extends SourceInter<K, V> {


  void updateByBatch(Map<K, V> rows);
}
