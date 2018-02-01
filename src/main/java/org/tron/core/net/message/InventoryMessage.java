package org.tron.core.net.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal.Inventory;


public class InventoryMessage extends Message {

  private Inventory inv;

  public InventoryMessage(byte[] packed) {
    super(packed);
  }

  public InventoryMessage(Inventory inv) {
    this.inv = inv;
    unpacked = true;
  }

  public InventoryMessage(List<Sha256Hash> hashList) {
    //Inventory.Builder invBuilder = Inventory.newBuilder();

//
//    Items.Builder itemsBuilder = Items.newBuilder();
//    itemsBuilder.setType(Items.ItemType.BLOCK);
//    itemsBuilder.addAllBlocks(this.blocks);
//    this.data = itemsBuilder.build().toByteArray();

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

  public List<Sha256Hash> getHashList() {
    Inventory inv = getInventory();
    List<Sha256Hash> ret = new ArrayList<>();
    for (ByteString hash :
        inv.getIdsList()) {
      ret.add(Sha256Hash.wrap(hash.toByteArray()));
    }
    return ret;
  }

  private void pack() {
    this.data = this.inv.toByteArray();
  }
}
