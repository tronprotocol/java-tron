package org.tron.core.net.message;

import io.scalecube.cluster.Member;
import org.tron.protos.Protocal;

public class SyncBlockChainMessage extends InvertoryMessage {

  private Member peer;


  public SyncBlockChainMessage(byte[] packed, Member peer) {
    super(packed);
    this.peer = peer;
  }

  public SyncBlockChainMessage(Protocal.Inventory inv, Member peer) {
    super(inv);
    this.peer = peer;
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
