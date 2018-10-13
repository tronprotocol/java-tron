package org.tron.core.db2.core;

import java.util.Map;

public interface Snapshot extends Iterable<Map.Entry<byte[], byte[]>> {

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

  void close();

  void reset();
}
