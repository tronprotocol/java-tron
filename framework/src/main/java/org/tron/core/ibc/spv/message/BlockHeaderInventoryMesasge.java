package org.tron.core.ibc.spv.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.SignedBlockHeader;

public class BlockHeaderInventoryMesasge extends TronMessage {

  private Protocol.BlockHeaderInventory blockHeaderInventory;

  public BlockHeaderInventoryMesasge(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_INVENTORY.asByte(), packed);
    this.blockHeaderInventory = Protocol.BlockHeaderInventory.parseFrom(packed);
  }

  public BlockHeaderInventoryMesasge(String chainId, long currentBlockHeight,
      List<SignedBlockHeader> signedBlockHeaderList) {
    this.blockHeaderInventory = Protocol.BlockHeaderInventory.newBuilder()
        .setChainId(ByteString.copyFrom(ByteArray.fromHexString(chainId)))
        .setCurrentBlockHeight(currentBlockHeight)
        .addAllSignedBlockHeader(signedBlockHeaderList)
        .build();
    super.type = MessageTypes.HEADER_INVENTORY.asByte();
    super.data = blockHeaderInventory.toByteArray();
  }

  public List<Protocol.SignedBlockHeader> getBlockHeaders() {
    return blockHeaderInventory.getSignedBlockHeaderList();
  }

  public long getCurrentBlockHeight() {
    return blockHeaderInventory.getCurrentBlockHeight();
  }

  public ByteString getChainId() {
    return blockHeaderInventory.getChainId();
  }


  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
