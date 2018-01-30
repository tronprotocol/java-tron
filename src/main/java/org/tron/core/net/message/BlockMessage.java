package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Protocal.Block;


public class BlockMessage extends Message {

  private Block block;

  public BlockMessage(byte[] packed) {
    super(packed);
  }

  public BlockMessage(Block block) {
    this.block = block;
    unpacked = true;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.BLOCK;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  public Block getBlock() {
    unPack();
    return block;
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }

    try {
      this.block = Block.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }

  private void pack() {
    this.data = this.block.toByteArray();
  }


}
