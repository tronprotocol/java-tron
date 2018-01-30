package org.tron.core.net.message;

import org.tron.protos.Protocal;

public class ItemNotFound extends Message {

  private org.tron.protos.Protocal.Items notFound;

  /**
   * means can not find this block or trx.
   */
  public ItemNotFound() {
    Protocal.Items.Builder itemsBuilder = Protocal.Items.newBuilder();
    itemsBuilder.setType(Protocal.Items.ItemType.ERR);
    notFound = itemsBuilder.build();
  }

  @Override
  public byte[] getData() {
    return notFound.toByteArray();
  }

  @Override
  public String toString() {
    return "item not found";
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.ITEM_NOT_FOUND;
  }
}
