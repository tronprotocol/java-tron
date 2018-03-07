package org.tron.core.net.message;

import java.util.List;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocal.BlockInventory.Type;

public class ChainInventoryMessage extends BlockInventoryMessage {

  public ChainInventoryMessage(byte[] packed) {
    super(packed);
  }

  public ChainInventoryMessage(List<BlockId> blockIds) {
    super(blockIds, Type.SYNC);
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.BLOCK_CHAIN_INVENTORY;
  }
}
