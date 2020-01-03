package org.tron.core.event.entity;

import com.google.protobuf.ByteString;
import org.tron.core.event.BaseEvent;

public class PbftBlockEvent extends BaseEvent {

  private long blockNum;
  private ByteString blockHash;

  public PbftBlockEvent(long blockNum, ByteString blockHash) {
    this.blockNum = blockNum;
    this.blockHash = blockHash;
  }

  public long getBlockNum() {
    return blockNum;
  }

  public PbftBlockEvent setBlockNum(long blockNum) {
    this.blockNum = blockNum;
    return this;
  }

  public ByteString getBlockHash() {
    return blockHash;
  }

  public PbftBlockEvent setBlockHash(ByteString blockHash) {
    this.blockHash = blockHash;
    return this;
  }
}
