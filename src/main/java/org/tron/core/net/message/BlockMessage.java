package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;

public class BlockMessage extends TronMessage {

  private Block block;

  public BlockMessage(byte[] data) throws InvalidProtocolBufferException {
    this.type = MessageTypes.BLOCK.asByte();
    this.data = data;
    this.block = Protocol.Block.parseFrom(data);
  }

  public BlockMessage(Block block) {
    this.block = block;
    this.type = MessageTypes.BLOCK.asByte();
    this.data = block.toByteArray();
  }

  public BlockMessage(BlockCapsule block) {
    data = block.getData();
    this.type = MessageTypes.BLOCK.asByte();
    this.block = block.getInstance();
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

  public Block getBlock() {
    return block;
  }

  public BlockCapsule getBlockCapsule() {
    return new BlockCapsule(getBlock());
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(block.getBlockHeader().getRawData())
        .append("trx size: ").append(block.getTransactionsList().size()).append("\n").toString();
  }
}
