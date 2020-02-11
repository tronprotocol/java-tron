package org.tron.tool.litefullnode.iterator;

import java.io.Closeable;

public interface DBIterator extends Closeable {

  public void seek(byte[] key);

  public void seekToFirst();

  public boolean hasNext();

  public byte[] getKey();

  public byte[] getValue();

  public void next();
}
