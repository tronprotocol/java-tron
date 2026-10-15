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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.discover.DiscoverService;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.discover.protocol.kad.KadService;
import org.tron.p2p.utils.TestPort;

public class ConnPoolServiceTest {

  private static String localIp = "127.0.0.1";
  // A fixed port collides with SocketTest and with other forks of this task.
  // PeerServer.start only logs on bind failure, so a collision used to let this
  // class pass while exercising nothing.
  private static int port = TestPort.choose();
  private static final InetSocketAddress REMOTE = new InetSocketAddress("127.0.0.2", 18888);
  private final List<Channel> channels = new ArrayList<>();
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private ConnPoolService pool;

  private static class CountingPeerClient extends PeerClient {

    private final AtomicInteger connectCount = new AtomicInteger();
    private ChannelFuture future = Mockito.mock(ChannelFuture.class);

    @Override
    public ChannelFuture connectAsync(Node node, boolean discoveryMode) {
      connectCount.incrementAndGet();
      return future;
    }
  }

  @Before
  public void setUp() {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp(localIp);
    Parameter.p2pConfig.setIpv6("");
    Parameter.p2pConfig.setDiscoverEnable(false);
    Parameter.p2pConfig.setPort(port);
    Parameter.handlerList = new ArrayList<>();
    clearChannels();
    pool = new ConnPoolService();
  }

  @After
  public void tearDown() {
    try {
      channels.forEach(Channel::markDisconnected);
      pool.close();
      clearChannels();
      ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
    } finally {
      Parameter.p2pConfig = previousConfig;
      Parameter.handlerList = previousHandlers;
    }
  }

  private static Method setPeerClientAndGetConnectMethod(ConnPoolService connPoolService,
      PeerClient peerClient) throws Exception {
    Field peerClientField = ConnPoolService.class.getDeclaredField("peerClient");
    peerClientField.setAccessible(true);
    peerClientField.set(connPoolService, peerClient);
    Method connect = ConnPoolService.class.getDeclaredMethod("connect", boolean.class);
    connect.setAccessible(true);
    return connect;
  }

  @Test
  public void connectDoesNotRetryCachedActiveNode() throws Exception {
    InetSocketAddress activeNode = new InetSocketAddress("127.0.0.2", 18888);
    Parameter.p2pConfig.setActiveNodes(Collections.singletonList(activeNode));
    Parameter.p2pConfig.setMinConnections(0);
    Parameter.p2pConfig.setMinActiveConnections(0);
    try {
      ConnPoolService connPoolService = new ConnPoolService();
      CountingPeerClient peerClient = new CountingPeerClient();
      Method connect = setPeerClientAndGetConnectMethod(connPoolService, peerClient);
      connect.invoke(connPoolService, false);
      connect.invoke(connPoolService, false);

      Assert.assertEquals(1, peerClient.connectCount.get());
    } finally {
      Parameter.p2pConfig.setActiveNodes(Collections.emptyList());
      Parameter.p2pConfig.setMinConnections(8);
      Parameter.p2pConfig.setMinActiveConnections(2);
    }
  }

  @Test
  public void triggerConnectClearsCachedActiveNode() throws Exception {
    InetSocketAddress activeNode = new InetSocketAddress("127.0.0.3", 18888);
    Parameter.p2pConfig.setActiveNodes(Collections.singletonList(activeNode));
    Parameter.p2pConfig.setMinConnections(0);
    Parameter.p2pConfig.setMinActiveConnections(0);
    try {
      ConnPoolService connPoolService = new ConnPoolService();
      CountingPeerClient peerClient = new CountingPeerClient();
      Method connect = setPeerClientAndGetConnectMethod(connPoolService, peerClient);
      connect.invoke(connPoolService, false);
      connPoolService.triggerConnect(activeNode);
      connect.invoke(connPoolService, false);

      Assert.assertEquals(2, peerClient.connectCount.get());
    } finally {
      Parameter.p2pConfig.setActiveNodes(Collections.emptyList());
      Parameter.p2pConfig.setMinConnections(8);
      Parameter.p2pConfig.setMinActiveConnections(2);
    }
  }

