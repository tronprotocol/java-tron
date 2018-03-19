package org.tron.common.overlay.discover.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.tmsg.MessageTypes;
import org.tron.protos.Message.P2pMessageCode;

public class PingMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  @Override
  public byte[] getData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public P2pMessageCode getCommand() {
    return P2pMessageCode.PING;
  }

  @Override
  public String toString() {
    return "[" + getCommand().name() + "]";
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.P2P_PING;
  }
}