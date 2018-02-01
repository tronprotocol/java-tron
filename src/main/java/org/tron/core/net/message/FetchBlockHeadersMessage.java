package org.tron.core.net.message;

import org.tron.protos.Protocal;

public class FetchBlockHeadersMessage extends InventoryMessage {

  public FetchBlockHeadersMessage(byte[] packed) {
    super(packed);
  }

  public FetchBlockHeadersMessage(Protocal.Inventory inv) {
    super(inv);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public org.tron.core.net.message.MessageTypes getType() {
    return MessageTypes.FETCH_BLOCK_HEADERS;
  }

}