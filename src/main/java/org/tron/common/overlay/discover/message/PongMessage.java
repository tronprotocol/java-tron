package org.tron.common.overlay.discover.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message.P2pMessageCode;

public class PongMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  public PongMessage() {
    this.type = MessageTypes.P2P_PONG.asByte();
  }

  @Override
  public byte[] getData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public P2pMessageCode getCommand() {
    return P2pMessageCode.PONG;
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