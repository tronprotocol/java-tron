package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetInventoryItemsMessage extends Message {

  private org.tron.protos.TronInventoryItems.InventoryItems items;

  public GetInventoryItemsMessage() {
    super();
  }

  public GetInventoryItemsMessage(byte[] packed) {
    super(packed);
  }

  public GetInventoryItemsMessage(org.tron.protos.TronInventoryItems.InventoryItems items) {
    this.items = items;
    unpacked = true;
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.INVENTORY;
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }

    try {
      this.items = org.tron.protos.TronInventoryItems.InventoryItems.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }

  private void pack() {
    this.data = this.items.toByteArray();
  }
}
