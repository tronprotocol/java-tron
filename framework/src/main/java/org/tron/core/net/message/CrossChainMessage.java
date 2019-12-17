package org.tron.core.net.message;

import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.CrossMessage;

public class CrossChainMessage extends TronMessage {

  private CrossMessage crossMessage;

  public CrossChainMessage(byte[] data) throws Exception {
    super(data);
    this.type = MessageTypes.CROSS_MSG.asByte();
    this.crossMessage = CrossMessage.parseFrom(data);
  }

  public CrossChainMessage(CrossMessage crossMessage) {
    data = crossMessage.toByteArray();
    this.type = MessageTypes.CROSS_MSG.asByte();
    this.crossMessage = crossMessage;
  }

  public CrossMessage getCrossMessage() {
    return this.crossMessage;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public Sha256Hash getMessageId() {
    return Sha256Hash.of(crossMessage.getTransaction().getRawData().toByteArray());
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
