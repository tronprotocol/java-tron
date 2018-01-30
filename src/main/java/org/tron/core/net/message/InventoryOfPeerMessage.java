package org.tron.core.net.message;

import io.scalecube.cluster.Member;
import org.tron.protos.Protocal;


public abstract class InventoryOfPeerMessage extends InvertoryMessage {

  protected Member peer;

  public InventoryOfPeerMessage(byte[] packed, Member peer) {
    super(packed);
    this.peer = peer;
  }

  public InventoryOfPeerMessage(Protocal.Inventory inv, Member peer) {
    super(inv);
    this.peer = peer;
  }

  public Member getPeer() {
    return peer;
  }
}
