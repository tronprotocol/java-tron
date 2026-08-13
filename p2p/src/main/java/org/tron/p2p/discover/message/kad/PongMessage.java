package org.tron.p2p.discover.message.kad;

import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.protos.Discover.Endpoint;
import org.tron.p2p.utils.NetUtil;

public class PongMessage extends KadMessage {

  private Discover.PongMessage pongMessage;

  public PongMessage(byte[] data) throws Exception {
    super(MessageType.KAD_PONG, data);
    this.pongMessage = Discover.PongMessage.parseFrom(data);
  }

  public PongMessage(Node from) {
    super(MessageType.KAD_PONG, null);
    Endpoint toEndpoint = getEndpointFromNode(from);
    this.pongMessage = Discover.PongMessage.newBuilder()
      .setFrom(toEndpoint)
      .setEcho(Parameter.p2pConfig.getNetworkId())
      .setTimestamp(System.currentTimeMillis())
      .build();
    this.data = this.pongMessage.toByteArray();
  }

  public int getNetworkId() {
    return this.pongMessage.getEcho();
  }

  @Override
  public long getTimestamp() {
    return this.pongMessage.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return NetUtil.getNode(pongMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[pongMessage: " + pongMessage;
  }

  @Override
  public boolean valid() {
    return NetUtil.validNode(getFrom());
  }
}
