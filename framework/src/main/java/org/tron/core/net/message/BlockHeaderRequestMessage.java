package org.tron.core.net.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;

public class BlockHeaderRequestMessage extends TronMessage {

  @Setter
  @Getter
  private Protocol.BlockHeaderRequestMessage blockHeaderRequestMessage;

  public BlockHeaderRequestMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_REQUEST_MESSAGE.asByte(), packed);
    this.blockHeaderRequestMessage = Protocol.BlockHeaderRequestMessage.parseFrom(packed);
  }

  public BlockHeaderRequestMessage(String chainId, long localLatestHeight, long blockHeaderLength) {
    blockHeaderRequestMessage = Protocol.BlockHeaderRequestMessage.newBuilder()
        .setChainId(ByteString.copyFrom(ByteArray.fromHexString(chainId)))
        .setBlockHeight(localLatestHeight)
        .setLength(blockHeaderLength)
        .build();
    super.data = blockHeaderRequestMessage.toByteArray();
    super.type = MessageTypes.HEADER_REQUEST_MESSAGE.asByte();
  }

  public long getBlockHeight() {
    return blockHeaderRequestMessage.getBlockHeight();
  }

  public long getLength() {
    return blockHeaderRequestMessage.getLength();
  }

  public byte[] getChainId() {
    return blockHeaderRequestMessage.getChainId().toByteArray();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
