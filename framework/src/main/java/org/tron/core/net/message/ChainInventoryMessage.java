package org.tron.core.net.message;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ChainInventory;

public class ChainInventoryMessage extends TronMessage {

  protected ChainInventory chainInventory;

  public ChainInventoryMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.BLOCK_CHAIN_INVENTORY.asByte();
    chainInventory = Protocol.ChainInventory.parseFrom(data);
  }

  public ChainInventoryMessage(List<BlockId> blockIds, Long remainNum) {
    ChainInventory.Builder invBuilder = ChainInventory.newBuilder();
    blockIds.forEach(blockId -> {
      ChainInventory.BlockId.Builder b = ChainInventory.BlockId.newBuilder();
      b.setHash(blockId.getByteString());
      b.setNumber(blockId.getNum());
      invBuilder.addIds(b);
    });

    invBuilder.setRemainNum(remainNum);
    chainInventory = invBuilder.build();
    this.type = MessageTypes.BLOCK_CHAIN_INVENTORY.asByte();
    this.data = chainInventory.toByteArray();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  private ChainInventory getChainInventory() {
    return chainInventory;
  }

  public List<BlockId> getBlockIds() {

    try {
      return getChainInventory().getIdsList().stream()
          .map(blockId -> new BlockId(blockId.getHash(), blockId.getNumber()))
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (Exception e) {
      logger.info("breakPoint");
    }
    return null;
  }

  public Long getRemainNum() {
    return getChainInventory().getRemainNum();
  }

  @Override
  public String toString() {
    Deque<BlockId> blockIdWeGet = new LinkedList<>(getBlockIds());
    StringBuilder sb = new StringBuilder(super.toString());
    int size = blockIdWeGet.size();
    sb.append("size: ").append(size);
    if (size >= 1) {
      sb.append(", first blockId: ").append(blockIdWeGet.peek().getString());
      if (size > 1) {
        sb.append(", end blockId: ").append(blockIdWeGet.peekLast().getString());
      }
    }
    return sb.toString();
  }
}
