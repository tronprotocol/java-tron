package org.tron.core.db2.core;

import java.util.Map;
import org.tron.core.db2.common.Instance;

public interface Snapshot<T> extends Iterable<Map.Entry<byte[], T>>, Instance<Snapshot<T>> {

  boolean isRoot();

  boolean isImpl();

  T get(byte[] key);

  void put(byte[] key, T value);

  void remove(byte[] key);

  void merge(Snapshot from);

  Snapshot<T> advance(Class<T> clz);

  Snapshot<T> retreat();

  Snapshot<T> getPrevious();

  void setPrevious(Snapshot<T> previous);

  Snapshot getRoot();

  Snapshot<T> getNext();

  void setNext(Snapshot<T> next);

  Snapshot<T> getSolidity();

  void close();

  void reset();

  void resetSolidity();

  void updateSolidity();

  String getDbName();

  boolean isOptimized();
}
