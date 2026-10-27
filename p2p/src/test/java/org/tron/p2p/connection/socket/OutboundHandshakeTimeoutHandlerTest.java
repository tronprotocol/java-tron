package org.tron.p2p.connection.socket;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.detect.NodeStat;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.keepalive.KeepAliveService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.NodeManager;
import org.tron.p2p.discover.protocol.kad.KadService;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class OutboundHandshakeTimeoutHandlerTest {

  private static final InetSocketAddress REMOTE = new InetSocketAddress("192.0.2.1", 18888);
  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private final List<Channel> peers = new ArrayList<>();
  private final List<Node> candidates = new ArrayList<>();
  private final Map<String, Object> previousServices = new HashMap<>();
  private final AtomicInteger disconnectCallbacks = new AtomicInteger();
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private Map<InetAddress, Long> previousBans;
  private Map<InetAddress, Long> previousBadNodes;
  private boolean previousShutdown;
  private Object previousDiscovery;
  private ConnPoolService pool;
  private NodeDetectService detect;
  private KeepAliveService keepAlive;

  @Before
  public void setUp() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    previousBadNodes = new HashMap<>(NodeDetectService.getBadNodesCache().asMap());
    previousShutdown = ChannelManager.isShutdown;
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    ChannelManager.getBannedNodes().invalidateAll();
    NodeDetectService.getBadNodesCache().invalidateAll();
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
    Parameter.p2pConfig.setNodeID(new byte[64]);
    Parameter.p2pConfig.setMinConnections(1);
    Parameter.p2pConfig.setMinActiveConnections(0);
    Parameter.handlerList = new ArrayList<>();
    ChannelManager.isShutdown = false;
    pool = new ConnPoolService();
    keepAlive = new KeepAliveService();
    detect = new NodeDetectService() {
      @Override
      public synchronized List<Node> getConnectableNodes() {
        return new ArrayList<>(candidates);
      }
    };
    installService("connPoolService", pool);
    installService("handshakeService", new HandshakeService());
    installService("keepAliveService", keepAlive);
    installService("nodeDetectService", detect);
    Field discovery = field(NodeManager.class, "discoverService");
    previousDiscovery = discovery.get(null);
    discovery.set(null, new KadService() {
      @Override
      public List<Node> getConnectableNodes() {
        return new ArrayList<>();
      }
    });
    Parameter.handlerList.add(new P2pEventHandler() {
      @Override
      public void onDisconnect(Channel channel) {
        disconnectCallbacks.incrementAndGet();
      }
    });
  }

  @After
  public void tearDown() throws Exception {
    try {
      ChannelManager.isShutdown = true;
      sockets.forEach(EmbeddedChannel::finishAndReleaseAll);
      pool.close();
      keepAlive.close();
      detect.close();
      ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    } finally {
      for (Map.Entry<String, Object> entry : previousServices.entrySet()) {
        field(ChannelManager.class, entry.getKey()).set(null, entry.getValue());
      }
      Parameter.p2pConfig = previousConfig;
      Parameter.handlerList = previousHandlers;
      ChannelManager.getBannedNodes().invalidateAll();
      ChannelManager.getBannedNodes().putAll(previousBans);
      NodeDetectService.getBadNodesCache().invalidateAll();
      NodeDetectService.getBadNodesCache().putAll(previousBadNodes);
      ChannelManager.isShutdown = previousShutdown;
      field(NodeManager.class, "discoverService").set(null, previousDiscovery);
    }
  }

  @Test
  public void silentOutboundExpiresAtHelloDeadline() {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS - 1);
    Assert.assertTrue(socket.isOpen());
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
    Assert.assertFalse(peers.get(0).isFinishHandshake());
  }

  @Test
  public void pingTrafficDoesNotExtendHelloDeadline() throws Exception {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    for (long elapsed = 1; elapsed < Parameter.HANDSHAKE_TIMEOUT_SECONDS; elapsed++) {
      advance(socket, 1);
      receive(socket, new PingMessage().getSendData());
      Assert.assertEquals(MessageType.KEEP_ALIVE_PONG,
          Message.parse(readOutbound(socket)).getType());
      Assert.assertTrue(socket.isOpen());
    }
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void pongTrafficDoesNotExtendHelloDeadline() {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    for (long elapsed = 1; elapsed < Parameter.HANDSHAKE_TIMEOUT_SECONDS; elapsed++) {
      advance(socket, 1);
      receive(socket, new PongMessage().getSendData());
      Assert.assertTrue(socket.isOpen());
    }
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void repeatedActivationDoesNotRestartDeadline() {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    advance(socket, 5);
    socket.pipeline().fireChannelActive();
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS - 5);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void successfulHelloCancelsDeadline() throws Exception {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    OutboundHandshakeTimeoutHandler guard = socket.pipeline()
        .get(OutboundHandshakeTimeoutHandler.class);
    ScheduledFuture<?> task = timeoutTask(guard);
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS - 1);
    receive(socket, hello());
    Assert.assertTrue(peers.get(0).isFinishHandshake());
    Assert.assertTrue(peers.get(0).isRegisteredPeer());
    Assert.assertNull(socket.pipeline().get(OutboundHandshakeTimeoutHandler.class));
    Assert.assertTrue(task.isCancelled());
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS + 1);
    Assert.assertTrue(socket.isOpen());
    Assert.assertEquals(1, ChannelManager.getChannelCount());
  }

  @Test
  public void lateHelloIsRejectedBeforeTimeoutTaskRuns() throws Exception {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    socket.advanceTimeBy(Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    receive(socket, hello());
    Assert.assertFalse(socket.isOpen());
    Assert.assertFalse(peers.get(0).isFinishHandshake());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void completionChecksDeadlineEvenWithoutAnotherRead() {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    socket.advanceTimeBy(Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    Assert.assertFalse(OutboundHandshakeTimeoutHandler.handshakeCompleted(socket));
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void closeCancelsDeadlineAndNotifiesOnce() throws Exception {
    EmbeddedChannel socket = outbound(REMOTE, false, false);
    ScheduledFuture<?> task = timeoutTask(socket.pipeline()
        .get(OutboundHandshakeTimeoutHandler.class));
    socket.close();
    socket.close();
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS + 1);
    Assert.assertTrue(task.isCancelled());
    Assert.assertEquals(1, disconnectCallbacks.get());
  }

  @Test
  public void deadlineStartsWhenSocketBecomesActive() throws Exception {
    AtomicBoolean active = new AtomicBoolean();
    EmbeddedChannel socket = new EmbeddedChannel() {
      @Override
      public boolean isActive() {
        return active.get() && super.isActive();
      }
    };
    sockets.add(socket);
    socket.freezeTime();
    socket.pipeline().addLast(new OutboundHandshakeTimeoutHandler());
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS + 1);
    Assert.assertTrue(socket.isOpen());
    active.set(true);
    socket.pipeline().fireChannelActive();
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS - 1);
    Assert.assertTrue(socket.isOpen());
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void discoveryConnectionsDoNotUseHelloDeadline() {
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    Assert.assertNull(socket.pipeline().get(OutboundHandshakeTimeoutHandler.class));
    advance(socket, Parameter.NODE_DETECT_TIMEOUT - 1, TimeUnit.MILLISECONDS);
    Assert.assertTrue(socket.isOpen());
    advance(socket, 1, TimeUnit.MILLISECONDS);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void fragmentedInputDoesNotExtendStatusDeadline() {
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{127}));
    for (int elapsed = 250; elapsed < Parameter.NODE_DETECT_TIMEOUT; elapsed += 250) {
      advance(socket, 250, TimeUnit.MILLISECONDS);
      socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{0}));
      Assert.assertTrue(socket.isOpen());
    }
    advance(socket, 250, TimeUnit.MILLISECONDS);
    Assert.assertFalse(socket.isOpen());
    Assert.assertEquals(0, disconnectCallbacks.get());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void lateStatusAfterRecordRemovalClosesSocket() throws Exception {
    NodeStat stat = pendingProbe();
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    stat.setLastDetectTime(System.currentTimeMillis() - Parameter.NODE_DETECT_TIMEOUT - 1);
    detect.trimNodeMap();
    Assert.assertFalse(nodeStats().containsKey(REMOTE));
    Assert.assertTrue(socket.isOpen());
    receive(socket, new StatusMessage().getSendData());
    Assert.assertFalse(socket.isOpen());
    Assert.assertNull(stat.getStatusMessage());
  }

  @Test
  public void successfulStatusClosesSocketAndCancelsDeadline() throws Exception {
    NodeStat stat = pendingProbe();
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    ScheduledFuture<?> task = statusTimeoutTask(socket);
    receive(socket, new StatusMessage().getSendData());
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(task.isCancelled());
    Assert.assertSame(stat, nodeStats().get(REMOTE));
    Assert.assertNotNull(stat.getStatusMessage());
    Assert.assertTrue(stat.finishDetect());
    Assert.assertTrue(NodeDetectService.getBadNodesCache().asMap().isEmpty());
    Assert.assertEquals(0, disconnectCallbacks.get());
  }

  @Test
  public void statusTimeoutRemovesPendingProbe() throws Exception {
    NodeStat stat = pendingProbe();
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    ScheduledFuture<?> task = statusTimeoutTask(socket);
    advance(socket, Parameter.NODE_DETECT_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(task.isDone());
    Assert.assertFalse(nodeStats().containsKey(REMOTE));
    Assert.assertNull(stat.getStatusMessage());
    Assert.assertNotNull(NodeDetectService.getBadNodesCache().getIfPresent(REMOTE.getAddress()));
  }

  @Test
  public void failedStatusClosesSocketAndCancelsDeadline() throws Exception {
    pendingProbe();
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    ScheduledFuture<?> task = statusTimeoutTask(socket);
    receive(socket, new byte[]{MessageType.STATUS.getType(), (byte) 0x80});
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(task.isCancelled());
    Assert.assertFalse(nodeStats().containsKey(REMOTE));
  }

  @Test
  public void lateStatusIsRejectedBeforeTimeoutTaskRuns() throws Exception {
    NodeStat stat = pendingProbe();
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    socket.advanceTimeBy(Parameter.NODE_DETECT_TIMEOUT, TimeUnit.MILLISECONDS);
    receive(socket, new StatusMessage().getSendData());
    Assert.assertFalse(socket.isOpen());
    Assert.assertNull(stat.getStatusMessage());
  }

  @Test
  public void statusProcessingRechecksDeadlineAfterDispatch() throws Exception {
    NodeStat stat = pendingProbe();
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    socket.advanceTimeBy(Parameter.NODE_DETECT_TIMEOUT, TimeUnit.MILLISECONDS);
    detect.processMessage(peers.get(0), new StatusMessage());
    Assert.assertFalse(socket.isOpen());
    Assert.assertNull(stat.getStatusMessage());
  }

  @Test
  public void repeatedActivationDoesNotRestartStatusDeadline() {
    EmbeddedChannel socket = outbound(REMOTE, true, false);
    advance(socket, 1);
    socket.pipeline().fireChannelActive();
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void statusDeadlineStartsWhenSocketBecomesActive() throws Exception {
    AtomicBoolean active = new AtomicBoolean();
    EmbeddedChannel socket = new EmbeddedChannel() {
      @Override
      public boolean isActive() {
        return active.get() && super.isActive();
      }
    };
    sockets.add(socket);
    socket.freezeTime();
    socket.pipeline().addLast(new OutboundStatusTimeoutHandler());
    advance(socket, Parameter.NODE_DETECT_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertTrue(socket.isOpen());
    active.set(true);
    socket.pipeline().fireChannelActive();
    advance(socket, Parameter.NODE_DETECT_TIMEOUT - 1, TimeUnit.MILLISECONDS);
    Assert.assertTrue(socket.isOpen());
    advance(socket, 1, TimeUnit.MILLISECONDS);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void timeoutReleasesBudgetAndAllowsOrdinaryReplenishment() throws Exception {
    field(ConnPoolService.class, "peerClient").set(pool, new PeerClient() {
      @Override
      public ChannelFuture connectAsync(Node node, boolean nodeDetectMode) {
        return outbound(node.getPreferInetSocketAddress(), nodeDetectMode, true)
            .newSucceededFuture();
      }
    });
    candidates.add(new Node(REMOTE));
    scan();
    Assert.assertEquals(1, sockets.size());
    Assert.assertEquals(1, pool.getOrdinaryOutboundConnectionCount().get());
    scan();
    Assert.assertEquals(1, sockets.size());
    candidates.clear();
    advance(sockets.get(0), Parameter.HANDSHAKE_TIMEOUT_SECONDS);
    drainPool();
    Assert.assertFalse(sockets.get(0).isOpen());
    Assert.assertEquals(0, pool.getOrdinaryOutboundConnectionCount().get());
    sockets.get(0).close();
    Assert.assertEquals(0, pool.getOrdinaryOutboundConnectionCount().get());

    candidates.add(new Node(new InetSocketAddress("192.0.2.2", 18888)));
    scan();
    Assert.assertEquals(2, sockets.size());
    Assert.assertTrue(sockets.get(1).isOpen());
    Assert.assertEquals(1, pool.getOrdinaryOutboundConnectionCount().get());
  }

  @Test(timeout = 25000)
  public void realOutboundInitializerClosesAndReleasesTimedOutConnection() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    io.netty.channel.Channel server = null;
    io.netty.channel.Channel client = null;
    try {
      server = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) {
              io.netty.util.ReferenceCountUtil.release(msg);
            }
          }).bind("127.0.0.1", 0).sync().channel();
      // Reserve one ordinary dial, as ConnPoolService does before the handshake completes.
      pool.getOrdinaryOutboundConnectionCount().incrementAndGet();
      client = new Bootstrap().group(group).channel(NioSocketChannel.class)
          .handler(new P2pChannelInitializer("remote-node", false, true))
          .connect(server.localAddress()).sync().channel();
      client.eventLoop().submit(() -> { }).sync();
      Assert.assertNotNull(client.pipeline().get(OutboundHandshakeTimeoutHandler.class));
      Assert.assertTrue(client.closeFuture().await(
          Parameter.HANDSHAKE_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS));
      // closeFuture completes before its listeners necessarily finish.
      client.eventLoop().submit(() -> { }).sync();
      drainPool();
      Assert.assertEquals(0, pool.getOrdinaryOutboundConnectionCount().get());
      Assert.assertEquals(1, disconnectCallbacks.get());
    } finally {
      if (client != null) {
        client.close().syncUninterruptibly();
      }
      if (server != null) {
        server.close().syncUninterruptibly();
      }
      group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
    }
  }

  private EmbeddedChannel outbound(InetSocketAddress address, boolean nodeDetectMode,
      boolean countedByPool) {
    EmbeddedChannel socket = new EmbeddedChannel() {
      @Override
      protected SocketAddress remoteAddress0() {
        return address;
      }
    };
    sockets.add(socket);
    socket.freezeTime();
    Channel peer = new Channel();
    peers.add(peer);
    peer.init(socket.pipeline(), "outbound-peer", nodeDetectMode);
    peer.setChannelHandlerContext(socket.pipeline().context("messageHandler"));
    socket.closeFuture().addListener(future -> {
      peer.markDisconnected();
      if (peer.isNodeDetectMode() && !peer.isRegisteredPeer()) {
        detect.notifyDisconnect(peer);
        return;
      }
      ChannelManager.notifyDisconnect(peer);
      if (countedByPool) {
        ChannelManager.triggerConnect(address);
      }
    });
    return socket;
  }

  private void advance(EmbeddedChannel socket, long seconds) {
    advance(socket, seconds, TimeUnit.SECONDS);
  }

  private void advance(EmbeddedChannel socket, long duration, TimeUnit unit) {
    socket.advanceTimeBy(duration, unit);
    socket.runScheduledPendingTasks();
    socket.runPendingTasks();
    socket.checkException();
  }

  private byte[] hello() throws Exception {
    byte[] id = new byte[64];
    id[0] = 1;
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(id))
        .setAddress(ByteString.copyFromUtf8(REMOTE.getHostString()))
        .setPort(REMOTE.getPort()).build();
    return new HelloMessage(Connect.HelloMessage.newBuilder().setFrom(endpoint)
        .setNetworkId(Parameter.p2pConfig.getNetworkId()).setVersion(Parameter.version)
        .setCode(DisconnectCode.NORMAL.getValue()).setTimestamp(System.currentTimeMillis())
        .build().toByteArray()).getSendData();
  }

  private void receive(EmbeddedChannel socket, byte[] data) {
    ByteBuf frame = Unpooled.buffer();
    int length = data.length;
    while ((length & ~0x7f) != 0) {
      frame.writeByte((length & 0x7f) | 0x80);
      length >>>= 7;
    }
    frame.writeByte(length);
    frame.writeBytes(data);
    socket.writeInbound(frame);
    socket.checkException();
  }

  private byte[] readOutbound(EmbeddedChannel socket) throws Exception {
    ByteBuf bytes = Unpooled.buffer();
    try {
      ByteBuf part;
      while ((part = socket.readOutbound()) != null) {
        try {
          bytes.writeBytes(part);
        } finally {
          part.release();
        }
      }
      Assert.assertTrue(bytes.isReadable());
      byte[] data = new byte[bytes.readableBytes()];
      bytes.readBytes(data);
      CodedInputStream input = CodedInputStream.newInstance(data);
      byte[] payload = input.readRawBytes(input.readRawVarint32());
      Assert.assertTrue(input.isAtEnd());
      return payload;
    } finally {
      bytes.release();
    }
  }

  private void scan() throws Exception {
    Method connect = ConnPoolService.class.getDeclaredMethod("connect", boolean.class);
    connect.setAccessible(true);
    connect.invoke(pool, false);
  }

  private void drainPool() throws Exception {
    ScheduledThreadPoolExecutor executor =
        (ScheduledThreadPoolExecutor) field(ConnPoolService.class, "poolLoopExecutor").get(pool);
    executor.submit(() -> { }).get(5, TimeUnit.SECONDS);
  }

  private ScheduledFuture<?> timeoutTask(OutboundHandshakeTimeoutHandler handler) throws Exception {
    return (ScheduledFuture<?>) field(OutboundHandshakeTimeoutHandler.class, "timeoutTask")
        .get(handler);
  }

  private ScheduledFuture<?> statusTimeoutTask(EmbeddedChannel socket) throws Exception {
    return (ScheduledFuture<?>) field(OutboundStatusTimeoutHandler.class, "timeoutTask")
        .get(socket.pipeline().get(OutboundStatusTimeoutHandler.class));
  }

  private NodeStat pendingProbe() throws Exception {
    NodeStat stat = new NodeStat(new Node(REMOTE));
    stat.setLastDetectTime(System.currentTimeMillis());
    nodeStats().put(REMOTE, stat);
    return stat;
  }

  @SuppressWarnings("unchecked")
  private Map<InetSocketAddress, NodeStat> nodeStats() throws Exception {
    return (Map<InetSocketAddress, NodeStat>) field(NodeDetectService.class, "nodeStatMap")
        .get(detect);
  }

  private Field field(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  private void installService(String name, Object service) throws Exception {
    Field field = field(ChannelManager.class, name);
    previousServices.put(name, field.get(null));
    field.set(null, service);
  }
}
