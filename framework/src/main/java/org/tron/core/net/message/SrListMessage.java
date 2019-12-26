package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import org.tron.protos.Protocol;

public class SrListMessage extends TronMessage {

  @Getter
  @Setter
  private Protocol.SrList srList;
  @Getter
  @Setter
  private Protocol.DataSign dataSign;

  public SrListMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.SR_LIST.asByte(), packed);
    dataSign = Protocol.DataSign.parseFrom(packed);
    srList = Protocol.SrList.parseFrom(dataSign.getData());
  }

  public long getEpoch() {
    return srList.getEpoch();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
