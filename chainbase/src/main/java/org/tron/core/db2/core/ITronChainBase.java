package org.tron.core.db2.core;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map.Entry;
import org.tron.common.utils.Quitable;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

public interface ITronChainBase<T> extends Iterable<Entry<byte[], T>>, Quitable {

  /**
   * reset the database.
   */
  void reset();

  /**
   * close the database.
   */
  void close();

  void put(byte[] key, T item);

  void delete(byte[] key);

  T get(byte[] key) throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  T getUnchecked(byte[] key);

  boolean has(byte[] key);

  String getName();

  String getDbName();

}
