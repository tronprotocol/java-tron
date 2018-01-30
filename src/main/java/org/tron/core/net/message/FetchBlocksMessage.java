package org.tron.core.net.message;

import io.scalecube.cluster.Member;
import org.tron.protos.Protocal.Inventory;

public class FetchBlocksMessage extends org.tron.core.net.message.InvertoryMessage {

  private Member peer;

  public FetchBlocksMessage(byte[] packed, Member peer) {
    super(packed);
    this.peer = peer;
  }

  public FetchBlocksMessage(Inventory inv, Member peer) {
    super(inv);
    this.peer = peer;
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
