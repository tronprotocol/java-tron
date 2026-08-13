package org.tron.p2p.connection.business.detect;

import lombok.Data;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.discover.Node;

import java.net.InetSocketAddress;

@Data
public class NodeStat {
  private int totalCount;
  private long lastDetectTime;
  private long lastSuccessDetectTime;
  private StatusMessage statusMessage;
  private Node node;
  private InetSocketAddress socketAddress;

  public NodeStat(Node node) {
    this.node = node;
    this.socketAddress = node.getPreferInetSocketAddress();
  }

  public boolean finishDetect() {
    return this.lastDetectTime == this.lastSuccessDetectTime;
  }
}
