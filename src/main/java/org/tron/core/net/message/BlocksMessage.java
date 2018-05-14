package org.tron.core.net.message;

import java.util.List;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Items;

public class BlocksMessage extends TronMessage {

  private List<Block> blocks;

  public BlocksMessage(byte[] data) throws Exception {
    this.type = MessageTypes.BLOCKS.asByte();
    this.data = data;
    Items items = Items.parseFrom(data);
    if (items.getType() == Items.ItemType.BLOCK) {
      blocks = items.getBlocksList();
    }
  }

  public List<Block> getBlocks() {
    return blocks;
  }

  @Override
  public String toString() {
    return "BlocksMessage{" +
        "blocks=" + blocks +
        "}," + super.toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
