package org.tron.common.overlay.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;

public class PingMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  public PingMessage() {
    this.type = MessageTypes.P2P_PING.asByte();
  }

  public PingMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public byte[] getRawData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public byte[] getNodeId() {
    return new byte[0];
  }

  @Override
  public P2pMessageCodes getCommand() {
    return P2pMessageCodes.fromByte(this.type);
  }

  @Override
  public String toString() {
    return "[" + getCommand().name() + "]";
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}