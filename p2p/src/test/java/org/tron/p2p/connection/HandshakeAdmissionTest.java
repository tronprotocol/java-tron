package org.tron.p2p.connection;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Connect.DisconnectReason;
import org.tron.p2p.protos.Discover;

public class HandshakeAdmissionTest {

  private static final int NETWORK_ID = 11111;
  private static final byte[] APPLICATION_MESSAGE = {1, 2, 3};
  private static P2pConfig config;

  private P2pConfig previousConfig;
  private int previousVersion;
  private List<P2pEventHandler> previousHandlers;
  private Map<Byte, P2pEventHandler> previousHandlerMap;
  private Map<InetAddress, Long> previousBans;
  private HandshakeService previousHandshakeService;
  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private final List<String> events = new ArrayList<>();

  @BeforeClass
  public static void createConfig() {
    config = new P2pConfig();
    config.setNetworkId(NETWORK_ID);
    config.setNodeID(new byte[64]);
    config.setIp("127.0.0.1");
    config.setIpv6("");
  }

  @Before
  public void setUp() throws Exception {
    previousConfig = Parameter.p2pConfig;
    previousVersion = Parameter.version;
    previousHandlers = Parameter.handlerList;
    previousHandlerMap = Parameter.handlerMap;
    previousBans = new HashMap<>(ChannelManager.getBannedNodes().asMap());
    previousHandshakeService = ChannelManager.getHandshakeService();
    Parameter.p2pConfig = config;
    Parameter.version = 1;
    config.setMaxConnections(50);
    config.setMaxConnectionsWithSameIp(2);
    config.getTrustNodes().clear();
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new HashMap<>();
    P2pEventHandler handler = new P2pEventHandler() {
      @Override
      public void onConnect(Channel channel) {
        Assert.assertTrue(channel.isFinishHandshake());
        Assert.assertEquals(NETWORK_ID, channel.getHelloMessage().getNetworkId());
        Assert.assertSame(channel,
            ChannelManager.getChannels().get(channel.getInetSocketAddress()));
        events.add("connect");
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
        Assert.assertArrayEquals(APPLICATION_MESSAGE, data);
        events.add("message");
      }
    };
    Parameter.handlerList.add(handler);
    Parameter.handlerMap.put(APPLICATION_MESSAGE[0], handler);
    setHandshakeService(new HandshakeService());
  }

  @After
  public void tearDown() throws Exception {
    try {
      for (EmbeddedChannel socket : sockets) {
        socket.finishAndReleaseAll();
      }
    } finally {
      ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
      Parameter.p2pConfig = previousConfig;
      Parameter.version = previousVersion;
      Parameter.handlerList = previousHandlers;
      Parameter.handlerMap = previousHandlerMap;
      ChannelManager.getBannedNodes().invalidateAll();
      ChannelManager.getBannedNodes().putAll(previousBans);
      setHandshakeService(previousHandshakeService);
    }
  }

  @Test
  public void applicationMessageBeforeHelloIsRejected() throws Exception {
    TestChannel channel = newChannel(false, 10001);
    receive(channel, APPLICATION_MESSAGE);

    assertRejected(channel);
    Assert.assertNull(channel.getHelloMessage());
    Message reply = Message.parse(readOutbound(channel));
    Assert.assertEquals(MessageType.DISCONNECT, reply.getType());
    Assert.assertEquals(DisconnectReason.BAD_PROTOCOL,
        Connect.P2pDisconnectMessage.parseFrom(reply.getData()).getReason());
  }

  @Test
  public void outboundApplicationMessageBeforeHelloIsRejected() throws Exception {
    TestChannel channel = newChannel(true, 10001);
    receive(channel, APPLICATION_MESSAGE);

    assertRejected(channel);
  }

  @Test
  public void trustedPeerStillNeedsHello() throws Exception {
    config.getTrustNodes().add(InetAddress.getByName("127.0.0.1"));
    TestChannel channel = newChannel(false, 10001);
    Assert.assertTrue(channel.isTrustPeer());
    receive(channel, APPLICATION_MESSAGE);

    assertRejected(channel);
  }

