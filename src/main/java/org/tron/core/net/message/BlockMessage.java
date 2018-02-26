package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
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
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  @Override
  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getBlock().getBlockHeader().toByteArray());
  }

  public Block getBlock() {
    unPack();
    return block;
  }

  public BlockCapsule getBlockCapsule() {
    return new BlockCapsule(getBlock());
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
