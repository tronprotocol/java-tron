package org.tron.core.db2.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.ItemNotFoundException;

public interface IRevokingDB<T extends ProtoCapsule> extends Iterable<Map.Entry<byte[], T>> {

  void put(byte[] key, T newValue);

  void delete(byte[] key);

  boolean has(byte[] key);

  T get(byte[] key) throws ItemNotFoundException;

  T getFromRoot(byte[] key) throws ItemNotFoundException;

  T getUnchecked(byte[] key);

  void close();

  void reset();

  void setCursor(Chainbase.Cursor cursor);

  void setCursor(Chainbase.Cursor cursor, long offset);

  Chainbase.Cursor getCursor();

  // for blockstore
  Set<T> getlatestValues(long limit);

  // for blockstore
  Set<T> getValuesNext(byte[] key, long limit);

  List<byte[]> getKeysNext(byte[] key, long limit);

  Map<WrappedByteArray, T> prefixQuery(byte[] key);

  default Map<byte[], T> getNext(byte[] key, long limit) {
    return Collections.emptyMap();
  }

}
