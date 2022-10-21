package org.tron.utils.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface DBInterface extends Closeable {

  byte[] get(byte[] key);

  void put(byte[] key, byte[] value);

  void delete(byte[] key);

  DBIterator iterator();

  long size();

  void close() throws IOException;

  List<String> getStats();

}
