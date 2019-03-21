package org.tron.common.overlay.message;

import com.google.protobuf.UnknownFieldSet;
import java.util.Collections;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ReasonCode;

public class DisconnectMessage extends P2pMessage {

  private Protocol.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.disconnectMessage = Protocol.DisconnectMessage.parseFrom(this.data);
    setUnknownFieldEmpty(disconnectMessage.getUnknownFields());
    data = disconnectMessage.toByteArray();
  }

  public DisconnectMessage(ReasonCode reasonCode) {
    this.disconnectMessage = Protocol.DisconnectMessage
        .newBuilder()
        .setReason(reasonCode)
        .build();
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
    this.data = this.disconnectMessage.toByteArray();
  }

  public int getReason() {
    return this.disconnectMessage.getReason().getNumber();
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