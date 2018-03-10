package org.tron.core.net.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Inventory;
import org.tron.protos.Protocol.Inventory.InventoryType;


public class InventoryMessage extends Message {

  protected Inventory inv;

  public InventoryMessage(byte[] packed) {
    super(packed);
  }

  public InventoryMessage() {
  }

  public InventoryMessage(Inventory inv) {
    this.inv = inv;
    unpacked = true;
  }

  public InventoryMessage(List<Sha256Hash> hashList, InventoryType type) {
    Inventory.Builder invBuilder = Inventory.newBuilder();

    for (Sha256Hash hash :
        hashList) {
      invBuilder.addIds(hash.getByteString());
    }
    invBuilder.setType(type);
    inv = invBuilder.build();
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
  public MessageTypes getType() {
    return MessageTypes.INVENTORY;
  }

  public Inventory getInventory() {
    unPack();
    return inv;
  }

  public MessageTypes getInvMessageType() {
    return getInventory().equals(InventoryType.BLOCK) ? MessageTypes.BLOCK : MessageTypes.TRX;

  }

  public InventoryType getInventoryType() {
    unPack();
    return inv.getType();
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
