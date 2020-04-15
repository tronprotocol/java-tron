package org.tron.core.vm.repository;

import lombok.Getter;

public class WriteOptionsWrapper {

  @Getter
  private org.rocksdb.WriteOptions rocks = null;
  @Getter
  private org.iq80.leveldb.WriteOptions level = null;

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
