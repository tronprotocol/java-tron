package org.tron.common.overlay.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;

public class PongMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  private Discover.PongMessage pongMessage;

  public PongMessage() {
    this.type = MessageTypes.P2P_PONG.asByte();
    this.data = FIXED_PAYLOAD;
  }

  public PongMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    pongMessage = Discover.PongMessage.parseFrom(data);
    data = pongMessage.toByteArray();
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