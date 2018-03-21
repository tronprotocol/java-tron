package org.tron.common.overlay.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.net.message.Message;
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
      this.disconnectMessage = Message.DisconnectMessage.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    unpacked = true;
  }

  private void pack() {
    this.rawData = this.disconnectMessage.toByteArray();
  }

  @Override
  public byte[] getRawData() {
    if (this.rawData == null) {
      this.pack();
    }
    return this.rawData;
  }

  @Override
  public byte[] getNodeId() {
    return new byte[0];
  }

  @Override
  public P2pMessageCodes getCommand() {
    return P2pMessageCodes.fromByte(this.type);
  }

  /**
   * Get reason of disconnect.
   */
  public ReasonCode getReason() {
    if (!this.unpacked) {
      this.unPack();
    }

    //TODO: fix this
    return ReasonCode.USER_REASON;
    //return this.disconnectMessage.getReason();
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