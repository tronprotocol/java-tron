package org.tron.tool.litefullnode.iterator;

import java.io.Closeable;

public interface DBIterator extends Closeable {

  void seek(byte[] key);

  void seekToFirst();

  boolean hasNext();

  byte[] getKey();

  byte[] getValue();

  void next();
}
