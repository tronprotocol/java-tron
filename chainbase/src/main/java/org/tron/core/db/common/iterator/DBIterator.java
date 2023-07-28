package org.tron.core.db.common.iterator;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.primitives.Bytes;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;

public interface DBIterator extends Iterator<Entry<byte[], byte[]>>, AutoCloseable, Closeable {

  void seek(byte[] key);

  void seekToFirst();

  void seekToLast();

  default UnmodifiableIterator<Entry<byte[], byte[]>> prefixQueryAfterThat
      (byte[] key, byte[] afterThat) {
    this.seek(afterThat == null ? key : afterThat);
    return Iterators.filter(this, entry -> Bytes.indexOf(entry.getKey(), key) == 0);
  }
}
