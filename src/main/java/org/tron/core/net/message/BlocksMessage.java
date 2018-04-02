package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Items;

@Slf4j(topic = "core.net")
public class BlocksMessage extends TronMessage {

  private List<Block> blocks;

  public BlocksMessage() {
    super();
    this.type = MessageTypes.BLOCKS.asByte();
  }

  public BlocksMessage(byte[] packed) {
    super(packed);
    this.type = MessageTypes.BLOCKS.asByte();
  }

  public BlocksMessage(List<Block> blocks) {
    this.blocks = blocks;
    unpacked = true;
    this.type = MessageTypes.BLOCKS.asByte();
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
