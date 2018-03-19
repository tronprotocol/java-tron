package org.tron.common.overlay.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Message.P2pMessageCode;

public class SyncPeersMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C104");

  @Override
  public byte[] getData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public P2pMessageCode getCommand() {
    return P2pMessageCode.GET_PEERS;
  }

  @Override
  public String toString() {
    return "[" + this.getCommand().name() + "]";
  }

  @Override
  public MessageTypes getType() {
    return null;
  }
}