package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadItemException;

public class BlockMessage extends TronMessage {

  //private Block block;

  private BlockCapsule block;

  public BlockMessage(byte[] data) throws InvalidProtocolBufferException, BadItemException {
    this.type = MessageTypes.BLOCK.asByte();
    this.data = data;
    this.block = new BlockCapsule(data);
  }

//  public BlockMessage(Block block) {
//    this.block = block;
//    this.type = MessageTypes.BLOCK.asByte();
//    this.data = block.toByteArray();
//  }

  public BlockMessage(BlockCapsule block) {
    data = block.getData();
    this.type = MessageTypes.BLOCK.asByte();
    this.block = block;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public Sha256Hash getMessageId() {
    return getBlockCapsule().getBlockId();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public BlockId getBlockId() {
    return getBlockCapsule().getBlockId();
  }

//  public Block getBlock() {
//    return block;
//  }

  public BlockCapsule getBlockCapsule() {
    return block;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(block.getBlockId())
        .append("trx size: ").append(block.getTransactions().size()).append("\n").toString();
  }
}
