package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;


public class GetInventoryItemsMessage extends TronMessage {

  private org.tron.protos.TronInventoryItems.InventoryItems items;

  public GetInventoryItemsMessage() {
    super();
    this.type = MessageTypes.INVENTORY.asByte();
  }

  public GetInventoryItemsMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.INVENTORY.asByte();
  }

  public GetInventoryItemsMessage(org.tron.protos.TronInventoryItems.InventoryItems items) {
    this.items = items;
    unpacked = true;
    this.type = MessageTypes.INVENTORY.asByte();
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
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
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
