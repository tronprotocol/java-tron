package org.tron.core.db2.common;

import java.util.Iterator;
import java.util.Map;

public interface DB<K, V> extends Iterable<Map.Entry<K, V>>, Instance<DB<K, V>> {

  V get(K k);

  void put(K k, V v);

  long size();

  boolean isEmpty();

  void remove(K k);

  Iterator iterator();

  void close();

  String getDbName();
}
