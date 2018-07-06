package org.tron.core.db2.snapshot;

import java.util.Map;

public interface Snapshot extends Iterable<Map.Entry<byte[], byte[]>> {

  byte[] get(byte[] key);

  void put(byte[] key, byte[] value);

  void remove(byte[] key);

  void merge(Snapshot from);

  void close();
}
