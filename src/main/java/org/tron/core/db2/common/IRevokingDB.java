package org.tron.core.db2.common;

import org.tron.core.exception.ItemNotFoundException;

import java.util.Map;

public interface IRevokingDB extends Iterable<Map.Entry<byte[], byte[]>> {
  void put(byte[] key, byte[] newValue);

  void delete(byte[] key);

  boolean has(byte[] key);

  byte[] get(byte[] key) throws ItemNotFoundException;

  byte[] getUnchecked(byte[] key);

  void close();

  void reset();

}
