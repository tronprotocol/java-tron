package org.tron.core.net.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;

import java.util.List;

public class BlockHeaderInventoryMesasge extends TronMessage {

  private Protocol.BlockHeaderInventory blockHeaderInventory;

  public BlockHeaderInventoryMesasge(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_INVENTORY.asByte(), packed);
    this.blockHeaderInventory = Protocol.BlockHeaderInventory.parseFrom(packed);
  }

  public BlockHeaderInventoryMesasge(String chainId, long currentBlockHeight, List<Protocol.BlockHeader> blockHeaders) {
    this.blockHeaderInventory = Protocol.BlockHeaderInventory.newBuilder()
        .setChainId(ByteString.copyFrom(ByteArray.fromHexString(chainId)))
        .setCurrentBlockHeight(currentBlockHeight)
        .addAllBlockHeader(blockHeaders)
        .build();
    super.type = MessageTypes.HEADER_INVENTORY.asByte();
    super.data = blockHeaderInventory.toByteArray();
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
