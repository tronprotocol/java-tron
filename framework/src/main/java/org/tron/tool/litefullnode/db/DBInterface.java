package org.tron.tool.litefullnode.db;

import java.io.Closeable;
import java.io.IOException;
import org.tron.tool.litefullnode.iterator.DBIterator;

public interface DBInterface extends Closeable {

  public byte[] get(byte[] key);

  public void put(byte[] key, byte[] value);

  public void delete(byte[] key);

  public DBIterator iterator();

  public long size();

  public void close() throws IOException;

}
