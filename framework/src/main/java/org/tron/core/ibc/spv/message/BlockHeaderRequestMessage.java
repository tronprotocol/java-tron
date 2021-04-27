package org.tron.core.ibc.spv.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol;

public class BlockHeaderRequestMessage extends TronMessage {

  @Setter
  @Getter
  private Protocol.BlockHeaderRequestMessage blockHeaderRequestMessage;

  public BlockHeaderRequestMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_REQUEST_MESSAGE.asByte(), packed);
    this.blockHeaderRequestMessage = Protocol.BlockHeaderRequestMessage.parseFrom(packed);
  }

  public BlockHeaderRequestMessage(String chainId,
                                   long localLatestHeight,
                                   long blockHeaderLength,
                                   long latestMaintenanceTime) {
    blockHeaderRequestMessage = Protocol.BlockHeaderRequestMessage.newBuilder()
        .setChainId(ByteString.copyFrom(ByteArray.fromHexString(chainId)))
        .setBlockHeight(localLatestHeight)
        .setLength(blockHeaderLength)
        .setLatestMaintenanceTime(latestMaintenanceTime)
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

  public ByteString getChainId() {
    return blockHeaderRequestMessage.getChainId();
  }

  public long getLatestMaintenanceTime() {
    return blockHeaderRequestMessage.getLatestMaintenanceTime();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
