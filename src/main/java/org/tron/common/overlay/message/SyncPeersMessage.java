package org.tron.common.overlay.message;

import org.spongycastle.util.encoders.Hex;
import org.tron.core.net.tmsg.MessageTypes;
import org.tron.protos.Message.P2pMessageCode;

public class SyncPeersMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C104");

  public SyncPeersMessage() {
    this.type = MessageTypes.P2P_SYNC_PEERS.asByte();
  }

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
    return MessageTypes.fromByte(this.type);
  }
}