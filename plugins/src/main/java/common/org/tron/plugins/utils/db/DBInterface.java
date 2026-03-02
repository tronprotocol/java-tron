package org.tron.plugins.utils.db;

import java.io.Closeable;
import java.io.IOException;


public interface DBInterface extends Closeable {

  byte[] get(byte[] key);

  void put(byte[] key, byte[] value);

  void delete(byte[] key);

  /**
   * Returns an iterator over the database.
   *
   * <p><b>CRITICAL:</b> The returned iterator holds native resources and <b>MUST</b> be closed
   * after use to prevent memory leaks. It is strongly recommended to use a try-with-resources
   * statement.
   *
   * <p>Example of correct usage:
   * <pre>{@code
   * try (DBIterator iterator = db.iterator()) {
   *  // do something
   * }
   * }</pre>
   *
   * @return a new database iterator that must be closed.
   */
  DBIterator iterator();

  long size() throws IOException;

  void close() throws IOException;

  String getName();

}
