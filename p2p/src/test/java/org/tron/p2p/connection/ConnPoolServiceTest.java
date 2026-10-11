package org.tron.p2p.connection;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.utils.TestPort;

public class ConnPoolServiceTest {

  private static String localIp = "127.0.0.1";
  // A fixed port collides with SocketTest and with other forks of this task.
  // PeerServer.start only logs on bind failure, so a collision used to let this
  // class pass while exercising nothing.
  private static int port = TestPort.choose();

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setDiscoverEnable(false);
    Parameter.p2pConfig.setPort(port);

    NodeManager.init();
    ChannelManager.init();
  }

  private void clearChannels() {
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @Test
  public void getNodes_chooseHomeNode() {
    InetSocketAddress localAddress = new InetSocketAddress(Parameter.p2pConfig.getIp(),
        Parameter.p2pConfig.getPort());
    Set<InetSocketAddress> inetInUse = new HashSet<>();
    inetInUse.add(localAddress);

    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(NodeManager.getHomeNode());

    ConnPoolService connPoolService = new ConnPoolService();
    List<Node> nodes = connPoolService.getNodes(new HashSet<>(), inetInUse, connectableNodes,
        1);
    Assert.assertEquals(0, nodes.size());

    nodes = connPoolService.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        1);
    Assert.assertEquals(1, nodes.size());
  }

  @Test
  public void getNodes_orderByUpdateTimeDesc() throws Exception {
    clearChannels();
    Node node1 = new Node(new InetSocketAddress(localIp, 90));
    Field field = node1.getClass().getDeclaredField("updateTime");
    field.setAccessible(true);
    field.set(node1, System.currentTimeMillis());

    Node node2 = new Node(new InetSocketAddress(localIp, 100));
    field = node2.getClass().getDeclaredField("updateTime");
    field.setAccessible(true);
    field.set(node2, System.currentTimeMillis() + 10);

    Assert.assertTrue(node1.getUpdateTime() < node2.getUpdateTime());

    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(node1);
    connectableNodes.add(node2);

    ConnPoolService connPoolService = new ConnPoolService();
    List<Node> nodes = connPoolService.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        2);
    Assert.assertEquals(2, nodes.size());
    // getNodes() sorts candidates by updateTime descending, but then calls
    // Collections.shuffle() before truncating to `limit`, so the order of the
    // returned list is deliberately randomised. Assert membership, not order —
    // asserting order here is a coin flip with two nodes. The effect of the
    // descending sort is covered by getNodes_prefersNewestAboveCandidateSize().
    Set<Long> returnedTimes = new HashSet<>();
    for (Node node : nodes) {
      returnedTimes.add(node.getUpdateTime());
    }
    Assert.assertTrue(returnedTimes.contains(node1.getUpdateTime()));
    Assert.assertTrue(returnedTimes.contains(node2.getUpdateTime()));

    int limit = 1;
    List<Node> nodes2 = connPoolService.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        limit);
    Assert.assertEquals(limit, nodes2.size());
  }

  /**
   * getNodes() keeps only the newest max(limit * 10, minCandidateSize) candidates
   * before shuffling, with minCandidateSize = 50. The descending sort is therefore
   * only observable once the candidate list exceeds that bound, which is what this
   * test exercises: with 60 candidates and limit 1, the 10 oldest must never be
   * returned no matter how the shuffle falls.
   */
  @Test
  public void getNodes_prefersNewestAboveCandidateSize() throws Exception {
    clearChannels();
    final int total = 60;
    final int candidateSize = 50;
    long base = System.currentTimeMillis();

    List<Node> connectableNodes = new ArrayList<>();
    for (int i = 0; i < total; i++) {
      Node node = new Node(new InetSocketAddress(localIp, 20000 + i));
      Field field = node.getClass().getDeclaredField("updateTime");
      field.setAccessible(true);
      // Node i gets updateTime base + i, so nodes 0..9 are the 10 oldest.
      field.set(node, base + i);
      connectableNodes.add(node);
    }

    long oldestRetainedTime = base + (total - candidateSize);
    ConnPoolService connPoolService = new ConnPoolService();
    // Repeat: a single draw could miss a mis-sorted entry by luck.
    for (int round = 0; round < 20; round++) {
      List<Node> nodes = connPoolService.getNodes(new HashSet<>(), new HashSet<>(),
          connectableNodes, 1);
      Assert.assertEquals(1, nodes.size());
      Assert.assertTrue("returned a node older than the newest " + candidateSize + " candidates",
          nodes.get(0).getUpdateTime() >= oldestRetainedTime);
    }
  }

  @Test
  public void getNodes_banNode() throws InterruptedException {
    clearChannels();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(localIp, 90);
    long banTime = 500L;
    ChannelManager.banNode(inetSocketAddress.getAddress(), banTime);
    Node node = new Node(inetSocketAddress);
    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(node);

    ConnPoolService connPoolService = new ConnPoolService();
    List<Node> nodes = connPoolService.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        1);
    Assert.assertEquals(0, nodes.size());
    Thread.sleep(2 * banTime);

    nodes = connPoolService.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes, 1);
    Assert.assertEquals(1, nodes.size());
  }

  @Test
  public void getNodes_nodeInUse() {
    clearChannels();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(localIp, 90);
    Node node = new Node(inetSocketAddress);
    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(node);

    Set<String> nodesInUse = new HashSet<>();
    nodesInUse.add(node.getHexId());
    ConnPoolService connPoolService = new ConnPoolService();
    List<Node> nodes = connPoolService.getNodes(nodesInUse, new HashSet<>(), connectableNodes, 1);
    Assert.assertEquals(0, nodes.size());
  }

  @AfterClass
  public static void destroy() {
    NodeManager.close();
    ChannelManager.close();
    // ChannelManager.close() latches isShutdown, and init() does not clear it. In
    // libp2p's own module that was harmless (one class per run, and its CI never ran
    // tests); here framework reuses a JVM across up to 100 classes, so leaving it set
    // makes every later PeerClient.connect() return null and ConnPoolService skip
    // reconnection. Reset it so the next class starts from a clean state.
    ChannelManager.isShutdown = false;
  }
}