  @Test
  public void inboundWrongNetworkIsNeverRegistered() throws Exception {
    TestChannel channel = newChannel(false, 10001);
    receive(channel, hello(NETWORK_ID + 1, 1, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
    Assert.assertNull(channel.getHelloMessage());
    HelloMessage reply = (HelloMessage) Message.parse(readOutbound(channel));
    Assert.assertEquals(DisconnectCode.DIFFERENT_VERSION.getValue().intValue(), reply.getCode());
  }

  @Test
  public void outboundWrongNetworkIsNeverRegistered() throws Exception {
    TestChannel channel = newChannel(true, 10001);
    receive(channel, hello(NETWORK_ID + 1, 1, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
  }

  @Test
  public void protocolVersionCannotSubstituteForNetworkId() throws Exception {
    TestChannel channel = newChannel(true, 10001);
    receive(channel, hello(NETWORK_ID + 1, NETWORK_ID, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
  }

  @Test
  public void outboundRejectedHelloIsNeverRegistered() throws Exception {
    TestChannel channel = newChannel(true, 10001);
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.TOO_MANY_PEERS, 1));

    assertRejected(channel);
  }

  @Test
  public void closingDuringHelloReplyDoesNotRegisterPeer() throws Exception {
    TestChannel channel = newChannel(false, 10001);
    channel.closeOnHelloReply = true;
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
    Assert.assertEquals(1, channel.helloSendAttempts);
  }

  @Test
  public void closedChannelCannotCompleteHandshake() throws Exception {
    TestChannel channel = newChannel(true, 10001);
    channel.close();
    ChannelManager.processMessage(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
  }

  @Test
  public void inboundHelloCompletesBeforeRegistration() throws Exception {
    TestChannel channel = newChannel(false, 10001);
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));

    assertAdmitted(channel);
    // Hello is a plain protocol frame even when subsequent messages use compression.
    HelloMessage reply = (HelloMessage) Message.parse(readOutbound(channel));
    Assert.assertEquals(NETWORK_ID, reply.getNetworkId());
    Assert.assertEquals(DisconnectCode.NORMAL.getValue().intValue(), reply.getCode());
    receive(channel, UpgradeController.codeSendData(1, APPLICATION_MESSAGE));
    Assert.assertEquals(Arrays.asList("connect", "message"), events);
  }

  @Test
  public void outboundHelloCompletesBeforeRegistration() throws Exception {
    TestChannel channel = newChannel(true, 10001);
    ChannelManager.getHandshakeService().startHandshake(channel);
    Assert.assertEquals(MessageType.HANDSHAKE_HELLO,
        Message.parse(readOutbound(channel)).getType());
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));

    assertAdmitted(channel);
    receive(channel, UpgradeController.codeSendData(1, APPLICATION_MESSAGE));
    Assert.assertEquals(Arrays.asList("connect", "message"), events);
  }

  @Test
  public void connectionLimitRejectsWithoutCompletingHandshake() throws Exception {
    config.setMaxConnections(0);
    TestChannel channel = newChannel(false, 10001);
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
    HelloMessage reply = (HelloMessage) Message.parse(readOutbound(channel));
    Assert.assertEquals(DisconnectCode.TOO_MANY_PEERS.getValue().intValue(), reply.getCode());
  }

  @Test
  public void sameIpLimitRejectsWithoutCompletingHandshake() throws Exception {
    config.setMaxConnectionsWithSameIp(0);
    TestChannel channel = newChannel(false, 10001);
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));

