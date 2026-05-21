package org.tron.common.storage;

import java.io.Closeable;

public class WriteOptionsWrapper implements Closeable {

  public org.rocksdb.WriteOptions rocks = null;
  public org.iq80.leveldb.WriteOptions level = null;

  private WriteOptionsWrapper() {

  }

  /**
   * Returns an WriteOptionsWrapper.
   *
   * <p><b>CRITICAL:</b> The returned WriteOptionsWrapper holds native resources
   * and <b>MUST</b> be closed
   * after use to prevent memory leaks. It is strongly recommended to use a try-with-resources
   * statement.
   *
   * <p>Example of correct usage:
   * <pre>{@code
   * try ( WriteOptionsWrapper readOptions = WriteOptionsWrapper.getInstance()) {
   *  // do something
   * }
   * }</pre>
   *
   * @return a new WriteOptionsWrapper that must be closed.
   */
  public static WriteOptionsWrapper getInstance() {
    WriteOptionsWrapper wrapper = new WriteOptionsWrapper();
    wrapper.level = new org.iq80.leveldb.WriteOptions();
    wrapper.rocks = new org.rocksdb.WriteOptions();

    return wrapper;
  }


  public WriteOptionsWrapper sync(boolean bool) {
    this.level.sync(bool);
    this.rocks.setSync(bool);
    return this;
  }

  @Override
  public void close() {
    if (rocks != null) {
      rocks.close();
    }
    // leveldb WriteOptions has no close method, and does not need to be closed
  }
}
