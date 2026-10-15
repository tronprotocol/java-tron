package org.tron.p2p.connection.socket;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.keepalive.KeepAliveService;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class PendingInboundConnectionHandlerTest {

  private static final String REMOTE_IP = "192.0.2.1";
  private static P2pConfig config;

  private final List<EmbeddedChannel> sockets = Collections.synchronizedList(new ArrayList<>());
  private final List<NioSocketChannel> nioSockets = new ArrayList<>();
  private NioEventLoopGroup nioGroup;
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private Map<InetAddress, Long> previousBans;
  private Object previousHandshakeService;
  private Object previousKeepAliveService;
  private Object previousNodeDetectService;
  private int connectCallbacks;

  @BeforeClass
  public static void createConfig() {
    config = new P2pConfig();
    config.setIp("127.0.0.1");
    config.setIpv6("");
    config.setNodeID(new byte[64]);
  }

  @Before
  public void setUp() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    Parameter.p2pConfig = config;
    config.setMaxConnections(50);
    config.setMaxConnectionsWithSameIp(2);
    config.getTrustNodes().clear();
    Parameter.handlerList = new ArrayList<>();
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    ChannelManager.getBannedNodes().invalidateAll();
    previousHandshakeService = setService("handshakeService", new HandshakeService());
    previousKeepAliveService = setService("keepAliveService", new KeepAliveService());
    previousNodeDetectService = setService("nodeDetectService", new NodeDetectService());
    Parameter.handlerList.add(new P2pEventHandler() {
      @Override
      public void onConnect(Channel channel) {
        Assert.assertTrue(channel.isFinishHandshake());
        Assert.assertNotNull(channel.getHelloMessage());
        Assert.assertNull(channel.getCtx().pipeline().get(PendingInboundConnectionHandler.class));
        connectCallbacks++;
      }
    });
  }

  @After
  public void tearDown() throws Exception {
    try {
      for (EmbeddedChannel socket : sockets) {
        socket.finishAndReleaseAll();
      }
      for (NioSocketChannel socket : nioSockets) {
        socket.close().syncUninterruptibly();
      }
      if (nioGroup != null) {
        nioGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
      }
      Field count = PendingInboundConnectionHandler.class.getDeclaredField("pendingConnections");
      count.setAccessible(true);
      Assert.assertEquals("Pending reservations leaked", 0, count.getInt(null));
      Field counts = PendingInboundConnectionHandler.class.getDeclaredField("pendingByIp");
      counts.setAccessible(true);
      Assert.assertTrue("Per-IP reservations leaked", ((Map<?, ?>) counts.get(null)).isEmpty());
    } finally {
      ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
      Parameter.p2pConfig = previousConfig;
      Parameter.handlerList = previousHandlers;
      ChannelManager.getBannedNodes().invalidateAll();
      ChannelManager.getBannedNodes().putAll(previousBans);
      setService("handshakeService", previousHandshakeService);
      ((KeepAliveService) setService("keepAliveService", previousKeepAliveService)).close();
      ((NodeDetectService) setService("nodeDetectService", previousNodeDetectService)).close();
    }
  }

  @Test
  public void globalPendingLimitIsIndependentOfPeerLimit() {
    config.setMaxConnections(0);
    for (int i = 0; i < Parameter.MAX_PENDING_INBOUND_CONNECTIONS; i++) {
      Assert.assertTrue(inbound("192.0.2." + (i + 1)).isOpen());
    }
    Assert.assertFalse(inbound("198.51.100.1").isOpen());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
    Assert.assertEquals(0, connectCallbacks);
  }

  @Test
  public void sameIpLimitCountsSilentConnections() {
    for (int i = 0; i < Parameter.MAX_PENDING_INBOUND_CONNECTIONS_WITH_SAME_IP; i++) {
      Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    }
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound("192.0.2.2").isOpen());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void ipv6ConnectionsShareTheSameIpQuota() {
    Assert.assertTrue(inbound("2001:db8::1").isOpen());
    Assert.assertTrue(inbound("2001:db8:0:0:0:0:0:1").isOpen());
    Assert.assertFalse(inbound("2001:db8::1").isOpen());
    Assert.assertTrue(inbound("2001:db8::2").isOpen());
  }

  @Test
  public void trustedInboundConnectionsAlsoConsumePendingQuota() throws Exception {
    config.getTrustNodes().add(InetAddress.getByName(REMOTE_IP));
    EmbeddedChannel socket = inbound(REMOTE_IP);
    Assert.assertTrue(initializeProtocol(socket).isTrustPeer());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
  }

  @Test(timeout = 20000)
  public void concurrentReservationsRespectGlobalLimit() throws Exception {
    assertConcurrentLimit(false, Parameter.MAX_PENDING_INBOUND_CONNECTIONS);
  }

  @Test(timeout = 20000)
  public void concurrentReservationsRespectSameIpLimit() throws Exception {
    assertConcurrentLimit(true, Parameter.MAX_PENDING_INBOUND_CONNECTIONS_WITH_SAME_IP);
  }

  @Test
  public void closeReleasesReservationExactlyOnce() {
    EmbeddedChannel first = inbound(REMOTE_IP);
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    first.close();
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    first.close();
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void silentConnectionExpiresAtAbsoluteDeadline() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS - 1);
    Assert.assertTrue(socket.isOpen());
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void repeatedActivationDoesNotRestartDeadline() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    advance(socket, 5);
    socket.pipeline().fireChannelActive();
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS - 5);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void pingTrafficCannotExtendHandshakeDeadline() throws Exception {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    Channel channel = initializeProtocol(socket);
    for (int i = 0; i < 3; i++) {
      advance(socket, 3);
      receive(socket, new PingMessage().getSendData());
      Assert.assertEquals(MessageType.KEEP_ALIVE_PONG,
          Message.parse(readOutbound(socket)).getType());
      Assert.assertTrue(socket.isOpen());
      Assert.assertFalse(channel.isFinishHandshake());
    }
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
    Assert.assertEquals(0, connectCallbacks);
  }

  @Test
  public void pongTrafficCannotExtendHandshakeDeadline() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    advance(socket, 9);
    receive(socket, new PongMessage().getSendData());
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void partialFrameTrafficCannotExtendHandshakeDeadline() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    // A valid length prefix for 1024 bytes, with an incomplete payload.
    ByteBuf partial = Unpooled.wrappedBuffer(new byte[]{(byte) 0x80, 8, 1});
    socket.writeInbound(partial);
    for (int i = 0; i < 3; i++) {
      advance(socket, 3);
      socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{1}));
      Assert.assertTrue(socket.isOpen());
    }
    advance(socket, 1);
    Assert.assertFalse(socket.isOpen());
    Assert.assertEquals(0, partial.refCnt());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
    Assert.assertEquals(0, connectCallbacks);
  }

  @Test
  public void incompleteLengthPrefixStillExpires() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{(byte) 0x80}));
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void handshakeFlagAloneDoesNotReleaseReservation() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket).setFinishHandshake(true);
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS);
    Assert.assertFalse(socket.isOpen());
  }

  @Test
  public void lateHelloCannotWinAgainstDelayedTimeoutTask() throws Exception {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    // Advance the clock without running scheduled tasks: the HELLO is handled first.
    socket.advanceTimeBy(Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    receive(socket, hello(config.getNetworkId()));
    Assert.assertFalse(socket.isOpen());
    Assert.assertEquals(0, connectCallbacks);
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void handshakeProcessingCannotCompletePastDeadline() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    socket.advanceTimeBy(Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    Assert.assertFalse(PendingInboundConnectionHandler.handshakeCompleted(socket));
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void helloReplyCrossingDeadlineDoesNotRegisterPeer() throws Exception {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    Channel channel = initializeProtocol(socket);
    int[] replies = {0};
    socket.pipeline().addFirst("delayHelloReply", new ChannelOutboundHandlerAdapter() {
      @Override
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        replies[0]++;
        // Let the deadline pass during the reply without running the scheduled timeout task.
        socket.advanceTimeBy(Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ctx.write(msg, promise);
      }
    });

    receive(socket, hello(config.getNetworkId()));

    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(channel.isDisconnect());
    Assert.assertFalse(channel.isFinishHandshake());
    Assert.assertEquals(1, replies[0]);
    Assert.assertEquals(0, connectCallbacks);
    Assert.assertTrue(ChannelManager.getChannels().isEmpty());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void validHelloReleasesReservationAndCancelsDeadline() throws Exception {
    config.setMaxConnections(1);
    EmbeddedChannel socket = inbound(REMOTE_IP);
    Channel channel = initializeProtocol(socket);
    EmbeddedChannel waiting = inbound(REMOTE_IP);
    receive(socket, hello(config.getNetworkId()));

    Assert.assertTrue(channel.isFinishHandshake());
    Assert.assertEquals(1, connectCallbacks);
    Assert.assertEquals(1, ChannelManager.getChannelCount());
    Assert.assertEquals(MessageType.HANDSHAKE_HELLO, Message.parse(readOutbound(socket)).getType());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
    advance(socket, Parameter.HANDSHAKE_TIMEOUT_SECONDS + 1);
    Assert.assertTrue(socket.isOpen());
    socket.close();
    // Closing an already promoted peer must not release another pending connection's slot.
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
    waiting.close();
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void wrongNetworkHelloClosesAndReleasesReservation() throws Exception {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    receive(socket, hello(config.getNetworkId() + 1));
    Assert.assertFalse(socket.isOpen());
    Assert.assertEquals(0, connectCallbacks);
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void peerCapacityRejectionReleasesReservation() throws Exception {
    config.setMaxConnections(0);
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    receive(socket, hello(config.getNetworkId()));
    Assert.assertFalse(socket.isOpen());
    Assert.assertEquals(0, connectCallbacks);
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void malformedFrameClosesAndReleasesReservation() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    initializeProtocol(socket);
    socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{
        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80}));
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void inboundDiscoveryClosesAndReleasesReservation() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    Channel channel = initializeProtocol(socket);
    receive(socket, new StatusMessage().getSendData());
    Assert.assertTrue(channel.isDiscoveryMode());
    Assert.assertFalse(socket.isOpen());
    Assert.assertEquals(0, connectCallbacks);
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test
  public void removingUnfinishedGuardClosesConnection() {
    EmbeddedChannel socket = inbound(REMOTE_IP);
    socket.pipeline().remove(PendingInboundConnectionHandler.class);
    Assert.assertFalse(socket.isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test(timeout = 15000)
  public void initializerRejectsBeforeInstallingProtocolHandlers() throws Exception {
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    NioSocketChannel socket = nioSocket();
    socket.eventLoop().submit(() ->
        new P2pChannelInitializer("", false, false).initChannel(socket)).sync();

    Assert.assertTrue(socket.closeFuture().await(5, TimeUnit.SECONDS));
    Assert.assertNull(socket.pipeline().get(MessageHandler.class));
    Assert.assertNull(socket.pipeline().get(P2pProtobufVarint32FrameDecoder.class));
    Assert.assertEquals(0, connectCallbacks);
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test(timeout = 15000)
  public void initializationFailureReleasesReservation() throws Exception {
    NioSocketChannel socket = nioSocket();
    socket.eventLoop().submit(() -> {
      socket.pipeline().addLast("pendingInboundHandshake", new ChannelInboundHandlerAdapter());
      new P2pChannelInitializer("", false, false).initChannel(socket);
    }).sync();

    Assert.assertTrue(socket.closeFuture().await(5, TimeUnit.SECONDS));
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
  }

  @Test(timeout = 15000)
  public void outboundAndDiscoveryInitializationDoNotConsumeInboundQuota() throws Exception {
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    Assert.assertTrue(inbound(REMOTE_IP).isOpen());
    for (boolean discovery : new boolean[]{false, true}) {
      NioSocketChannel socket = nioSocket();
      socket.eventLoop().submit(() ->
          new P2pChannelInitializer("outbound", discovery, false).initChannel(socket)).sync();
      Assert.assertTrue(socket.isOpen());
      Assert.assertNull(socket.pipeline().get(PendingInboundConnectionHandler.class));
      Assert.assertNotNull(socket.pipeline().get(MessageHandler.class));
    }
    Assert.assertFalse(inbound(REMOTE_IP).isOpen());
  }

  @Test(timeout = 20000)
  public void realTcpConnectionsAreLimitedBeforeSendingAnyBytes() throws Exception {
    nioGroup = new NioEventLoopGroup(1);
    BlockingQueue<NioSocketChannel> accepted = new LinkedBlockingQueue<>();
    List<Socket> clients = new ArrayList<>();
    io.netty.channel.Channel server = new ServerBootstrap()
        .group(nioGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<NioSocketChannel>() {
          @Override
          protected void initChannel(NioSocketChannel socket) {
            new P2pChannelInitializer("", false, false).initChannel(socket);
            accepted.add(socket);
          }
        })
        .bind("127.0.0.1", 0).sync().channel();
    try {
      NioSocketChannel first = connectClient(server, clients, accepted);
      NioSocketChannel second = connectClient(server, clients, accepted);
      NioSocketChannel rejected = connectClient(server, clients, accepted);
      Assert.assertTrue(first.isActive());
      Assert.assertTrue(second.isActive());
      Assert.assertNotNull(first.pipeline().get(PendingInboundConnectionHandler.class));
      Assert.assertTrue(rejected.closeFuture().await(5, TimeUnit.SECONDS));
      Assert.assertNull(rejected.pipeline().get(MessageHandler.class));
      Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
      Assert.assertEquals(0, connectCallbacks);

      clients.get(0).close();
      Assert.assertTrue(first.closeFuture().await(5, TimeUnit.SECONDS));
      NioSocketChannel replacement = connectClient(server, clients, accepted);
      Assert.assertTrue(replacement.isActive());
      Assert.assertNotNull(replacement.pipeline().get(PendingInboundConnectionHandler.class));
    } finally {
      for (Socket client : clients) {
        client.close();
      }
      server.close().syncUninterruptibly();
    }
  }

  private NioSocketChannel connectClient(io.netty.channel.Channel server, List<Socket> clients,
      BlockingQueue<NioSocketChannel> accepted) throws Exception {
    Socket client = new Socket();
    clients.add(client);
    client.connect(server.localAddress(), 3000);
    NioSocketChannel socket = accepted.poll(5, TimeUnit.SECONDS);
    Assert.assertNotNull("Server did not initialize the connection", socket);
    nioSockets.add(socket);
    // Wait until registration and channelActive have finished on the server event loop.
    socket.eventLoop().submit(() -> { }).sync();
    return socket;
  }

  private EmbeddedChannel inbound(String ip) {
    InetSocketAddress address = new InetSocketAddress(ip, 18888);
    EmbeddedChannel socket = new EmbeddedChannel() {
      @Override
      protected SocketAddress remoteAddress0() {
        return address;
      }
    };
    sockets.add(socket);
    socket.freezeTime();
    if (!PendingInboundConnectionHandler.tryAdd(socket)) {
      socket.close();
    }
    return socket;
  }

  private Channel initializeProtocol(EmbeddedChannel socket) {
    Channel channel = new Channel();
    channel.init(socket.pipeline(), "", false);
    channel.setChannelHandlerContext(socket.pipeline().context("messageHandler"));
    socket.closeFuture().addListener(future -> {
      channel.markDisconnected();
      ChannelManager.notifyDisconnect(channel);
    });
    return channel;
  }

  private NioSocketChannel nioSocket() throws Exception {
    if (nioGroup == null) {
      nioGroup = new NioEventLoopGroup(1);
    }
    NioSocketChannel socket = new NioSocketChannel() {
      @Override
      protected SocketAddress remoteAddress0() {
        return new InetSocketAddress(REMOTE_IP, 18888);
      }
    };
    nioSockets.add(socket);
    nioGroup.register(socket).sync();
    return socket;
  }

  private void assertConcurrentLimit(boolean sameIp, int expected) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(8,
        BasicThreadFactory.builder().namingPattern("pending-inbound-test-%d").build());
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int i = 0; i < Parameter.MAX_PENDING_INBOUND_CONNECTIONS * 2; i++) {
        String ip = sameIp ? REMOTE_IP : "192.0.2." + (i + 1);
        results.add(executor.submit(() -> {
          Assert.assertTrue(start.await(5, TimeUnit.SECONDS));
          return inbound(ip).isOpen();
        }));
      }
      start.countDown();
      int accepted = 0;
      for (Future<Boolean> result : results) {
        if (result.get(10, TimeUnit.SECONDS)) {
          accepted++;
        }
      }
      Assert.assertEquals(expected, accepted);
    } finally {
      start.countDown();
      executor.shutdown();
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
      Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private void advance(EmbeddedChannel socket, long seconds) {
    socket.advanceTimeBy(seconds, TimeUnit.SECONDS);
    socket.runScheduledPendingTasks();
    socket.runPendingTasks();
    socket.checkException();
  }

  private byte[] hello(int networkId) throws Exception {
    byte[] id = new byte[64];
    id[0] = 1;
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(id))
        .setAddress(ByteString.copyFromUtf8(REMOTE_IP))
        .setPort(18888)
        .build();
    return new HelloMessage(Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(networkId)
        .setVersion(Parameter.version)
        .setCode(DisconnectCode.NORMAL.getValue())
        .setTimestamp(System.currentTimeMillis())
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
      Assert.assertTrue("Expected a protocol reply", bytes.isReadable());
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

  private Object setService(String name, Object service) throws Exception {
    Field field = ChannelManager.class.getDeclaredField(name);
    field.setAccessible(true);
    Object previous = field.get(null);
    field.set(null, service);
    return previous;
  }
}
