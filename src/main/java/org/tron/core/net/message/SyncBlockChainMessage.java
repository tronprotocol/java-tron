package org.tron.core.net.message;

import java.util.List;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.BlockInventory.Type;

public class SyncBlockChainMessage extends BlockInventoryMessage {

  public SyncBlockChainMessage(byte[] packed) {
    super(packed);
  }

  public SyncBlockChainMessage(List<BlockId> blockIds) {
    super(blockIds, Type.SYNC);
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.SYNC_BLOCK_CHAIN;
  }
}
