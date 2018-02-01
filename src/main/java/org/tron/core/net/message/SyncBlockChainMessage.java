package org.tron.core.net.message;

import io.scalecube.cluster.Member;
import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal;

public class SyncBlockChainMessage extends InventoryOfPeerMessage {

  private Member peer;

  public SyncBlockChainMessage(byte[] packed, Member peer) {
    super(packed, peer);
  }

  public SyncBlockChainMessage(Protocal.Inventory inv, Member peer) {
    super(inv, peer);
  }

  public SyncBlockChainMessage(List<Sha256Hash> hashList, Member peer) {
    super(hashList, peer);
  }


  public Member getPeer() {
    return this.peer;
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
