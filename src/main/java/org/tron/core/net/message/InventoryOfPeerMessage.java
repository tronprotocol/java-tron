package org.tron.core.net.message;

import io.scalecube.cluster.Member;
import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal;


public abstract class InventoryOfPeerMessage extends InventoryMessage {

  protected Member peer;

  public InventoryOfPeerMessage(byte[] packed, Member peer) {
    super(packed);
    this.peer = peer;
  }

  public InventoryOfPeerMessage(Protocal.Inventory inv, Member peer) {
    super(inv);
    this.peer = peer;
  }

  public InventoryOfPeerMessage(List<Sha256Hash> hashList, Member peer) {
    super(hashList);
    this.peer = peer;
  }

  public Member getPeer() {
    return peer;
  }
}
