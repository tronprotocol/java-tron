package org.tron.core.net.tmsg;

import java.util.List;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.BlockInventory.Type;

public class FetchBlockInvMessage extends BlockInventoryMessage  {

  public FetchBlockInvMessage(byte[] packed) {
    super(packed);
  }

  public FetchBlockInvMessage(List<BlockId> blockIds) {
    super(blockIds, Type.FETCH);
  }

  @Override
  public MessageTypes getType() {
    return super.getType();
  }
}
