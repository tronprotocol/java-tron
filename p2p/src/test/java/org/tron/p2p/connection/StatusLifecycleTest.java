package org.tron.p2p.connection;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.detect.NodeStat;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.socket.MessageHandler;
import org.tron.p2p.connection.socket.P2pChannelInitializer;
import org.tron.p2p.discover.Node;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.utils.NetUtil;

public class StatusLifecycleTest {

  private final Map<io.netty.channel.Channel, Channel> accepted = new ConcurrentHashMap<>();
  private final List<Socket> clients = new ArrayList<>();
  private final AtomicInteger connected = new AtomicInteger();
  private final AtomicInteger disconnected = new AtomicInteger();
  private NioEventLoopGroup group;
  private io.netty.channel.Channel listener;
  private ConnPoolService pool;
  private NodeDetectService detect;
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private Map<InetSocketAddress, Channel> previousChannels;
  private Map<InetAddress, Long> previousBans;
  private Map<InetAddress, Long> previousBadNodes;
  private NodeDetectService previousDetect;
  private HandshakeService previousHandshake;

  @Before
  public void init() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    previousChannels = new HashMap<>(ChannelManager.getChannels());
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    previousBadNodes = new HashMap<>(NodeDetectService.getBadNodesCache().asMap());
    previousDetect = ChannelManager.getNodeDetectService();
    previousHandshake = ChannelManager.getHandshakeService();
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
    Parameter.p2pConfig.setNodeDetectEnable(false);
    Parameter.handlerList = new ArrayList<>();
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    NodeDetectService.getBadNodesCache().invalidateAll();
    pool = new ConnPoolService();
    Parameter.handlerList.add(new P2pEventHandler() {
      @Override
      public void onConnect(Channel channel) {
        connected.incrementAndGet();
      }

      @Override
      public void onDisconnect(Channel channel) {
        disconnected.incrementAndGet();
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
      }
    });
    detect = new NodeDetectService();
    detect.init(null); // Local probing is disabled; inbound STATUS must still work.
    set(ChannelManager.class, null, "nodeDetectService", detect);
    set(ChannelManager.class, null, "handshakeService", new HandshakeService());
    group = new NioEventLoopGroup(1);
    listener = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
        .childHandler(initializer("", false)).bind("127.0.0.1", 0).sync().channel();
  }

  @After
  public void destroy() throws Exception {
    for (Socket socket : clients) {
      socket.close();
    }
    if (listener != null) {
      listener.close().sync();
    }
    if (group != null) {
      group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
    }
    if (pool != null) {
      pool.close();
    }
    if (detect != null) {
      detect.close();
    }
    ChannelManager.getChannels().clear();
    ChannelManager.getChannels().putAll(previousChannels);
    ChannelManager.getBannedNodes().invalidateAll();
    ChannelManager.getBannedNodes().putAll(previousBans);
    NodeDetectService.getBadNodesCache().invalidateAll();
    NodeDetectService.getBadNodesCache().putAll(previousBadNodes);
    Parameter.handlerList = previousHandlers;
    Parameter.p2pConfig = previousConfig;
    set(ChannelManager.class, null, "nodeDetectService", previousDetect);
    set(ChannelManager.class, null, "handshakeService", previousHandshake);
  }

  @Test
  public void statusFirstRemainsAValidProbe() throws Exception {
    Socket socket = open();
    write(socket, status());
    Assert.assertEquals(MessageType.STATUS, read(socket).getType());
    Assert.assertEquals(-1, socket.getInputStream().read());
    Channel channel = inbound(socket);
    awaitClosed(channel);
    Assert.assertTrue(channel.isDiscoveryMode());
    Assert.assertFalse(channel.isRegisteredPeer());
    Assert.assertTrue(ChannelManager.getChannels().isEmpty());
    Assert.assertTrue(ChannelManager.getBannedNodes().asMap().isEmpty());
    Assert.assertEquals(0, connected.get());
    Assert.assertEquals(0, disconnected.get());
  }

  @Test
  public void statusAfterHelloReleasesPeerSlotsRepeatedly() throws Exception {
    for (int i = 0; i < 100; i++) {
      Socket socket = handshake();
      Channel channel = inbound(socket);
      Assert.assertEquals(1, pool.getPassivePeersCount().get());
      write(socket, status());
      Assert.assertEquals(-1, socket.getInputStream().read());
      awaitClosed(channel);
      Assert.assertFalse(channel.isDiscoveryMode());
      Assert.assertTrue(channel.isRegisteredPeer());
      Assert.assertTrue(channel.getDisconnectTime() > 0);
      Assert.assertTrue(ChannelManager.getChannels().isEmpty());
      Assert.assertEquals(0, ChannelManager.getConnectionNum(channel.getInetAddress()));
      Assert.assertEquals(0, pool.getPassivePeersCount().get());
      Assert.assertEquals(0, pool.getActivePeersCount().get());
      Assert.assertEquals(i + 1, disconnected.get());
      Assert.assertNotNull(ChannelManager.getBannedNodes().getIfPresent(channel.getInetAddress()));
      // Clear the normal disconnect ban to repeat the lifecycle with one loopback source.
      ChannelManager.getBannedNodes().invalidateAll();
    }
    Assert.assertEquals(100, connected.get());
  }

  @Test
  public void registeredPeerRejectsStatusBeforeHandshakeCompletion() throws Exception {
    Socket socket = handshake();
    Channel channel = inbound(socket);
    channel.setFinishHandshake(false);
    write(socket, status());
    Assert.assertEquals(-1, socket.getInputStream().read());
    awaitClosed(channel);
    Assert.assertFalse(channel.isDiscoveryMode());
    Assert.assertTrue(ChannelManager.getChannels().isEmpty());
    Assert.assertEquals(1, disconnected.get());
  }

  @Test
  public void registeredPeerCleanupSurvivesModeChange() throws Exception {
    Socket socket = handshake();
    Channel channel = inbound(socket);
    channel.getCtx().executor().submit(() -> {
      channel.setDiscoveryMode(true);
      channel.getCtx().close();
    }).sync();
    awaitClosed(channel);
    Assert.assertTrue(ChannelManager.getChannels().isEmpty());
    Assert.assertTrue(channel.getDisconnectTime() > 0);
    Assert.assertEquals(0, pool.getPassivePeersCount().get());
    Assert.assertEquals(1, disconnected.get());
  }

  @Test
  public void concurrentCloseAndNotificationAreIdempotent() throws Exception {
    Channel channel = inbound(handshake());
    CountDownLatch start = new CountDownLatch(1);
    CompletableFuture<Void> notify = CompletableFuture.runAsync(() -> {
      awaitStart(start);
      ChannelManager.notifyDisconnect(channel);
    });
    CompletableFuture<Void> close = CompletableFuture.runAsync(() -> {
      awaitStart(start);
      channel.close();
    });
    start.countDown();
    CompletableFuture.allOf(notify, close).get(5, TimeUnit.SECONDS);
    awaitClosed(channel);
    ChannelManager.notifyDisconnect(channel);
    Assert.assertEquals(1, disconnected.get());
    Assert.assertEquals(0, pool.getPassivePeersCount().get());
    Assert.assertTrue(ChannelManager.getChannels().isEmpty());
  }

  @Test
  public void oldChannelCannotRemoveReplacementAtSameAddress() throws Exception {
    Channel old = inbound(handshake());
    Channel replacement = new Channel();
    set(Channel.class, replacement, "inetSocketAddress", old.getInetSocketAddress());
    Assert.assertEquals(old, replacement); // equals() is not an ownership check.
    ChannelManager.getChannels().put(old.getInetSocketAddress(), replacement);
    old.close();
    awaitClosed(old);
    Assert.assertSame(replacement, ChannelManager.getChannels().get(old.getInetSocketAddress()));
    Assert.assertEquals(1, disconnected.get());
  }

  @Test
  public void outboundDetectSuccessKeepsResult() throws Exception {
    NodeStat stat = probe((InetSocketAddress) listener.localAddress(), false);
    Assert.assertSame(stat, nodeStats().get(stat.getSocketAddress()));
    Assert.assertNotNull(stat.getStatusMessage());
    Assert.assertTrue(stat.finishDetect());
    Assert.assertTrue(NodeDetectService.getBadNodesCache().asMap().isEmpty());
  }

  @Test
  public void outboundDetectTimeoutRemovesResult() throws Exception {
    NodeStat stat = probe((InetSocketAddress) listener.localAddress(), true);
    Assert.assertFalse(nodeStats().containsKey(stat.getSocketAddress()));
    Assert.assertNotNull(NodeDetectService.getBadNodesCache()
        .getIfPresent(stat.getSocketAddress().getAddress()));
  }

  @Test
  public void outboundDetectFailureRemovesResult() throws Exception {
    io.netty.channel.Channel rejectingServer = new ServerBootstrap().group(group)
        .channel(NioServerSocketChannel.class).childHandler(new ChannelInboundHandlerAdapter() {
          @Override
          public void channelActive(ChannelHandlerContext ctx) {
            ctx.close();
          }
        }).bind("127.0.0.1", 0).sync().channel();
    try {
      NodeStat stat = probe((InetSocketAddress) rejectingServer.localAddress(), false);
      Assert.assertFalse(nodeStats().containsKey(stat.getSocketAddress()));
      Assert.assertNotNull(NodeDetectService.getBadNodesCache()
          .getIfPresent(stat.getSocketAddress().getAddress()));
    } finally {
      rejectingServer.close().sync();
    }
  }

  private NodeStat probe(InetSocketAddress address, boolean expired) throws Exception {
    NodeStat stat = new NodeStat(new Node(address));
    stat.setLastDetectTime(System.currentTimeMillis() - (expired ? 3000 : 0));
    nodeStats().put(address, stat);
    io.netty.channel.Channel socket = new Bootstrap().group(group).channel(NioSocketChannel.class)
        .handler(initializer(stat.getNode().getHexId(), true)).connect(address).sync().channel();
    socket.eventLoop().submit(() -> { }).sync(); // Wait for channelActive after connect succeeds.
    Channel channel = accepted.get(socket);
    awaitClosed(channel);
    Assert.assertTrue(channel.isActive());
    Assert.assertTrue(channel.isDiscoveryMode());
    Assert.assertFalse(channel.isRegisteredPeer());
    Assert.assertTrue(ChannelManager.getChannels().isEmpty());
    Assert.assertEquals(0, connected.get());
    Assert.assertEquals(0, disconnected.get());
    return stat;
  }

  @SuppressWarnings("unchecked")
  private Map<InetSocketAddress, NodeStat> nodeStats() throws Exception {
    Field field = NodeDetectService.class.getDeclaredField("nodeStatMap");
    field.setAccessible(true);
    return (Map<InetSocketAddress, NodeStat>) field.get(detect);
  }

  private P2pChannelInitializer initializer(String remoteId, boolean discovery) {
    return new P2pChannelInitializer(remoteId, discovery, false) {
      @Override
      public void initChannel(NioSocketChannel socket) {
        super.initChannel(socket);
        try {
          Field field = MessageHandler.class.getDeclaredField("channel");
          field.setAccessible(true);
          accepted.put(socket, (Channel) field.get(socket.pipeline().get("messageHandler")));
        } catch (Exception e) {
          throw new AssertionError(e);
        }
      }
    };
  }

  private Socket handshake() throws Exception {
    int expected = connected.get() + 1;
    Socket socket = open();
    HelloMessage hello = new HelloMessage(Connect.HelloMessage.newBuilder().setFrom(endpoint())
        .setNetworkId(Parameter.p2pConfig.getNetworkId()).setVersion(0)
        .setTimestamp(System.currentTimeMillis()).build().toByteArray());
    write(socket, hello);
    Assert.assertEquals(DisconnectCode.NORMAL.getValue().intValue(),
        ((HelloMessage) read(socket)).getCode());
    await(() -> connected.get() == expected);
    Assert.assertTrue(inbound(socket).isFinishHandshake());
    return socket;
  }

  private Socket open() throws Exception {
    Socket socket = new Socket();
    clients.add(socket);
    socket.setSoTimeout(3000);
    socket.connect(listener.localAddress());
    return socket;
  }

  private Channel inbound(Socket socket) {
    return accepted.values().stream()
        .filter(channel -> channel.getInetSocketAddress() != null
            && channel.getInetSocketAddress().getPort() == socket.getLocalPort())
        .findFirst().orElseThrow(() -> new AssertionError("Missing inbound channel"));
  }

  private Discover.Endpoint endpoint() {
    return Discover.Endpoint.newBuilder().setNodeId(ByteString.copyFrom(NetUtil.getNodeId()))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1")).setPort(18888).build();
  }

  private StatusMessage status() throws Exception {
    return new StatusMessage(Connect.StatusMessage.newBuilder().setFrom(endpoint())
        .setNetworkId(Parameter.p2pConfig.getNetworkId()).setMaxConnections(50)
        .setTimestamp(System.currentTimeMillis()).build().toByteArray());
  }

  private void write(Socket socket, Message message) throws Exception {
    byte[] data = message.getSendData();
    OutputStream out = socket.getOutputStream();
    int length = data.length;
    while ((length & ~127) != 0) {
      out.write((length & 127) | 128);
      length >>>= 7;
    }
    out.write(length);
    out.write(data);
    out.flush();
  }

  private Message read(Socket socket) throws Exception {
    DataInputStream in = new DataInputStream(socket.getInputStream());
    int length = 0;
    for (int shift = 0; shift < 35; shift += 7) {
      int value = in.readUnsignedByte();
      length |= (value & 127) << shift;
      if ((value & 128) == 0) {
        byte[] data = new byte[length];
        in.readFully(data);
        return Message.parse(data);
      }
    }
    throw new AssertionError("Invalid frame");
  }

  private void awaitClosed(Channel channel) throws Exception {
    Assert.assertTrue(channel.getCtx().channel().closeFuture().await(3, TimeUnit.SECONDS));
    channel.getCtx().executor().submit(() -> { }).sync();
    Assert.assertTrue(channel.isDisconnect());
  }

  private void await(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
    Assert.assertTrue(condition.getAsBoolean());
  }

  private void awaitStart(CountDownLatch start) {
    try {
      Assert.assertTrue(start.await(3, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }

  private static void set(Class<?> type, Object target, String name, Object value)
      throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
