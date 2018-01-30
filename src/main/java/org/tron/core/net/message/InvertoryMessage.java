package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Protocal.Inventory;


public class InvertoryMessage extends Message {

  private Inventory inv;

  public InvertoryMessage(byte[] packed) {
    super(packed);
  }

  public InvertoryMessage(Inventory inv) {
    this.inv = inv;
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

  public Inventory getInventory() {
    unPack();
    return inv;
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }

    try {
      this.inv = Inventory.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }

  private void pack() {
    this.data = this.inv.toByteArray();
  }
}
