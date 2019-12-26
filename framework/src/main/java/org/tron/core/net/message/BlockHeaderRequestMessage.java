package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import org.tron.protos.Protocol;

public class BlockHeaderRequestMessage extends TronMessage {

  @Setter
  @Getter
  private Protocol.BlockHeaderRequestMessage blockHeaderRequestMessage;

  public BlockHeaderRequestMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.HEADER_REQUEST_MESSAGE.asByte(), packed);
    this.blockHeaderRequestMessage = Protocol.BlockHeaderRequestMessage.parseFrom(packed);
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
