package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.Block;

public class BlockMessage extends TronMessage {

  private Block block;

  public BlockMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.BLOCK.asByte();
  }

  public BlockMessage(Block block) {
    this.block = block;
    unpacked = true;
    this.type = MessageTypes.BLOCK.asByte();
  }

  public BlockMessage(BlockCapsule block) {
    data = block.getData();
    unpacked = false;
    this.type = MessageTypes.BLOCK.asByte();
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
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