    assertRejected(channel);
    HelloMessage reply = (HelloMessage) Message.parse(readOutbound(channel));
    Assert.assertEquals(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP.getValue().intValue(),
        reply.getCode());
  }

  @Test
  public void selfHelloIsRejectedBeforeRegistration() throws Exception {
    TestChannel channel = newChannel(false, 10001);
    receive(channel, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 0));

    assertRejected(channel);
    HelloMessage reply = (HelloMessage) Message.parse(readOutbound(channel));
    Assert.assertEquals(DisconnectCode.DUPLICATE_PEER.getValue().intValue(), reply.getCode());
  }

  @Test
  public void wrongNetworkCannotEvictAnExistingPeer() throws Exception {
    TestChannel unverified = newChannel(false, 10001);
    TestChannel existing = newChannel(false, 10002);
    // Force the unverified connection to be older, so a premature checkPeer would evict existing.
    Field startTime = Channel.class.getDeclaredField("startTime");
    startTime.setAccessible(true);
    startTime.set(unverified, existing.getStartTime() - 1);
    receive(existing, hello(NETWORK_ID, 1, DisconnectCode.NORMAL, 1));
    Assert.assertTrue(existing.socket.isOpen());
    Assert.assertTrue(existing.isFinishHandshake());
    Assert.assertSame(existing,
        ChannelManager.getChannels().get(existing.getInetSocketAddress()));
    events.clear();

    receive(unverified, hello(NETWORK_ID + 1, 1, DisconnectCode.NORMAL, 1));

    assertRejected(unverified);
    Assert.assertTrue(existing.socket.isOpen());
    Assert.assertFalse(existing.isDisconnect());
    Assert.assertEquals(1, ChannelManager.getChannels().size());
    Assert.assertSame(existing,
        ChannelManager.getChannels().get(existing.getInetSocketAddress()));
  }

  private void assertRejected(TestChannel channel) {
    Assert.assertFalse(channel.socket.isOpen());
    Assert.assertTrue(channel.isDisconnect());
    Assert.assertFalse(channel.isFinishHandshake());
    Assert.assertFalse(channel.isRegisteredPeer());
    Assert.assertEquals(0, channel.handshakeCompletions);
    // No close listener removes peers in this fixture: this also catches transient registration.
    Assert.assertFalse(ChannelManager.getChannels().containsKey(channel.getInetSocketAddress()));
    Assert.assertTrue(events.isEmpty());
  }

  private void assertAdmitted(TestChannel channel) {
    Assert.assertTrue(channel.socket.isOpen());
    Assert.assertTrue(channel.isFinishHandshake());
    Assert.assertTrue(channel.isRegisteredPeer());
    Assert.assertEquals(1, channel.handshakeCompletions);
    Assert.assertFalse(channel.registeredBeforeHandshake);
    Assert.assertSame(channel,
        ChannelManager.getChannels().get(channel.getInetSocketAddress()));
    Assert.assertEquals(Arrays.asList("connect"), events);
  }

  private TestChannel newChannel(boolean active, int port) {
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
    EmbeddedChannel socket = new EmbeddedChannel() {
      @Override
      protected SocketAddress remoteAddress0() {
        return address;
      }
    };
    sockets.add(socket);
    TestChannel channel = new TestChannel(socket);
    channel.init(socket.pipeline(), active ? "remote" : "", false);
    channel.setChannelHandlerContext(socket.pipeline().context("messageHandler"));
    return channel;
  }

  private byte[] hello(int networkId, int version, DisconnectCode code, int nodeId)
      throws Exception {
    byte[] id = new byte[64];
    id[0] = (byte) nodeId;
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(id))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888)
        .build();
    return new HelloMessage(Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(networkId)
        .setVersion(version)
        .setCode(code.getValue())
        .setTimestamp(System.currentTimeMillis())
        .build().toByteArray()).getSendData();
  }

  private void receive(TestChannel channel, byte[] data) {
    ByteBuf frame = Unpooled.buffer();
    int length = data.length;
    while ((length & ~0x7f) != 0) {
      frame.writeByte((length & 0x7f) | 0x80);
      length >>>= 7;
    }
    frame.writeByte(length);
    frame.writeBytes(data);
    channel.socket.writeInbound(frame);
    channel.socket.checkException();
  }

  private byte[] readOutbound(TestChannel channel) throws Exception {
    ByteBuf bytes = Unpooled.buffer();
    try {
      ByteBuf part;
      while ((part = channel.socket.readOutbound()) != null) {
        try {
          bytes.writeBytes(part);
        } finally {
          part.release();
        }
      }
      Assert.assertTrue("Expected an outbound protocol frame", bytes.isReadable());
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

  private void setHandshakeService(HandshakeService service) throws Exception {
    Field field = ChannelManager.class.getDeclaredField("handshakeService");
    field.setAccessible(true);
    field.set(null, service);
  }

  private static class TestChannel extends Channel {
    private final EmbeddedChannel socket;
    private int handshakeCompletions;
    private int helloSendAttempts;
    private boolean registeredBeforeHandshake;
    private boolean closeOnHelloReply;

    private TestChannel(EmbeddedChannel socket) {
      this.socket = socket;
    }

    @Override
    public void send(Message message) {
      if (message.getType() == MessageType.HANDSHAKE_HELLO) {
        helloSendAttempts++;
        if (closeOnHelloReply) {
          close();
          return;
        }
      }
      super.send(message);
    }

    @Override
    public void setFinishHandshake(boolean finishHandshake) {
      if (finishHandshake) {
        handshakeCompletions++;
        registeredBeforeHandshake =
            ChannelManager.getChannels().containsKey(getInetSocketAddress());
      }
      super.setFinishHandshake(finishHandshake);
    }
  }
}
