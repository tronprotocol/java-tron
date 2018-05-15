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
    StringBuilder sb = new StringBuilder(getType().toString()).append(": ");
    if (CollectionUtils.isNotEmpty(blockIdList)) {
      sb.append("\n BlockIds size " + blockIdList.size());
      sb.append("\n start block " + blockIdList.get(0));
      sb.append("\n end block " + blockIdList.get(blockIdList.size() - 1));
    } else {
      sb.append("BlockIds size 0");
    }
    return sb.toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return ChainInventoryMessage.class;
  }
}
