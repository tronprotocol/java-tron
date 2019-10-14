package org.tron.common.net.udp.message.discover;

import static org.tron.common.net.udp.message.UdpMessageTypeEnum.DISCOVER_PING;

import com.google.protobuf.ByteString;
import org.tron.common.net.udp.message.Message;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.Args;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

public class PingMessage extends Message {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) throws Exception {
    super(DISCOVER_PING, data);
    this.pingMessage = Discover.PingMessage.parseFrom(data);
  }

  public PingMessage(Node from, Node to) {
    super(DISCOVER_PING, null);
    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();
    Endpoint toEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(to.getId()))
        .setPort(to.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(to.getHost())))
        .build();
    this.pingMessage = Discover.PingMessage.newBuilder()
        .setVersion(Args.getInstance().getNodeP2pVersion())
        .setFrom(fromEndpoint)
        .setTo(toEndpoint)
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.pingMessage.toByteArray();
  }

  public int getVersion() {
    return this.pingMessage.getVersion();
  }

  public Node getTo() {
    Endpoint to = this.pingMessage.getTo();
    Node node = new Node(to.getNodeId().toByteArray(),
        ByteArray.toStr(to.getAddress().toByteArray()), to.getPort());
    return node;
  }

  @Override
  public long getTimestamp() {
    return this.pingMessage.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return Message.getNode(pingMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[pingMessage: " + pingMessage;
  }

}
