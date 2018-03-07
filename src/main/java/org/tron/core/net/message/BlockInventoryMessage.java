package org.tron.core.net.message;

import java.util.List;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocal.Inventory;
import org.tron.protos.Protocal.Inventory.InventoryType;

public class BlockInventoryMessage extends InventoryMessage {

  public BlockInventoryMessage(byte[] packed) {
    super(packed);
  }

  public BlockInventoryMessage(Inventory inv) {
    super(inv);
  }

  public BlockInventoryMessage(List<BlockId> blockIds) {

    Inventory.Builder invBuilder = Inventory.newBuilder();

    for (BlockId id :
        blockIds) {
      invBuilder.addIds(id.getByteString());
    }
    invBuilder.setType(InventoryType.BLOCK);
    inv = invBuilder.build();
    unpacked = true;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.BLOCK_INVENTORY;
  }

  //public List<BlockId> getBlockIds()

}
