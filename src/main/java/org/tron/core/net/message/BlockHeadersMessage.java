package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;

import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Items;


public class BlockHeadersMessage extends Message {

  private List<BlockHeader> blockHeaders = new ArrayList<>();

  public BlockHeadersMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.BLOCKHEADERS.asByte();
  }

  public BlockHeadersMessage(List<BlockHeader> blockHeaders) {
    this.blockHeaders = blockHeaders;
    this.type = MessageTypes.BLOCKHEADERS.asByte();
    unpacked = true;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

  @Override
  public byte[] getData() {
    if (data == null) {
      pack();
    }
    return data;
  }

  public List<BlockHeader> getBlockHeaders() {
    unPack();
    return blockHeaders;
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }
    try {
      Items items = Items.parseFrom(data);
      if (items.getType() == Items.ItemType.BLOCKHEADER) {
        blockHeaders = items.getBlockHeadersList();
      }
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
    unpacked = true;
  }

  private void pack() {
    Items.Builder itemsBuilder = Items.newBuilder();
    itemsBuilder.setType(Items.ItemType.BLOCKHEADER);
    itemsBuilder.addAllBlockHeaders(this.blockHeaders);
    this.data = itemsBuilder.build().toByteArray();
  }

}
