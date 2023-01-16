package org.tron.plugins.utils.db;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;

public interface DBIterator extends Iterator<Map.Entry<byte[], byte[]>>, Closeable {

  /**
   * An iterator is either positioned at a key/value pair, or
   * not valid.  This method returns true iff the iterator is valid.
   *
   * @return an iterator is either positioned at a key/value pair
   */
  boolean valid();

  /**
   * Position at the first key in the source that is at or past target.
   * The iterator is valid() after this call iff the source contains
   * an entry that comes at or past target.
   *
   * @param key target
   */
  void seek(byte[] key);

  /**
   * Position at the first key in the source.  The iterator is valid()
   * after this call iff the source is not empty.
   */
  void seekToFirst();

  /**
   * Position at the last key in the source.  The iterator is
   * valid() after this call iff the source is not empty.
   */
  void seekToLast();

  boolean hasNext();

  /**
   * The underlying storage for
   * the returned slice is valid only until the next modification of
   * the iterator.
   * REQUIRES: valid()
   *
   * @return the key for the current entry
   */
  byte[] getKey();

  /**
   * The underlying storage for
   * the returned slice is valid only until the next modification of
   * the iterator.
   * REQUIRES: valid()
   *
   * @return the value for the current entry
   */
  byte[] getValue();
}
