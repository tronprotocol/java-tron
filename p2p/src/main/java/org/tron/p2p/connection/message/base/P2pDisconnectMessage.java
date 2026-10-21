package org.tron.p2p.connection.message.base;

import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Connect.DisconnectReason;
import org.tron.p2p.utils.ProtoUtil;


public class P2pDisconnectMessage extends Message {

  private Connect.P2pDisconnectMessage p2pDisconnectMessage;

  public P2pDisconnectMessage(byte[] data) throws Exception {
    super(MessageType.DISCONNECT, data);
    this.p2pDisconnectMessage = ProtoUtil.parseFrom(Connect.P2pDisconnectMessage.parser(), data);
  }

  public P2pDisconnectMessage(DisconnectReason disconnectReason) {
    super(MessageType.DISCONNECT, null);
    this.p2pDisconnectMessage = Connect.P2pDisconnectMessage.newBuilder()
        .setReason(disconnectReason).build();
    this.data = p2pDisconnectMessage.toByteArray();
  }

  private DisconnectReason getReason() {
    return p2pDisconnectMessage.getReason();
  }

  @Override
  public boolean valid() {
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("reason: ")
        .append(getReason()).toString();
  }
}
