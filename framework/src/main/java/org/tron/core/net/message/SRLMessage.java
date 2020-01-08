package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import org.tron.protos.Protocol;

public class SRLMessage extends TronMessage {

  @Getter
  @Setter
  private Protocol.SRL srl;
  @Getter
  @Setter
  private Protocol.PBFTCommitResult dataSign;

  public SRLMessage(byte[] packed) throws InvalidProtocolBufferException {
    super(MessageTypes.SR_LIST.asByte(), packed);
    dataSign = Protocol.PBFTCommitResult.parseFrom(packed);
    Protocol.PBFTMessage.Raw rawData = Protocol.PBFTMessage.Raw.parseFrom(dataSign.getData());
    srl = Protocol.SRL.parseFrom(rawData.getData());
  }

  public SRLMessage(Protocol.PBFTCommitResult dataSign) throws InvalidProtocolBufferException {
    this.dataSign = dataSign;
    super.type = MessageTypes.SR_LIST.asByte();
    super.data = this.dataSign.toByteArray();
    Protocol.PBFTMessage.Raw rawData = Protocol.PBFTMessage.Raw.parseFrom(this.dataSign.getData());
    srl = Protocol.SRL.parseFrom(rawData.getData());
  }
  public long getEpoch() {
    return srl.getEpoch();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
