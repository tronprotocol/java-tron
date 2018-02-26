package org.tron.core.net.message;

import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal.Inventory;
import org.tron.protos.Protocal.Inventory.InventoryType;

public class ChainInventoryMessage extends InventoryMessage {

  public ChainInventoryMessage(byte[] packed) {
    super(packed);
  }

  public ChainInventoryMessage(Inventory inv) {
    super(inv);
  }

  public ChainInventoryMessage(List<Sha256Hash> hashList) {
    super(hashList, InventoryType.BLOCK);
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.BLOCK_CHAIN_INVENTORY;
  }
}
