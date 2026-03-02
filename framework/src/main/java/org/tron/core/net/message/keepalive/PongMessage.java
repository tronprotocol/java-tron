package org.tron.core.net.message.keepalive;

import org.bouncycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;

public class PongMessage extends TronMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  public PongMessage() {
    this.type = MessageTypes.P2P_PONG.asByte();
    this.data = FIXED_PAYLOAD;
  }

  public PongMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  public PongMessage(byte[] data) {
    super(MessageTypes.P2P_PONG.asByte(), data);
  }

  @Override
  public byte[] getData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public String toString() {
    return super.toString();
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
