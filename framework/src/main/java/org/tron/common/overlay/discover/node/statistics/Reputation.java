package org.tron.common.overlay.discover.node.statistics;

import static java.lang.Math.min;

public class Reputation {

  private NodeStatistics nodeStatistics;

  public Reputation(NodeStatistics nodeStatistics) {
    this.nodeStatistics = nodeStatistics;

  }

  public int getScore() {
    return getNodeActiveScore() + getPacketLossRateScore() + getNetLatencyScore()
        + getHandshakeScore() + getTcpFlowScore() + getDisconnectionScore();
  }

  private int getNodeActiveScore() {
    long inPongTotalCount = nodeStatistics.messageStatistics.discoverInPong.getTotalCount();
    return inPongTotalCount == 0 ? 0 : 100;
  }

  private int getPacketLossRateScore() {
    MessageStatistics s = nodeStatistics.messageStatistics;
    long in = s.discoverInPong.getTotalCount() + s.discoverInNeighbours.getTotalCount();
    long out = s.discoverOutPing.getTotalCount() + s.discoverOutFindNode.getTotalCount();
    return out == 0 ? 0 : 100 - min((int) ((1 - (double) in / out) * 200), 100);
  }

  private int getNetLatencyScore() {
    return (int) (nodeStatistics.discoverMessageLatency.getAvg() == 0 ? 0
        : min(1000 / nodeStatistics.discoverMessageLatency.getAvg(), 20));
  }

  private int getHandshakeScore() {
    return nodeStatistics.p2pHandShake.getTotalCount() > 0 ? 20 : 0;
  }

  private int getTcpFlowScore() {
    return (int) min(nodeStatistics.tcpFlow.getTotalCount() / 10240, 20);
  }

  private int getDisconnectionScore() {
    return -10 * nodeStatistics.getDisconnectTimes();
  }

}