  @Test
  public void fastConnectionFailureAllowsRetryOnNextTick() throws Exception {
    InetSocketAddress activeNode = new InetSocketAddress("127.0.0.4", 18888);
    Parameter.p2pConfig.setActiveNodes(Collections.singletonList(activeNode));
    Parameter.p2pConfig.setMinConnections(0);
    Parameter.p2pConfig.setMinActiveConnections(0);
    try {
      ConnPoolService connPoolService = new ConnPoolService();
      AtomicInteger attempts = new AtomicInteger();
      PeerClient peerClient = new PeerClient() {
        @Override
        public ChannelFuture connectAsync(Node node, boolean discoveryMode) {
          attempts.incrementAndGet();
          connPoolService.triggerConnect(node.getPreferInetSocketAddress());
          return Mockito.mock(ChannelFuture.class);
        }
      };
      Method connect = setPeerClientAndGetConnectMethod(connPoolService, peerClient);
      connect.invoke(connPoolService, false);
      connect.invoke(connPoolService, false);

      Assert.assertEquals(2, attempts.get());
    } finally {
      Parameter.p2pConfig.setActiveNodes(Collections.emptyList());
      Parameter.p2pConfig.setMinConnections(8);
      Parameter.p2pConfig.setMinActiveConnections(2);
    }
  }

  @Test
  public void runtimeAddedActiveNodeRetriesAfterFailure() throws Exception {
    List<InetSocketAddress> activeNodes = Parameter.p2pConfig.getActiveNodes();
    Parameter.p2pConfig.setMinConnections(0);
    Parameter.p2pConfig.setMinActiveConnections(0);
    try {
      ConnPoolService connPoolService = new ConnPoolService();
      InetSocketAddress activeNode = new InetSocketAddress("127.0.0.5", 18888);
      activeNodes.add(activeNode);
      AtomicInteger attempts = new AtomicInteger();
      PeerClient peerClient = new PeerClient() {
        @Override
        public ChannelFuture connectAsync(Node node, boolean discoveryMode) {
          attempts.incrementAndGet();
          connPoolService.triggerConnect(node.getPreferInetSocketAddress());
          return Mockito.mock(ChannelFuture.class);
        }
      };
      Method connect = setPeerClientAndGetConnectMethod(connPoolService, peerClient);
      connect.invoke(connPoolService, false);
      connect.invoke(connPoolService, false);

      Assert.assertEquals(2, attempts.get());
    } finally {
      Parameter.p2pConfig.setActiveNodes(Collections.emptyList());
      Parameter.p2pConfig.setMinConnections(8);
      Parameter.p2pConfig.setMinActiveConnections(2);
    }
  }

