package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.Items;

public class BlocksMessage extends Message {

  private List<Block> blocks;

  public BlocksMessage() {
    super();
  }

  public BlocksMessage(byte[] packed) {
    super(packed);
  }

  public BlocksMessage(List<Block> blocks) {
    this.blocks = blocks;
    unpacked = true;
  }

  public List<Block> getBlocks() {
    unPack();
    return blocks;
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.BLOCKS;
  }

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }
    try {
      Items items = Items.parseFrom(data);
      if (items.getType() == Items.ItemType.BLOCK) {
        blocks = items.getBlocksList();
      }
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
    unpacked = true;
  }

  private void pack() {
    Items.Builder itemsBuilder = Items.newBuilder();
    itemsBuilder.setType(Items.ItemType.BLOCK);
    itemsBuilder.addAllBlocks(this.blocks);
    this.data = itemsBuilder.build().toByteArray();
  }
}
