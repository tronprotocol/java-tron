package org.tron.core.db.common.iterator;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.primitives.Bytes;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public interface DBIterator extends Iterator<Entry<byte[], byte[]>>, AutoCloseable, Closeable {

  void seek(byte[] key);

  void seekToFirst();

  void seekToLast();

  default UnmodifiableIterator<Entry<byte[], byte[]>> prefixQueryAfterThat
      (byte[] key, byte[] afterThat) {
    this.seek(afterThat == null ? key : afterThat);
    return Iterators.filter(this, entry -> Bytes.indexOf(entry.getKey(), key) == 0);
  }

  /**
   * An iterator is either positioned at a key/value pair, or
   * not valid.  This method returns true iff the iterator is valid.
   *
   * REQUIRES: iterator not closed
   *
   * @throws IllegalStateException if the iterator is closed.
   * @return an iterator is either positioned at a key/value pair
   */
  boolean valid();

  /**
   * The underlying storage for
   * the returned slice is valid only until the next modification of
   * the iterator.
   *
   * REQUIRES: valid() && !closed
   *
   * @throws IllegalStateException if the iterator is closed.
   * @throws NoSuchElementException if the iterator is not valid.
   *
   * @return the key for the current entry
   */
  byte[] getKey();

  /**
   * The underlying storage for
   * the returned slice is valid only until the next modification of
   * the iterator.
   *
   * REQUIRES: valid() && !closed
   *
   * @throws IllegalStateException if the iterator is closed.
   * @throws NoSuchElementException if the iterator is not valid.
   *
   * @return the value for the current entry
   */
  byte[] getValue();

  /**
   * @throws IllegalStateException if the iterator is closed.
   */
  void checkState();

  /**
   * @throws NoSuchElementException if the iterator is not valid.
   */
  default void checkValid() {
    if (!valid()) {
      throw new NoSuchElementException();
    }
  }
}
