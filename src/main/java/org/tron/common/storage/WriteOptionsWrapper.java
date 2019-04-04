package org.tron.common.storage;

import lombok.Getter;

public class WriteOptionsWrapper {

  @Getter
  private org.rocksdb.WriteOptions rocks = null;
  @Getter
  private org.iq80.leveldb.WriteOptions level = null;

  public static WriteOptionsWrapper getInstance() {
    WriteOptionsWrapper wapper = new WriteOptionsWrapper();
    wapper.level = new org.iq80.leveldb.WriteOptions();
    wapper.rocks = new org.rocksdb.WriteOptions();
    return wapper;
  }

  public WriteOptionsWrapper sync(boolean bool) {
    this.level.sync(bool);
    this.rocks.setSync(bool);
    return this;
  }
}