package org.tron.core.db.common.iterator;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public interface DBIterator extends Iterator<Entry<byte[], byte[]>>, Closeable {

  void seek(byte[] key);

  byte[] key();
}
