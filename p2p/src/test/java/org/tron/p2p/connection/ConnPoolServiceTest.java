package org.tron.p2p.connection;

import io.netty.channel.ChannelFuture;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.socket.PeerClient;
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
  public void getNodesReturnsAllCandidatesWhenLimitAllows() {
    clearChannels();
    Node node1 = new Node(new InetSocketAddress(localIp, 90));
    Node node2 = new Node(new InetSocketAddress(localIp, 100));

    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(node1);
    connectableNodes.add(node2);

    ConnPoolService connPoolService = new ConnPoolService();
    List<Node> nodes = connPoolService.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        2);
    Assert.assertEquals(2, nodes.size());
    Set<InetSocketAddress> selectedAddresses = new HashSet<>();
    nodes.forEach(node -> selectedAddresses.add(node.getPreferInetSocketAddress()));
    Assert.assertTrue(selectedAddresses.contains(node1.getPreferInetSocketAddress()));
    Assert.assertTrue(selectedAddresses.contains(node2.getPreferInetSocketAddress()));

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

  @Test
  public void poolScanUsesIsolatedActiveNodesWithoutDialCooldown() throws Exception {
    clearChannels();
    ConnectionPolicy.replaceBlockedIps(new HashSet<>());
    P2pConfig isolatedConfig = new P2pConfig();
    isolatedConfig.setMinConnections(0);
    isolatedConfig.setMinActiveConnections(0);
    InetSocketAddress address = new InetSocketAddress("192.0.2.10", 18888);
    isolatedConfig.getActiveNodes().add(address);
    Assert.assertFalse(Parameter.p2pConfig.getActiveNodes().contains(address));
    CountingPeerClient peerClient = new CountingPeerClient();
    ConnPoolService connPoolService = new ConnPoolService();
    connPoolService.p2pConfig = isolatedConfig;
    Field peerClientField = ConnPoolService.class.getDeclaredField("peerClient");
    peerClientField.setAccessible(true);
    peerClientField.set(connPoolService, peerClient);
    Method connect = ConnPoolService.class.getDeclaredMethod("connect", boolean.class);
    connect.setAccessible(true);
    try {
      connect.invoke(connPoolService, false);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount);

      isolatedConfig.getActiveNodes().remove(address);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount);

      InetSocketAddress blocked = new InetSocketAddress("192.0.2.12", 18888);
      isolatedConfig.getActiveNodes().add(blocked);
      Assert.assertFalse(Parameter.p2pConfig.getActiveNodes().contains(blocked));
      ConnectionPolicy.replaceBlockedIps(Collections.singleton(blocked.getAddress()));
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount);
    } finally {
      ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
    }
  }

  @Test
  public void poolScanTracksOnlyScheduledDials() throws Exception {
    clearChannels();
    ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
    P2pConfig isolatedConfig = new P2pConfig();
    isolatedConfig.setMinConnections(1);
    isolatedConfig.setMinActiveConnections(0);
    InetSocketAddress address = new InetSocketAddress("192.0.2.20", 18888);
    NodeDetectService nodeDetectService = Mockito.mock(NodeDetectService.class);
    Mockito.when(nodeDetectService.getConnectableNodes())
        .thenReturn(Collections.singletonList(new Node(address)));
    Field detectServiceField = ChannelManager.class.getDeclaredField("nodeDetectService");
    detectServiceField.setAccessible(true);
    Object originalDetectService = detectServiceField.get(null);

    CountingPeerClient peerClient = new CountingPeerClient();
    ConnPoolService connPoolService = new ConnPoolService();
    connPoolService.p2pConfig = isolatedConfig;
    Field peerClientField = ConnPoolService.class.getDeclaredField("peerClient");
    peerClientField.setAccessible(true);
    peerClientField.set(connPoolService, peerClient);
    Method connect = ConnPoolService.class.getDeclaredMethod("connect", boolean.class);
    connect.setAccessible(true);

    try {
      detectServiceField.set(null, nodeDetectService);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(1, peerClient.connectCount);
      Assert.assertEquals(0, connPoolService.getConnectingPeersCount().get());

      peerClient.future = Mockito.mock(ChannelFuture.class);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount);
      Assert.assertEquals(1, connPoolService.getConnectingPeersCount().get());

      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount);
      Assert.assertEquals(1, connPoolService.getConnectingPeersCount().get());
    } finally {
      detectServiceField.set(null, originalDetectService);
    }
  }

  private static class CountingPeerClient extends PeerClient {

    private int connectCount;
    private ChannelFuture future;

    @Override
    public ChannelFuture connectAsync(Node node, boolean discoveryMode) {
      connectCount++;
      return future;
    }
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
