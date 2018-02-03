package org.tron.core.net.message;

import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal.Inventory;
import org.tron.protos.Protocal.Inventory.InventoryType;

public class TransactionInventoryMessage extends InventoryMessage  {

  public TransactionInventoryMessage(byte[] packed) {
    super(packed);
  }

  public TransactionInventoryMessage(Inventory inv) {
    super(inv);
  }

  public TransactionInventoryMessage(List<Sha256Hash> hashList) {
    Inventory.Builder invBuilder = Inventory.newBuilder();

    for (Sha256Hash hash :
        hashList) {
      invBuilder.addIds(hash.getByteString());
    }
    invBuilder.setType(InventoryType.TRX);
    inv = invBuilder.build();
    unpacked = true;
  }

  @Override
  public byte[] getData() {
    return super.getData();
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.TRX_INVENTORY;
  }

  @Override
  public Inventory getInventory() {
    return super.getInventory();
  }

  @Override
  public List<Sha256Hash> getHashList() {
    return super.getHashList();
  }
}
