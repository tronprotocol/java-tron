package org.tron.core.db2.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.ItemNotFoundException;

public interface IRevokingDB extends Iterable<Map.Entry<byte[], byte[]>> {

  void put(byte[] key, byte[] newValue);

  void delete(byte[] key);

  boolean has(byte[] key);

  byte[] get(byte[] key) throws ItemNotFoundException;

  byte[] getFromRoot(byte[] key) throws ItemNotFoundException;

  byte[] getUnchecked(byte[] key);

  void close();

  void reset();

  void setCursor(Chainbase.Cursor cursor);

  void setCursor(Chainbase.Cursor cursor, long offset);

  Chainbase.Cursor getCursor();

  // for blockstore
  Set<byte[]> getlatestValues(long limit);

  // for blockstore
  Set<byte[]> getValuesNext(byte[] key, long limit);

  List<byte[]> getKeysNext(byte[] key, long limit);

  Map<WrappedByteArray, byte[]> prefixQuery(byte[] key);

  default Map<byte[], byte[]> getNext(byte[] key, long limit) {
    return Collections.emptyMap();
  }

}
