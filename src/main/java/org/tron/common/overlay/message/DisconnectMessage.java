package org.tron.common.overlay.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message.P2pMessageCode;

public class DisconnectMessage extends P2pMessage {

  private Message.DisconnectMessage disconnectMessage;

  public DisconnectMessage(byte[] encoded) {
    super(encoded);
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
  }

  /**
   * The reason of disconnect.
   */
  public DisconnectMessage(Message.ReasonCode reason) {
    this.disconnectMessage = Message.DisconnectMessage
        .newBuilder()
        .setReason(reason)
        .build();
    unpacked = true;
    this.type = MessageTypes.P2P_DISCONNECT.asByte();
  }

  private void unPack() {
    try {
      this.disconnectMessage = Message.DisconnectMessage.parseFrom(data);
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
  public P2pMessageCode getCommand() {
    return P2pMessageCode.DISCONNECT;
  }

  /**
   * Get reason of disconnect.
   */
  public Message.ReasonCode getReason() {
    if (!this.unpacked) {
      this.unPack();
    }
    return this.disconnectMessage.getReason();
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
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}