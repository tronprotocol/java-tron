package org.tron.core.net.message;

import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.Inventory;

public class SyncBlockChainMessage extends InventoryMessage {

  public SyncBlockChainMessage(byte[] packed) {
    super(packed);
  }

  public SyncBlockChainMessage(Inventory inv) {
    super(inv);
  }

  public SyncBlockChainMessage(List<Sha256Hash> hashList) {
    super(hashList);
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
    return MessageTypes.SYNC_BLOCK_CHAIN;
  }

  @Override
  public Protocal.Inventory getInventory() {
    return super.getInventory();
  }
}
