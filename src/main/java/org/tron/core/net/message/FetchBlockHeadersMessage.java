package org.tron.core.net.message;

import org.tron.protos.Protocol;

public class FetchBlockHeadersMessage extends InventoryMessage {

  public FetchBlockHeadersMessage(byte[] packed) {
    super(packed);
  }

  public FetchBlockHeadersMessage(Protocol.Inventory inv) {
    super(inv);
  }

  @Override
  public org.tron.core.net.message.MessageTypes getType() {
    return MessageTypes.FETCH_BLOCK_HEADERS;
  }

}