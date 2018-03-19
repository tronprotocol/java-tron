package org.tron.core.net.tmsg;

import org.tron.protos.Protocol;

public class FetchBlockHeadersMessage extends InventoryMessage {

  public FetchBlockHeadersMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.FETCH_BLOCK_HEADERS.asByte();
  }

  public FetchBlockHeadersMessage(Protocol.Inventory inv) {
    super(inv);
    this.type = MessageTypes.FETCH_BLOCK_HEADERS.asByte();
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}