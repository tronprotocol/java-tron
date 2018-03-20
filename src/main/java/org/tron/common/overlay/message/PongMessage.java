package org.tron.common.overlay.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;

public class PongMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  public PongMessage() {
    this.type = MessageTypes.P2P_PONG.asByte();
  }

  public PongMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  @Override
  public byte[] getRawData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public P2pMessageCodes getCommand() {
    return P2pMessageCodes.fromByte(this.type);
  }

  @Override
  public String toString() {
    return "[" + this.getCommand().name() + "]";
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}