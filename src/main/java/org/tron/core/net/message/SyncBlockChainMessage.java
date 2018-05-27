package org.tron.core.net.message;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.BlockInventory.Type;

public class SyncBlockChainMessage extends BlockInventoryMessage {

  public SyncBlockChainMessage(byte[] packed) throws Exception {
    super(packed);
    this.type = MessageTypes.SYNC_BLOCK_CHAIN.asByte();
  }

  public SyncBlockChainMessage(List<BlockId> blockIds) {
    super(blockIds, Type.SYNC);
    this.type = MessageTypes.SYNC_BLOCK_CHAIN.asByte();
  }

  @Override
  public String toString() {
    List<BlockId> blockIdList = getBlockIds();
    StringBuilder sb = new StringBuilder();
    int size = blockIdList.size();
    sb.append(super.toString()).append("size: ").append(size);
    if (size >= 1) {
      sb.append(", start block: " + blockIdList.get(0).getString());
      if (size > 1) {
        sb.append(", end block " + blockIdList.get(blockIdList.size() - 1).getString());
      }
    }
    return sb.toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return ChainInventoryMessage.class;
  }
}
