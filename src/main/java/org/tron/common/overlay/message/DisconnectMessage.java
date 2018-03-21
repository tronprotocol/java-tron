package org.tron.common.overlay.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message;

public class DisconnectMessage extends P2pMessage {

  private Message.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte[] rawData) {
    super(rawData);
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
  }

  public DisconnectMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  /**
   * The reason of disconnect.
   */
  public DisconnectMessage(ReasonCode reasonCode) {
    this.disconnectMessage = Message.DisconnectMessage
        .newBuilder()
        .setReason(Message.ReasonCode.forNumber(reasonCode.asByte()))
        .build();
    unpacked = true;
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
  }

  private void unPack() {
    try {
      this.disconnectMessage = Message.DisconnectMessage.parseFrom(this.data);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.data = this.disconnectMessage.toByteArray();
  }

  @Override
  public byte[] getData() {
    if (this.data == null) {
      this.pack();
    }
    return this.data;
  }

  @Override
  public P2pMessageCodes getCommand() {
    return P2pMessageCodes.fromByte(this.type);
  }

  /**
   * Get reason of disconnect.
   */
  public byte getReason() {
    if (!this.unpacked) {
      this.unPack();
    }

    return ReasonCode.fromInt(this.disconnectMessage.getReason().getNumber()).asByte();
  }

  /**
   * Print reason of disconnect.
   */
  public String toString() {
    if (!this.unpacked) {
      this.unPack();
    }
    return "[" + this.getCommand().name() + " reason=" + this.getReason() + "]";
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}