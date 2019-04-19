package org.tron.core.net.message;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Items;

public class BlocksMessage extends TronMessage {

  private List<Block> blocks;

  public BlocksMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.BLOCKS.asByte();
    Items items = Items.parseFrom(getCodedInputStream(data));
    if (items.getType() == Items.ItemType.BLOCK) {
      blocks = items.getBlocksList();
    }
    if (isFilter()) {
      for (Block block : blocks) {
        for (Protocol.Transaction transaction : block.getTransactionsList()) {
          TransactionCapsule.validContractProto(transaction.getRawData().getContract(0));
        }
      }
    }
    compareBytes(data, items.toByteArray());
  }

  public List<Block> getBlocks() {
    return blocks;
  }

  @Override
  public String toString() {
    return super.toString() + "size: " + (CollectionUtils.isNotEmpty(blocks) ? blocks
        .size() : 0);
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
