package org.tron.core.net.message;

import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.BlockInventory.Type;

import java.util.List;

public class SyncBlockChainMessage extends BlockInventoryMessage {

  public SyncBlockChainMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.SYNC_BLOCK_CHAIN.asByte();
  }

  public SyncBlockChainMessage(List<BlockId> blockIds) {
    super(blockIds, Type.SYNC);
    this.type = MessageTypes.SYNC_BLOCK_CHAIN.asByte();
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getType().toString()).append(": ");
    getBlockIds().forEach(blockId -> sb.append("\n").append(blockId.getString()));
    return sb.toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return ChainInventoryMessage.class;
  }
}
