package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.Block;

@Slf4j
public class BlockMessage extends Message {

  private Block block;

  public BlockMessage(byte[] packed) {
    super(packed);
  }

  public BlockMessage(Block block) {
    this.block = block;
    unpacked = true;
  }

  public BlockMessage(BlockCapsule block) {
    data = block.getData();
    unpacked = false;
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
    return getBlockCapsule().getBlockId();
    //return Sha256Hash.of(getBlock().getBlockHeader().toByteArray());
  }

  public BlockId getBlockId() {
    return getBlockCapsule().getBlockId();
    //return Sha256Hash.of(getBlock().getBlockHeader().toByteArray());
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
