package org.tron.core.net.message.keepalive;

import org.bouncycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;

public class PingMessage extends TronMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  public PingMessage() {
    this.type = MessageTypes.P2P_PING.asByte();
    this.data = FIXED_PAYLOAD;
  }

  public PingMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  public PingMessage(byte[] data) {
    super(MessageTypes.P2P_PING.asByte(), data);
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
    return PongMessage.class;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}
