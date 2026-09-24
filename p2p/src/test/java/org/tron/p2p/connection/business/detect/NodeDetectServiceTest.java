package org.tron.p2p.connection.business.detect;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;

/**
 * Node detection probes candidate peers with STATUS and keeps a per-address
 * NodeStat. The bad-node cache and the trim pass are what stop an unreachable
 * address from being retried forever.
 */
public class NodeDetectServiceTest {

  private P2pConfig saved;
  private NodeDetectService service;

  @SuppressWarnings("unchecked")
  private Map<InetSocketAddress, NodeStat> nodeStatMap() throws Exception {
    Field field = NodeDetectService.class.getDeclaredField("nodeStatMap");
    field.setAccessible(true);
    return (Map<InetSocketAddress, NodeStat>) field.get(service);
  }

  private static Node nodeAt(String ip, int port) {
    return new Node(NetUtil.getNodeId(), ip, null, port, port);
  }

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setPort(18888);
    config.setIp("127.0.0.1");
    config.setMaxConnections(30);
    Parameter.p2pConfig = config;
    ChannelManager.getChannels().clear();
    NodeDetectService.getBadNodesCache().invalidateAll();
    service = new NodeDetectService();
  }

  @After
  public void tearDown() {
    NodeDetectService.getBadNodesCache().invalidateAll();
    ChannelManager.getChannels().clear();
    Parameter.p2pConfig = saved;
  }

  @Test
  public void nodeStatStartsUnfinishedOnlyAfterADetectIsRecorded() {
    NodeStat stat = new NodeStat(nodeAt("127.0.0.1", 10001));
    // Both timestamps are zero to begin with, which counts as finished.
    Assert.assertTrue(stat.finishDetect());

    stat.setLastDetectTime(1000L);
    Assert.assertFalse(stat.finishDetect());

    stat.setLastSuccessDetectTime(1000L);
    Assert.assertTrue(stat.finishDetect());
  }

  @Test
  public void trimDropsTimedOutProbesAndBansTheAddress() throws Exception {
    Node node = nodeAt("127.0.0.1", 10001);
    NodeStat stat = new NodeStat(node);
    // A detect started long ago and never answered.
    stat.setLastDetectTime(System.currentTimeMillis() - 60_000);
    nodeStatMap().put(node.getPreferInetSocketAddress(), stat);

    service.trimNodeMap();

    Assert.assertTrue(nodeStatMap().isEmpty());
    Assert.assertNotNull("the address should be remembered as bad",
        NodeDetectService.getBadNodesCache()
            .getIfPresent(node.getPreferInetSocketAddress().getAddress()));
  }

  @Test
  public void trimKeepsAProbeThatIsStillInFlight() throws Exception {
    Node node = nodeAt("127.0.0.1", 10001);
    NodeStat stat = new NodeStat(node);
    stat.setLastDetectTime(System.currentTimeMillis());
    nodeStatMap().put(node.getPreferInetSocketAddress(), stat);

    service.trimNodeMap();

    Assert.assertEquals(1, nodeStatMap().size());
  }

  @Test
  public void trimKeepsACompletedProbe() throws Exception {
    Node node = nodeAt("127.0.0.1", 10001);
    NodeStat stat = new NodeStat(node);
    long when = System.currentTimeMillis() - 60_000;
    stat.setLastDetectTime(when);
    stat.setLastSuccessDetectTime(when);
    nodeStatMap().put(node.getPreferInetSocketAddress(), stat);

    service.trimNodeMap();

    Assert.assertEquals(1, nodeStatMap().size());
  }

  @Test
  public void connectableNodesAreEmptyUntilSomethingAnswers() throws Exception {
    Node node = nodeAt("127.0.0.1", 10001);
    nodeStatMap().put(node.getPreferInetSocketAddress(), new NodeStat(node));

    // A NodeStat without a StatusMessage has not answered yet.
    List<Node> connectable = service.getConnectableNodes();
    Assert.assertNotNull(connectable);
    Assert.assertTrue(connectable.isEmpty());
  }

  @Test
  public void closeIsSafeToCallTwice() {
    service.close();
    service.close();
  }
}
