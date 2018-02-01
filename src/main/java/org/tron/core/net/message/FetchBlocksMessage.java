package org.tron.core.net.message;

import io.scalecube.cluster.Member;
import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal.Inventory;

public class FetchBlocksMessage extends InventoryOfPeerMessage {

  private Member peer;


  public FetchBlocksMessage(byte[] packed, Member peer) {
    super(packed, peer);
  }

  public FetchBlocksMessage(Inventory inv, Member peer) {
    super(inv, peer);
  }

  public FetchBlocksMessage(List<Sha256Hash> hashList, Member peer) {
    super(hashList, peer);
  }

  public Member getPeer() {
    return this.peer;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public org.tron.core.net.message.MessageTypes getType() {
    return org.tron.core.net.message.MessageTypes.FETCH_BLOCKS;
  }

}
