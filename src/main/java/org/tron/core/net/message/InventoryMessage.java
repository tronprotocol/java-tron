package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Inventory;
import org.tron.protos.Protocol.Inventory.InventoryType;


public class InventoryMessage extends TronMessage {

  protected Inventory inv;

  public InventoryMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.INVENTORY.asByte();
  }

  public InventoryMessage() {
    this.type = MessageTypes.INVENTORY.asByte();
  }

  public InventoryMessage(Inventory inv) {
    this.inv = inv;
    unpacked = true;
    this.type = MessageTypes.INVENTORY.asByte();
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
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

  public Inventory getInventory() {
    unPack();
    return inv;
  }

  public MessageTypes getInvMessageType() {
    return getInventoryType().equals(InventoryType.BLOCK) ? MessageTypes.BLOCK : MessageTypes.TRX;

  }

  public InventoryType getInventoryType() {
    unPack();
    return inv.getType();
  }

  @Override
  public String toString() {
    Deque<Sha256Hash> hashes = new LinkedList<>(getHashList());
    StringBuilder builder = new StringBuilder();
    builder.append(getType()).append(":").append(getInvMessageType())
            .append(", size=").append(hashes.size())
            .append(", First hash:").append(hashes.peekFirst());
    if (hashes.size() > 1){
      builder.append(", End hash: ").append(hashes.peekLast());
    }
    return builder.toString();
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
    return getInventory().getIdsList().stream()
        .map(hash -> Sha256Hash.wrap(hash.toByteArray()))
        .collect(Collectors.toList());
  }

  private void pack() {
    this.data = this.inv.toByteArray();
  }
}
