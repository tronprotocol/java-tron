package org.tron.p2p.connection.message.detect;

import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.utils.NetUtil;

public class StatusMessage extends Message {
  private Connect.StatusMessage statusMessage;

  public StatusMessage(byte[] data) throws Exception {
    super(MessageType.STATUS, data);
    this.statusMessage = Connect.StatusMessage.parseFrom(data);
  }

  public StatusMessage() {
    super(MessageType.STATUS, null);
    Discover.Endpoint endpoint = Parameter.getHomeNode();
    this.statusMessage = Connect.StatusMessage.newBuilder()
      .setFrom(endpoint)
      .setMaxConnections(Parameter.p2pConfig.getMaxConnections())
      .setCurrentConnections(ChannelManager.getChannels().size())
      .setNetworkId(Parameter.p2pConfig.getNetworkId())
      .setTimestamp(System.currentTimeMillis()).build();
    this.data = statusMessage.toByteArray();
  }

  public int getNetworkId() {
    return this.statusMessage.getNetworkId();
  }

  public int getVersion() {
    return this.statusMessage.getVersion();
  }

  public int getRemainConnections() {
    return this.statusMessage.getMaxConnections() - this.statusMessage.getCurrentConnections();
  }

  public long getTimestamp() {
    return this.statusMessage.getTimestamp();
  }

  public Node getFrom() {
    return NetUtil.getNode(statusMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[StatusMessage: " + statusMessage;
  }

  @Override
  public boolean valid() {
    return NetUtil.validNode(getFrom());
  }
}
