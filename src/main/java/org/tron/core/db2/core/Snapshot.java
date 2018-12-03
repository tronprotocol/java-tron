package org.tron.core.db2.core;

import java.util.Map;

public interface Snapshot extends Iterable<Map.Entry<byte[], byte[]>> {

  static boolean isRoot(Snapshot snapshot) {
    return snapshot != null && snapshot.getClass() == SnapshotRoot.class;
  }

  static boolean isImpl(Snapshot snapshot) {
    return snapshot != null && snapshot.getClass() == SnapshotImpl.class;
  }

  byte[] get(byte[] key);

  void put(byte[] key, byte[] value);

  void remove(byte[] key);

  void merge(Snapshot from);

  Snapshot advance();

  Snapshot retreat();

  Snapshot getPrevious();

  void setPrevious(Snapshot previous);

  Snapshot getRoot();

  Snapshot getNext();

  void setNext(Snapshot next);

  Snapshot getSolidity();

  void close();

  void reset();

  void resetSolidity();

  void updateSolidity();
}
