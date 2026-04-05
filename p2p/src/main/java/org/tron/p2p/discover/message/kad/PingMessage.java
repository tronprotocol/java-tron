package org.tron.p2p.discover.message.kad;

import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.protos.Discover.Endpoint;
import org.tron.p2p.utils.NetUtil;

public class PingMessage extends KadMessage {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) throws Exception {
    super(MessageType.KAD_PING, data);
    this.pingMessage = Discover.PingMessage.parseFrom(data);
  }

  public PingMessage(Node from, Node to) {
    super(MessageType.KAD_PING, null);
    Endpoint fromEndpoint = getEndpointFromNode(from);
    Endpoint toEndpoint = getEndpointFromNode(to);
    this.pingMessage = Discover.PingMessage.newBuilder()
      .setVersion(Parameter.p2pConfig.getNetworkId())
      .setFrom(fromEndpoint)
      .setTo(toEndpoint)
      .setTimestamp(System.currentTimeMillis())
      .build();
    this.data = this.pingMessage.toByteArray();
  }

  public int getNetworkId() {
    return this.pingMessage.getVersion();
  }

  public Node getTo() {
    return NetUtil.getNode(this.pingMessage.getTo());
  }

  @Override
  public long getTimestamp() {
    return this.pingMessage.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return NetUtil.getNode(pingMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[pingMessage: " + pingMessage;
  }

  @Override
  public boolean valid() {
    return NetUtil.validNode(getFrom());
  }
}
