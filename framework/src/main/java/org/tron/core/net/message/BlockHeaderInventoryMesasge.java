package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Protocol;

import java.util.List;

public class BlockHeaderInventoryMesasge extends TronMessage {

  private Protocol.BlockHeaderInventory blockHeaderInventory;

  public BlockHeaderInventoryMesasge(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_INVENTORY.asByte(), packed);
    this.blockHeaderInventory = Protocol.BlockHeaderInventory.parseFrom(packed);
  }

  public List<Protocol.BlockHeader> getBlockHeaders() {
    return blockHeaderInventory.getBlockHeaderList();
  }

  public long getCurrentBlockHeight() {
    return blockHeaderInventory.getCurrentBlockHeight();
  }

  public byte[] getChainId() {
    return blockHeaderInventory.getChainId().toByteArray();
  }


  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
