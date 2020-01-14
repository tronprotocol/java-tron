package org.tron.core.event.entity;

import com.google.protobuf.ByteString;
import org.tron.core.event.BaseEvent;

public class PbftBlockCommitEvent extends BaseEvent {

  private long blockNum;
  private ByteString blockHash;

  public PbftBlockCommitEvent(long blockNum, ByteString blockHash) {
    this.blockNum = blockNum;
    this.blockHash = blockHash;
  }

  public long getBlockNum() {
    return blockNum;
  }

  public PbftBlockCommitEvent setBlockNum(long blockNum) {
    this.blockNum = blockNum;
    return this;
  }

  public ByteString getBlockHash() {
    return blockHash;
  }

  public PbftBlockCommitEvent setBlockHash(ByteString blockHash) {
    this.blockHash = blockHash;
    return this;
  }
}
