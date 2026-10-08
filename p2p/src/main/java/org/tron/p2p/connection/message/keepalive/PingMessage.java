package org.tron.p2p.connection.message.keepalive;

import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect;

public class PingMessage extends Message {

  private Connect.KeepAliveMessage keepAliveMessage;

  public PingMessage(byte[] data) throws Exception {
    super(MessageType.KEEP_ALIVE_PING, data);
    this.keepAliveMessage = Connect.KeepAliveMessage.parseFrom(data);
  }

  public PingMessage() {
    super(MessageType.KEEP_ALIVE_PING, null);
    this.keepAliveMessage = Connect.KeepAliveMessage.newBuilder()
      .setTimestamp(System.currentTimeMillis()).build();
    this.data = this.keepAliveMessage.toByteArray();
  }

  public long getTimeStamp() {
    return this.keepAliveMessage.getTimestamp();
  }

  @Override
  public boolean valid() {
    return getTimeStamp() > 0
      && getTimeStamp() <= System.currentTimeMillis() + Parameter.NETWORK_TIME_DIFF;
  }
}
