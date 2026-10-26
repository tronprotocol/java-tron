package org.tron.p2p.connection;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.socket.PeerClient;
import org.tron.p2p.discover.Node;

public class ConnPoolConnectionTargetTest {

  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private final List<Channel> peers = new ArrayList<>();
  private final List<Node> candidates = new ArrayList<>();
  private final List<ChannelPromise> dials = new ArrayList<>();
  private final List<InetSocketAddress> dialedAddresses = new ArrayList<>();
  private final List<InetSocketAddress> configuredAddresses = new ArrayList<>();
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private NodeDetectService previousDetect;
  private NodeDetectService detect;
  private Map<InetAddress, Long> previousBans;
  private List<Channel> previousChannels;
  private ConnPoolService pool;
  private Method connect;
  private boolean admitDuringDial;
  private boolean failConfiguredDuringDial;
  private boolean rejectConfiguredDial;

  @Before
  public void setUp() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    previousDetect = ChannelManager.getNodeDetectService();
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    previousChannels = new ArrayList<>(registry());
    registry().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.handlerList = new ArrayList<>();
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
    Parameter.p2pConfig.setMinConnections(8);
    Parameter.p2pConfig.setMinActiveConnections(3);
    for (int i = 1; i <= 20; i++) {
      candidates.add(new Node(new InetSocketAddress("192.0.2." + i, 18888)));
    }
    detect = new NodeDetectService() {
      @Override
      public synchronized List<Node> getConnectableNodes() {
        return candidates;
      }
    };
    setStatic(ChannelManager.class, "nodeDetectService", detect);
    connect = ConnPoolService.class.getDeclaredMethod("connect", boolean.class);
    connect.setAccessible(true);
  }

  @After
  public void tearDown() throws Exception {
    try {
      peers.forEach(Channel::markDisconnected);
      sockets.forEach(EmbeddedChannel::finishAndReleaseAll);
      if (pool != null) {
        pool.close();
      }
      detect.close();
    } finally {
      registry().clear();
      registry().addAll(previousChannels);
      ChannelManager.getBannedNodes().invalidateAll();
      ChannelManager.getBannedNodes().putAll(previousBans);
      setStatic(ChannelManager.class, "nodeDetectService", previousDetect);
      Parameter.handlerList = previousHandlers;
      Parameter.p2pConfig = previousConfig;
    }
  }

  @Test
  public void twoOrdinaryPeersNeedOnlyOneMoreConnection() throws Exception {
    assertEstablishedTarget(2, 1);
  }

  @Test
  public void threeOrdinaryPeersAlreadySatisfyBothTargets() throws Exception {
    assertEstablishedTarget(3, 0);
  }

  @Test
  public void fourOrdinaryPeersAlreadySatisfyBothTargets() throws Exception {
    assertEstablishedTarget(4, 0);
  }

  @Test
  public void configuredPeersDoNotSatisfyOrdinaryOutboundTarget() throws Exception {
    startPool(8);
    addConfiguredPeers();

    scan(false);

    Assert.assertEquals(3, dials.size());
    Assert.assertEquals(3, pool.getOrdinaryOutboundConnectionCount().get());
  }

  @Test
  public void passivePeersContributeToTotalTarget() throws Exception {
    startPool(2);
    addConfiguredPeers();
    addPeer(newSocket(new InetSocketAddress("198.51.100.1", 18888)), true);
    addPeer(newSocket(new InetSocketAddress("198.51.100.2", 20000)), false);
    addPeer(newSocket(new InetSocketAddress("198.51.100.3", 20001)), false);
    pool.getOrdinaryOutboundConnectionCount().set(1);

    scan(false);

    Assert.assertEquals(3, dials.size());
  }

  @Test
  public void ordinaryPendingDialsAndCompletedHandshakesAreCountedOnce() throws Exception {
    startPool(0);

    scan(false);
    Assert.assertEquals(8, dials.size());
    Assert.assertEquals(8, pool.getOrdinaryOutboundConnectionCount().get());
    scan(false);
    Assert.assertEquals(8, dials.size());

    // TCP completion alone does not finish the peer handshake.
    for (ChannelPromise dial : dials) {
      dial.setSuccess();
    }
    scan(false);
    Assert.assertEquals(8, dials.size());

    for (ChannelPromise dial : dials) {
      addPeer((EmbeddedChannel) dial.channel(), true);
    }
    scan(false);
    Assert.assertEquals(8, dials.size());
  }

  @Test
  public void failedOrdinaryDialReleasesTotalBudget() throws Exception {
    Parameter.p2pConfig.setMinConnections(1);
    Parameter.p2pConfig.setMinActiveConnections(0);
    startPool(0);
    scan(false);
    Assert.assertEquals(1, dials.size());

    dials.get(0).setFailure(new IOException("connection failed"));
    dials.get(0).channel().close().syncUninterruptibly();
    pool.triggerConnect(dialedAddresses.get(0));
    Field executorField = ConnPoolService.class.getDeclaredField("poolLoopExecutor");
    executorField.setAccessible(true);
    ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) executorField.get(pool);
    executor.submit(() -> { }).get(5, TimeUnit.SECONDS);

    Assert.assertEquals(2, dials.size());
    Assert.assertEquals(1, pool.getOrdinaryOutboundConnectionCount().get());
    scan(false);
    Assert.assertEquals(2, dials.size());
  }

  @Test
  public void configuredRetriesDoNotStarveOrdinaryConnections() throws Exception {
    assertConfiguredRetriesDoNotReserveBudget(3);
  }

  @Test
  public void configuredRetriesAllowOrdinaryConnectionsWithZeroOutboundMinimum() throws Exception {
    assertConfiguredRetriesDoNotReserveBudget(0);
  }

  @Test
  public void configuredPendingDialsDoNotStopTotalReplenishmentAtOutboundMinimum()
      throws Exception {
    startPool(8);
    for (int i = 1; i <= 3; i++) {
      addPeer(newSocket(new InetSocketAddress("198.51.100." + i, 18888)), true);
    }
    pool.getOrdinaryOutboundConnectionCount().set(3);

    scan(false);

    Assert.assertEquals(13, dials.size());
    Assert.assertEquals(8, pool.getOrdinaryOutboundConnectionCount().get());
    scan(false);
    Assert.assertEquals(13, dials.size());
  }

  @Test
  public void immediatelyFailedConfiguredDialDoesNotReserveTotalBudget() throws Exception {
    assertUnscheduledConfiguredDialDoesNotReserveBudget(false);
  }

  @Test
  public void rejectedConfiguredDialDoesNotReserveTotalBudget() throws Exception {
    assertUnscheduledConfiguredDialDoesNotReserveBudget(true);
  }

  @Test
  public void inboundPeerDoesNotHidePendingOutboundSocketAtSameAddress() throws Exception {
    Parameter.p2pConfig.setMinConnections(1);
    Parameter.p2pConfig.setMinActiveConnections(0);
    startPool(0);
    scan(false);
    Assert.assertEquals(1, dials.size());

    addPeer(newSocket(dialedAddresses.get(0)), false);
    Parameter.p2pConfig.setMinConnections(2);
    scan(false);

    Assert.assertEquals(1, dials.size());
  }

  @Test
  public void handshakeBeforeDialReturnsDoesNotLeavePendingCount() throws Exception {
    Parameter.p2pConfig.setMinConnections(1);
    Parameter.p2pConfig.setMinActiveConnections(0);
    startPool(0);
    admitDuringDial = true;
    scan(false);
    Assert.assertEquals(1, dials.size());

    Parameter.p2pConfig.setMinConnections(2);
    scan(false);

    Assert.assertEquals(2, dials.size());
  }

  @Test
  public void runtimeActiveCandidatesAlsoContributeToOrdinaryOutboundTarget() throws Exception {
    Parameter.p2pConfig.setMinConnections(3);
    startPool(0);
    for (int i = 1; i <= 3; i++) {
      Parameter.p2pConfig.getActiveNodes().add(new InetSocketAddress("203.0.113." + i, 18888));
    }

    scan(false);

    Assert.assertEquals(3, dials.size());
    Assert.assertEquals(3, pool.getOrdinaryOutboundConnectionCount().get());
    Assert.assertTrue(Parameter.p2pConfig.getActiveNodes().containsAll(dialedAddresses));
  }

  private void assertEstablishedTarget(int ordinaryPeers, int expectedDials) throws Exception {
    startPool(5);
    addConfiguredPeers();
    for (int i = 1; i <= ordinaryPeers; i++) {
      addPeer(newSocket(new InetSocketAddress("198.51.100." + i, 18888)), true);
    }
    pool.getOrdinaryOutboundConnectionCount().set(ordinaryPeers);

    scan(false);

    Assert.assertEquals(expectedDials, dials.size());
    scan(false);
    Assert.assertEquals(expectedDials, dials.size());
  }

  private void assertConfiguredRetriesDoNotReserveBudget(int outboundMinimum) throws Exception {
    Parameter.p2pConfig.setMinActiveConnections(outboundMinimum);
    startPool(8);

    scan(false);
    Assert.assertEquals(16, dials.size());
    Assert.assertEquals(8, pool.getOrdinaryOutboundConnectionCount().get());
    for (int i = 0; i < dials.size(); i++) {
      if (!configuredAddresses.contains(dialedAddresses.get(i))) {
        dials.get(i).setSuccess();
        addPeer((EmbeddedChannel) dials.get(i).channel(), true);
      }
    }
    Assert.assertEquals(8, peers.size());

    for (int retry = 0; retry < 3; retry++) {
      int previousAttempts = dials.size();
      for (int i = 0; i < previousAttempts; i++) {
        if (configuredAddresses.contains(dialedAddresses.get(i))
            && dials.get(i).channel().isOpen()) {
          dials.get(i).setFailure(new IOException("connection timed out"));
          dials.get(i).channel().close().syncUninterruptibly();
          pool.triggerConnect(dialedAddresses.get(i));
        }
      }
      scan(false);
      Assert.assertEquals(previousAttempts + 8, dials.size());
      Assert.assertTrue(configuredAddresses.containsAll(
          dialedAddresses.subList(previousAttempts, dials.size())));
      Assert.assertEquals(8, pool.getOrdinaryOutboundConnectionCount().get());
      scan(false);
      Assert.assertEquals(previousAttempts + 8, dials.size());
    }
  }

  private void assertUnscheduledConfiguredDialDoesNotReserveBudget(boolean rejected)
      throws Exception {
    Parameter.p2pConfig.setMinConnections(1);
    Parameter.p2pConfig.setMinActiveConnections(0);
    startPool(1);
    failConfiguredDuringDial = !rejected;
    rejectConfiguredDial = rejected;

    scan(false);

    Assert.assertEquals(2, dials.size());
    Assert.assertFalse(configuredAddresses.contains(dialedAddresses.get(1)));
    Assert.assertEquals(1, pool.getOrdinaryOutboundConnectionCount().get());
  }

  private void startPool(int configuredPeers) throws Exception {
    for (int i = 1; i <= configuredPeers; i++) {
      configuredAddresses.add(new InetSocketAddress("203.0.113." + i, 18888));
    }
    Parameter.p2pConfig.setActiveNodes(new ArrayList<>(configuredAddresses));
    pool = new ConnPoolService();
    Field client = ConnPoolService.class.getDeclaredField("peerClient");
    client.setAccessible(true);
    client.set(pool, new PeerClient() {
      @Override
      public ChannelFuture connectAsync(Node node, boolean nodeDetectMode) {
        InetSocketAddress address = node.getPreferInetSocketAddress();
        EmbeddedChannel socket = newSocket(address);
        ChannelPromise dial = socket.newPromise();
        dials.add(dial);
        dialedAddresses.add(address);
        if (configuredAddresses.contains(address)
            && (failConfiguredDuringDial || rejectConfiguredDial)) {
          dial.setFailure(new IOException("connection failed"));
          socket.close();
          return rejectConfiguredDial ? null : dial;
        }
        if (admitDuringDial) {
          try {
            addPeer(socket, true);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        }
        return dial;
      }
    });
  }

  private void addConfiguredPeers() throws Exception {
    for (InetSocketAddress address : configuredAddresses) {
      addPeer(newSocket(address), true);
    }
  }

  private EmbeddedChannel newSocket(InetSocketAddress address) {
    EmbeddedChannel socket = new EmbeddedChannel(new ChannelInboundHandlerAdapter()) {
      @Override
      protected SocketAddress remoteAddress0() {
        return address;
      }
    };
    sockets.add(socket);
    return socket;
  }

  private void addPeer(EmbeddedChannel socket, boolean outbound) throws Exception {
    Channel peer = new Channel();
    peer.setChannelHandlerContext(socket.pipeline().firstContext());
    Field direction = Channel.class.getDeclaredField("outbound");
    direction.setAccessible(true);
    direction.set(peer, outbound);
    ChannelManager.addPeer(peer);
    pool.onConnect(peer);
    peers.add(peer);
  }

  private void scan(boolean filterActive) throws Exception {
    connect.invoke(pool, filterActive);
  }

  @SuppressWarnings("unchecked")
  private Collection<Channel> registry() throws Exception {
    Field field = ChannelManager.class.getDeclaredField("channels");
    field.setAccessible(true);
    return (Collection<Channel>) field.get(null);
  }

  private void setStatic(Class<?> type, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }
}
