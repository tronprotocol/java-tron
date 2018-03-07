package org.tron.core.net.message;

import java.util.List;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.Inventory;

public class SyncBlockChainMessage extends BlockInventoryMessage {

  public SyncBlockChainMessage(byte[] packed) {
    super(packed);
  }

  public SyncBlockChainMessage(Inventory inv) {
    super(inv);
  }

  public SyncBlockChainMessage(List<BlockId> blockIds) {
    super(blockIds);
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
