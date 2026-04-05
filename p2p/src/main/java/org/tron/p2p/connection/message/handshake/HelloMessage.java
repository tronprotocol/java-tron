package org.tron.p2p.connection.message.handshake;

import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.utils.ByteArray;
import org.tron.p2p.utils.NetUtil;

public class HelloMessage extends Message {

  private Connect.HelloMessage helloMessage;

  public HelloMessage(byte[] data) throws Exception {
    super(MessageType.HANDSHAKE_HELLO, data);
    this.helloMessage = Connect.HelloMessage.parseFrom(data);
  }

  public HelloMessage(DisconnectCode code, long time) {
    super(MessageType.HANDSHAKE_HELLO, null);
    Discover.Endpoint endpoint = Parameter.getHomeNode();
    this.helloMessage = Connect.HelloMessage.newBuilder()
      .setFrom(endpoint)
      .setNetworkId(Parameter.p2pConfig.getNetworkId())
      .setCode(code.getValue())
      .setVersion(Parameter.version)
      .setTimestamp(time).build();
    this.data = helloMessage.toByteArray();
  }

  public int getNetworkId() {
    return this.helloMessage.getNetworkId();
  }

  public int getVersion() {
    return this.helloMessage.getVersion();
  }

  public int getCode() {
    return this.helloMessage.getCode();
  }

  public long getTimestamp() {
    return this.helloMessage.getTimestamp();
  }

  public Node getFrom() {
    return NetUtil.getNode(helloMessage.getFrom());
  }

  @Override
  public String toString() {
    return "[HelloMessage: " + format();
  }

  @Override
  public boolean valid() {
    return NetUtil.validNode(getFrom());
  }

  public String format() {
    String[] lines = helloMessage.toString().split("\n");
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      if (line.contains("nodeId")) {
        String nodeId = ByteArray.toHexString(helloMessage.getFrom().getNodeId().toByteArray());
        line = "  nodeId: \"" + nodeId + "\"";
      }
      sb.append(line).append("\n");
    }
    return sb.toString();
  }

}
