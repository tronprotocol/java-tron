package org.tron.core.net.message.base;

import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ReasonCode;

public class DisconnectMessage extends TronMessage {

  private Protocol.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.disconnectMessage = Protocol.DisconnectMessage.parseFrom(this.data);
  }

  public DisconnectMessage(byte[] data) throws Exception {
    super(MessageTypes.P2P_DISCONNECT.asByte(), data);
    this.disconnectMessage = Protocol.DisconnectMessage.parseFrom(data);
  }

  public DisconnectMessage(ReasonCode reasonCode) {
    this.disconnectMessage = Protocol.DisconnectMessage
        .newBuilder()
        .setReason(reasonCode)
        .build();
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
    this.data = this.disconnectMessage.toByteArray();
  }

  public ReasonCode getReason() {
    return this.disconnectMessage.getReason();
  }

  public ReasonCode getReasonCode() {
    return disconnectMessage.getReason();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("reason: ")
        .append(this.disconnectMessage.getReason()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}