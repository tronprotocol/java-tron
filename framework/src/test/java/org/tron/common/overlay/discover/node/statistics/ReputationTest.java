package org.tron.common.overlay.discover.node.statistics;

import org.junit.Assert;
import org.junit.Test;

public class ReputationTest {

  NodeStatistics nodeStatistics = new NodeStatistics();
  Reputation reputation = new Reputation(nodeStatistics);

  @Test
  public void testGetScore() {
    Assert.assertEquals(0, reputation.getScore());

    nodeStatistics.messageStatistics.discoverInPong.add(3);
    Assert.assertEquals(100, reputation.getScore());

    nodeStatistics.messageStatistics.discoverOutPing.add(3);
    Assert.assertEquals(200, reputation.getScore());

    nodeStatistics.messageStatistics.discoverOutPing.add(1);
    Assert.assertEquals(150, reputation.getScore());

    nodeStatistics.tcpFlow.add(10240 * 5);
    Assert.assertEquals(155, reputation.getScore());

    nodeStatistics.discoverMessageLatency.add(100);
    Assert.assertEquals(165, reputation.getScore());

    nodeStatistics.notifyDisconnect();
    Assert.assertEquals(155, reputation.getScore());
  }
}