  @Test
  public void runtimeAddedActiveNodeFailureWaitsForNextTick() throws Exception {
    List<InetSocketAddress> activeNodes = Parameter.p2pConfig.getActiveNodes();
    Parameter.p2pConfig.setMinConnections(1);
    Parameter.p2pConfig.setMinActiveConnections(0);
    Field discoveryField = NodeManager.class.getDeclaredField("discoverService");
    discoveryField.setAccessible(true);
    DiscoverService previousDiscovery = (DiscoverService) discoveryField.get(null);
    Field detectField = ChannelManager.class.getDeclaredField("nodeDetectService");
    detectField.setAccessible(true);
    NodeDetectService previousDetect = (NodeDetectService) detectField.get(null);
    ConnPoolService connPoolService = null;
    try {
      detectField.set(null, new NodeDetectService());
      connPoolService = new ConnPoolService();
      InetSocketAddress activeNode = new InetSocketAddress("127.0.0.6", 18888);
      activeNodes.add(activeNode);
      Node candidate = new Node(new byte[64], "127.0.0.6", "", 18888);
      discoveryField.set(null, new KadService() {
        @Override
        public List<Node> getConnectableNodes() {
          return Collections.singletonList(candidate);
        }
      });
      AtomicInteger attempts = new AtomicInteger();
      List<InetSocketAddress> dialed = Collections.synchronizedList(new ArrayList<>());
      ConnPoolService pool = connPoolService;
      PeerClient peerClient = new PeerClient() {
        @Override
        public ChannelFuture connectAsync(Node node, boolean discoveryMode) {
          dialed.add(node.getPreferInetSocketAddress());
          if (attempts.incrementAndGet() == 1) {
            pool.triggerConnect(node.getPreferInetSocketAddress());
          }
          return Mockito.mock(ChannelFuture.class);
        }
      };
      Method connect = setPeerClientAndGetConnectMethod(pool, peerClient);
      connect.invoke(pool, false);
      Field executorField = ConnPoolService.class.getDeclaredField("poolLoopExecutor");
      executorField.setAccessible(true);
      ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) executorField.get(pool);
      executor.submit(() -> { }).get(5, TimeUnit.SECONDS);

      Assert.assertEquals(1, attempts.get());
      Assert.assertEquals(activeNode, dialed.get(0));
      Assert.assertEquals(0, pool.getConnectingPeersCount().get());
    } finally {
      if (connPoolService != null) {
        connPoolService.close();
      }
      discoveryField.set(null, previousDiscovery);
      detectField.set(null, previousDetect);
      Parameter.p2pConfig.setActiveNodes(Collections.emptyList());
      Parameter.p2pConfig.setMinConnections(8);
      Parameter.p2pConfig.setMinActiveConnections(2);
    }
  }

  @Test
  public void poolScanUsesItsConfigAndSkipsBlockedActiveNodes() throws Exception {
    P2pConfig isolatedConfig = new P2pConfig();
    isolatedConfig.setMinConnections(0);
    isolatedConfig.setMinActiveConnections(0);
    InetSocketAddress address = new InetSocketAddress("192.0.2.10", 18888);
    isolatedConfig.getActiveNodes().add(address);
    Assert.assertFalse(Parameter.p2pConfig.getActiveNodes().contains(address));
    CountingPeerClient peerClient = new CountingPeerClient();
    peerClient.future = null;
    ConnPoolService connPoolService = new ConnPoolService();
    connPoolService.p2pConfig = isolatedConfig;
    Method connect = setPeerClientAndGetConnectMethod(connPoolService, peerClient);
    try {
      connect.invoke(connPoolService, false);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount.get());

      isolatedConfig.getActiveNodes().remove(address);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount.get());

      InetSocketAddress blocked = new InetSocketAddress("192.0.2.12", 18888);
      isolatedConfig.getActiveNodes().add(blocked);
      ConnectionPolicy.replaceBlockedIps(Collections.singleton(blocked.getAddress()));
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount.get());
    } finally {
      connPoolService.close();
    }
  }

  @Test
  public void poolScanCountsOnlyScheduledDials() throws Exception {
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
    peerClient.future = null;
    ConnPoolService connPoolService = new ConnPoolService();
    connPoolService.p2pConfig = isolatedConfig;
    Method connect = setPeerClientAndGetConnectMethod(connPoolService, peerClient);
    try {
      detectServiceField.set(null, nodeDetectService);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(1, peerClient.connectCount.get());
      Assert.assertEquals(0, connPoolService.getConnectingPeersCount().get());

      peerClient.future = Mockito.mock(ChannelFuture.class);
      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount.get());
      Assert.assertEquals(1, connPoolService.getConnectingPeersCount().get());

      connect.invoke(connPoolService, false);
      Assert.assertEquals(2, peerClient.connectCount.get());
      Assert.assertEquals(1, connPoolService.getConnectingPeersCount().get());
    } finally {
      connPoolService.close();
      detectServiceField.set(null, originalDetectService);
    }
  }

  @Test
  public void getNodes_chooseHomeNode() {
    InetSocketAddress localAddress = new InetSocketAddress(Parameter.p2pConfig.getIp(),
        Parameter.p2pConfig.getPort());
    Set<InetSocketAddress> inetInUse = new HashSet<>();
    inetInUse.add(localAddress);

    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(new Node(Parameter.p2pConfig.getNodeID(),
        Parameter.p2pConfig.getIp(), Parameter.p2pConfig.getIpv6(), Parameter.p2pConfig.getPort()));

    List<Node> nodes = pool.getNodes(new HashSet<>(), inetInUse, connectableNodes,
        1);
    Assert.assertEquals(0, nodes.size());

    nodes = pool.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        1);
    Assert.assertEquals(1, nodes.size());
  }

  @Test
  public void getNodes_respectsLimit() {
    Node node1 = new Node(new InetSocketAddress(localIp, 90));
    Node node2 = new Node(new InetSocketAddress(localIp, 100));

    List<Node> connectableNodes = new ArrayList<>();
    connectableNodes.add(node1);
    connectableNodes.add(node2);

    List<Node> nodes = pool.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        2);
    Assert.assertEquals(2, nodes.size());
    // getNodes shuffles candidates, so compare their contents without assuming an order.
    Assert.assertEquals(new HashSet<>(connectableNodes), new HashSet<>(nodes));

    int limit = 1;
    List<Node> nodes2 = pool.getNodes(new HashSet<>(), new HashSet<>(), connectableNodes,
        limit);
    Assert.assertEquals(limit, nodes2.size());
    Assert.assertTrue(connectableNodes.containsAll(nodes2));
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
  public void sameAddressPeersAreTrackedSeparatelyInEitherCloseOrder() throws Exception {
    for (boolean firstActive : new boolean[]{false, true}) {
      for (boolean closeFirst : new boolean[]{false, true}) {
        Channel first = channel(firstActive);
        Channel second = channel(!firstActive);
        Assert.assertNotSame(first, second);
        Assert.assertEquals(first, second);
        pool.onConnect(first);
        pool.onConnect(second);
        assertCounts(1, 1);
        assertPeers(first, second);

        pool.onDisconnect(closeFirst ? first : second);
        Channel survivor = closeFirst ? second : first;
        assertCounts(survivor.isActive() ? 1 : 0, survivor.isActive() ? 0 : 1);
        assertPeers(survivor);
        pool.onDisconnect(survivor);
        assertCounts(0, 0);
        assertPeers();
      }
    }
  }

  @Test
  public void unregisteredSameAddressDisconnectDoesNotRemoveRegisteredPeer() throws Exception {
    for (boolean active : new boolean[]{false, true}) {
      Channel registered = channel(active);
      Channel unregistered = channel(!active);
      pool.onConnect(registered);
      Assert.assertFalse(unregistered.isFinishHandshake());

      pool.onDisconnect(unregistered);
      pool.onDisconnect(unregistered);
      assertCounts(active ? 1 : 0, active ? 0 : 1);
      assertPeers(registered);
      pool.onDisconnect(registered);
      assertCounts(0, 0);
      assertPeers();
    }
  }

  @Test
  public void duplicateCallbacksCountEachInstanceOnce() throws Exception {
    Channel inbound = channel(false);
    Channel outbound = channel(true);
    pool.onConnect(inbound);
    pool.onConnect(inbound);
    pool.onConnect(outbound);
    pool.onConnect(outbound);
    assertCounts(1, 1);
    assertPeers(inbound, outbound);

    pool.onDisconnect(inbound);
    pool.onDisconnect(inbound);
    assertCounts(1, 0);
    assertPeers(outbound);
    pool.onDisconnect(outbound);
    pool.onDisconnect(outbound);
    assertCounts(0, 0);
    assertPeers();
  }

  @Test
  public void lateDisconnectDoesNotRemoveReplacementAtSameAddress() throws Exception {
    for (boolean active : new boolean[]{false, true}) {
      Channel old = channel(active);
      pool.onConnect(old);
      pool.onDisconnect(old);
      Channel replacement = channel(!active);
      pool.onConnect(replacement);

      pool.onDisconnect(old);
      assertCounts(active ? 0 : 1, active ? 1 : 0);
      assertPeers(replacement);
      pool.onDisconnect(replacement);
      assertCounts(0, 0);
      assertPeers();
    }
  }

  @Test
  public void repeatedSameAddressLifecyclesDoNotAccumulateCounterDrift() throws Exception {
    for (int i = 0; i < 100; i++) {
      Channel first = channel(i % 2 == 0);
      Channel second = channel(!first.isActive());
      pool.onConnect(first);
      pool.onConnect(second);
      pool.onDisconnect(second);
      pool.onDisconnect(first);
      assertCounts(0, 0);
      assertPeers();
    }
  }

  private Channel channel(boolean active) throws Exception {
    Channel channel = new Channel();
    set(channel, "inetSocketAddress", REMOTE);
    set(channel, "inetAddress", REMOTE.getAddress());
    set(channel, "isActive", active);
    channels.add(channel);
    return channel;
  }

  private void assertCounts(int active, int passive) {
    Assert.assertEquals(active, pool.getActivePeersCount().get());
    Assert.assertEquals(passive, pool.getPassivePeersCount().get());
  }

  @SuppressWarnings("unchecked")
  private void assertPeers(Channel... expected) throws Exception {
    Field field = ConnPoolService.class.getDeclaredField("activePeers");
    field.setAccessible(true);
    List<Channel> actual = (List<Channel>) field.get(pool);
    Assert.assertEquals(expected.length, actual.size());
    for (int i = 0; i < expected.length; i++) {
      Assert.assertSame(expected[i], actual.get(i));
    }
  }

  private void set(Channel channel, String name, Object value) throws Exception {
    Field field = Channel.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(channel, value);
  }

  private void clearChannels() {
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    ChannelManager.getBannedNodes().invalidateAll();
  }
}
