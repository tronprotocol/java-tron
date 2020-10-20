package org.tron.common.storage;

public class WriteOptionsWrapper {

  public org.rocksdb.WriteOptions rocks = null;
  public org.iq80.leveldb.WriteOptions level = null;

  private WriteOptionsWrapper() {

  }

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
}
