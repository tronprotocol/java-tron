package org.tron.core.net.tmsg;

import java.util.List;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Inventory;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class FetchInvDataMessage extends InventoryMessage {


  public FetchInvDataMessage(byte[] packed) {
    super(packed);
  }

  public FetchInvDataMessage(Inventory inv) {
    super(inv);
  }

  public FetchInvDataMessage(List<Sha256Hash> hashList, InventoryType type) {
    Inventory.Builder invBuilder = Inventory.newBuilder();

    for (Sha256Hash hash :
        hashList) {
      invBuilder.addIds(hash.getByteString());
    }
    invBuilder.setType(type);
    inv = invBuilder.build();
    unpacked = true;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  public MessageTypes getInvType() {
    return inv.getType().equals(InventoryType.BLOCK) ? MessageTypes.BLOCK : MessageTypes.TRX;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.FETCH_INV_DATA;
  }

}
