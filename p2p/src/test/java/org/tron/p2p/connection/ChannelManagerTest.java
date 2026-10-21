package org.tron.p2p.connection;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.pool.ConnPoolService;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

public class ChannelManagerTest {

  private static final InetSocketAddress REMOTE = new InetSocketAddress("127.0.0.2", 18888);
  private final long startTime = System.currentTimeMillis() - 10000;
  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private P2pConfig previousConfig;
  private List<P2pEventHandler> previousHandlers;
  private ConnPoolService pool;
  private HandshakeService handshake;

  @Before
  public void setUp() {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerList;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setNetworkId(11111);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);
    Parameter.handlerList = new ArrayList<>();
    clearChannels();
    pool = new ConnPoolService();
    handshake = new HandshakeService();
  }

  @After
  public void tearDown() {
    try {
      sockets.forEach(EmbeddedChannel::finishAndReleaseAll);
    } finally {
      try {
        pool.close();
        clearChannels();
        ConnectionPolicy.replaceBlockedIps(Collections.emptySet());
      } finally {
        Parameter.p2pConfig = previousConfig;
        Parameter.handlerList = previousHandlers;
      }
    }
  }

  private void register(Channel channel, InetSocketAddress address) throws Exception {
    Field field = Channel.class.getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(channel, address);
    Assert.assertEquals(DisconnectCode.NORMAL, ChannelManager.processPeer(channel));
  }

  @Test
  public synchronized void testGetConnectionNum() throws Exception {
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.1", 100);
    Field field =  c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());

    Channel c2 = new Channel();
    InetSocketAddress a2 = new InetSocketAddress("100.1.1.2", 100);
    field =  c2.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c2, a2.getAddress());

    Channel c3 = new Channel();
    InetSocketAddress a3 = new InetSocketAddress("100.1.1.2", 99);
    field =  c3.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c3, a3.getAddress());

    int cnt = ChannelManager.getConnectionNum(a1.getAddress());
    Assert.assertTrue(cnt == 0);

    register(c1, a1);
    cnt = ChannelManager.getConnectionNum(a1.getAddress());
    Assert.assertTrue(cnt == 1);

    register(c2, a2);
    cnt = ChannelManager.getConnectionNum(a2.getAddress());
    Assert.assertTrue(cnt == 1);

    register(c3, a3);
    cnt = ChannelManager.getConnectionNum(a3.getAddress());
    Assert.assertTrue(cnt == 2);
  }

  @Test
  public synchronized void testNotifyDisconnect() throws Exception {
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.1", 100);

    Field field =  c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1);

    InetAddress inetAddress = a1.getAddress();
    field =  c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, inetAddress);

    register(c1, a1);

    Long time = ChannelManager.getBannedNodes().getIfPresent(a1.getAddress());
    Assert.assertEquals(1, ChannelManager.getChannelCount());
    Assert.assertTrue(time == null);

    ChannelManager.notifyDisconnect(c1);
    time = ChannelManager.getBannedNodes().getIfPresent(a1.getAddress());
    Assert.assertTrue(time != null);
    Assert.assertEquals(0, ChannelManager.getChannelCount());
  }

  @Test
  public synchronized void testCheckPeer() throws Exception {
    clearChannels();
    Parameter.p2pConfig = new P2pConfig();

    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.2", 100);

    Field field =  c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1);
    field =  c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());

    DisconnectCode code;
    synchronized (ChannelManager.class) {
      code = ChannelManager.checkPeer(c1);
      Assert.assertTrue(code.equals(DisconnectCode.NORMAL));
      Assert.assertTrue(ChannelManager.getChannels().isEmpty());
      ChannelManager.addPeer(c1);
    }

    Thread.sleep(5);

    Parameter.p2pConfig.setMaxConnections(1);

    Channel c2 = new Channel();
    InetSocketAddress a2 = new InetSocketAddress("100.1.1.2", 99);

    field =  c2.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c2, a2);
    field =  c2.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c2, a2.getAddress());

    code = ChannelManager.checkPeer(c2);
    Assert.assertTrue(code.equals(DisconnectCode.TOO_MANY_PEERS));

    Parameter.p2pConfig.setMaxConnections(2);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(1);
    code = ChannelManager.checkPeer(c2);
    Assert.assertTrue(code.equals(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP));

    Parameter.p2pConfig.setMaxConnectionsWithSameIp(2);
    c1.setNodeId("cc");
    c2.setNodeId("cc");
    code = ChannelManager.checkPeer(c2);
    Assert.assertTrue(code.equals(DisconnectCode.DUPLICATE_PEER));
  }

  @Test
  public void processPeerRejectsManuallyBlockedIp() throws Exception {
    Channel channel = channel(true, 1);
    ConnectionPolicy.replaceBlockedIps(Collections.singleton(REMOTE.getAddress()));

    Assert.assertEquals(DisconnectCode.UNKNOWN, ChannelManager.processPeer(channel));
    Assert.assertEquals(0, ChannelManager.getChannelCount());
  }

  @Test
  public void connectRejectsUnresolvedAddressBeforeNetworkAccess() {
    try {
      ChannelManager.connect(InetSocketAddress.createUnresolved("peer.example", 18888));
      Assert.fail("Expected unresolved address to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("must be resolved"));
    }
  }

  @Test
  public void disconnectUsesEndpointAndBlockedReplacementUsesIp() throws Exception {
    InetSocketAddress firstAddress = new InetSocketAddress("192.0.2.31", 18888);
    InetSocketAddress secondAddress = new InetSocketAddress("192.0.2.31", 18889);
    InetSocketAddress otherAddress = new InetSocketAddress("192.0.2.32", 18888);
    Channel first = channel(true, 1);
    Channel second = channel(true, 2);
    Channel other = channel(true, 3);
    set(first, "inetSocketAddress", firstAddress);
    set(first, "inetAddress", firstAddress.getAddress());
    set(second, "inetSocketAddress", secondAddress);
    set(second, "inetAddress", secondAddress.getAddress());
    set(other, "inetSocketAddress", otherAddress);
    set(other, "inetAddress", otherAddress.getAddress());
    ChannelManager.addPeer(first);
    ChannelManager.addPeer(second);
    ChannelManager.addPeer(other);

    Assert.assertEquals(1, ChannelManager.disconnect(firstAddress));
    Assert.assertTrue(first.isDisconnect());
    Assert.assertFalse(second.isDisconnect());
    Assert.assertFalse(other.isDisconnect());
    ByteBuf disconnectData = ((EmbeddedChannel) first.getCtx().channel()).readOutbound();
    Assert.assertNotNull(disconnectData);
    try {
      byte[] encodedMessage = new byte[disconnectData.readableBytes()];
      disconnectData.readBytes(encodedMessage);
      Assert.assertEquals(MessageType.DISCONNECT.getType(), encodedMessage[0]);
      Connect.P2pDisconnectMessage disconnectMessage = Connect.P2pDisconnectMessage.parseFrom(
          ArrayUtils.subarray(encodedMessage, 1, encodedMessage.length));
      Assert.assertEquals(Connect.DisconnectReason.REQUESTED, disconnectMessage.getReason());
    } finally {
      disconnectData.release();
    }

    ConnectionPolicy.replaceBlockedIps(Collections.singleton(secondAddress.getAddress()));
    Assert.assertEquals(1, ChannelManager.disconnectBlockedIps());
    Assert.assertTrue(second.isDisconnect());
    Assert.assertFalse(other.isDisconnect());
    Assert.assertNull(((EmbeddedChannel) second.getCtx().channel()).readOutbound());
  }

  @Test
  public void disconnectClosesAllChannelsAtTheSameEndpoint() throws Exception {
    Channel first = channel(true, 1);
    Channel second = channel(true, 2);
    ChannelManager.addPeer(first);
    ChannelManager.addPeer(second);

    Assert.assertEquals(2, ChannelManager.disconnect(REMOTE));
    Assert.assertTrue(first.isDisconnect());
    Assert.assertTrue(second.isDisconnect());
    Assert.assertEquals(0, ChannelManager.getChannelCount());
  }

  private void clearChannels() {
    ChannelManager.getAllChannels().forEach(ChannelManager::notifyDisconnect);
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @Test
  public synchronized void testDiscoveryModeRejectsHelloMessage() throws Exception {
    clearChannels();
    Parameter.p2pConfig = new P2pConfig();

    Channel channel = new Channel();
    channel.setDiscoveryMode(true);

    InetSocketAddress addr = new InetSocketAddress("100.1.1.5", 18888);
    Field f = channel.getClass().getDeclaredField("inetSocketAddress");
    f.setAccessible(true);
    f.set(channel, addr);
    f = channel.getClass().getDeclaredField("inetAddress");
    f.setAccessible(true);
    f.set(channel, addr.getAddress());

    EmbeddedChannel ec = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    sockets.add(ec);
    ChannelHandlerContext ctx = ec.pipeline().firstContext();
    f = channel.getClass().getDeclaredField("ctx");
    f.setAccessible(true);
    f.set(channel, ctx);

    byte[] helloBytes = buildHelloMessageBytes();

    ChannelManager.processMessage(channel, helloBytes);

    Assert.assertTrue(channel.isDisconnect());
    Assert.assertNull(channel.getHelloMessage());
    Assert.assertFalse(channel.isFinishHandshake());
    Assert.assertTrue(ChannelManager.getAllChannels().isEmpty());
  }

  @Test
  public void sameAddressHandshakesRemainRegisteredInBothCloseOrders() throws Exception {
    for (boolean firstActive : new boolean[]{false, true}) {
      for (boolean closeFirst : new boolean[]{false, true}) {
        ChannelManager.getBannedNodes().invalidateAll();
        Channel first = channel(firstActive, 1);
        Channel second = channel(!firstActive, 2);
        hello(first, 1);
        hello(second, 2);
        Assert.assertNotSame(first, second);
        Assert.assertEquals(first, second);
        Assert.assertTrue(first.isFinishHandshake());
        Assert.assertTrue(second.isFinishHandshake());
        assertRegistered(first, second);
        assertCounts(1, 1);

        Channel closed = closeFirst ? first : second;
        Channel survivor = closeFirst ? second : first;
        closed.close();
        Assert.assertTrue(survivor.getCtx().channel().isOpen());
        assertRegistered(survivor);
        assertCounts(survivor.isActive() ? 1 : 0, survivor.isActive() ? 0 : 1);

        ChannelManager.notifyDisconnect(closed);
        assertRegistered(survivor);
        assertCounts(survivor.isActive() ? 1 : 0, survivor.isActive() ? 0 : 1);
        survivor.close();
        assertRegistered();
        assertCounts(0, 0);
      }
    }
  }

  @Test
  public void rejectedDuplicateHandshakeKeepsEstablishedChannel() throws Exception {
    Channel established = channel(true, 1);
    Channel rejected = channel(false, 2);
    hello(established, 1);
    hello(rejected, 1);
    Assert.assertTrue(established.isFinishHandshake());
    Assert.assertTrue(established.getCtx().channel().isOpen());
    Assert.assertFalse(rejected.isFinishHandshake());
    Assert.assertTrue(rejected.isDisconnect());
    assertRegistered(established);
    assertCounts(1, 0);
  }

  @Test
  public void lateCloseNotificationKeepsSameNodeReplacement() throws Exception {
    Channel newer = channel(true, 2);
    Channel earlier = channel(false, 1);
    hello(newer, 1);
    hello(earlier, 1);
    Assert.assertTrue(newer.isDisconnect());
    Assert.assertTrue(earlier.isFinishHandshake());
    Assert.assertTrue(earlier.getCtx().channel().isOpen());
    assertRegistered(earlier);
    assertCounts(0, 1);

    ChannelManager.notifyDisconnect(newer);
    assertRegistered(earlier);
    assertCounts(0, 1);
  }

  @Test
  public void unregisteredSameAddressCloseKeepsEstablishedChannel() throws Exception {
    Channel established = channel(true, 1);
    hello(established, 1);
    Channel unregistered = channel(false, 2);
    unregistered.close();
    ChannelManager.notifyDisconnect(unregistered);
    assertRegistered(established);
    assertCounts(1, 0);
  }

  @Test
  public void repeatedAdmissionOfSameInstanceDoesNotDoubleCount() throws Exception {
    Channel established = channel(false, 1);
    hello(established, 1);
    Assert.assertEquals(DisconnectCode.NORMAL, ChannelManager.processPeer(established));
    assertRegistered(established);
    assertCounts(0, 1);
  }

  @Test
  public void connectionLimitsCountAllInstancesAtSameAddress() throws Exception {
    Channel first = channel(false, 1);
    Channel second = channel(true, 2);
    hello(first, 1);
    hello(second, 2);
    Channel candidate = channel(false, 3);
    candidate.setNodeId("third-node");

    Parameter.p2pConfig.setMaxConnections(2);
    Assert.assertEquals(DisconnectCode.TOO_MANY_PEERS, ChannelManager.processPeer(candidate));
    Parameter.p2pConfig.setMaxConnections(3);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(2);
    Assert.assertEquals(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP,
        ChannelManager.processPeer(candidate));
    assertRegistered(first, second);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void snapshotsAreDetachedAndStatusCountsInstances() throws Exception {
    Channel first = channel(false, 1);
    Channel second = channel(true, 2);
    hello(first, 1);
    hello(second, 2);

    Connect.StatusMessage status = Connect.StatusMessage.parseFrom(new StatusMessage().getData());
    Assert.assertEquals(2, status.getCurrentConnections());
    List<Channel> snapshot = ChannelManager.getAllChannels();
    Map<InetSocketAddress, Channel> addresses = ChannelManager.getChannels();
    Assert.assertEquals(1, addresses.size());
    Assert.assertSame(second, addresses.get(REMOTE));
    addresses.clear();
    snapshot.clear();
    assertRegistered(first, second);

    second.close();
    Assert.assertSame(first, ChannelManager.getChannels().get(REMOTE));
    status = Connect.StatusMessage.parseFrom(new StatusMessage().getData());
    Assert.assertEquals(1, status.getCurrentConnections());
  }

  private Channel channel(boolean active, long order) throws Exception {
    Channel channel = new Channel();
    EmbeddedChannel socket = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    sockets.add(socket);
    set(channel, "inetSocketAddress", REMOTE);
    set(channel, "inetAddress", REMOTE.getAddress());
    set(channel, "isActive", active);
    set(channel, "startTime", startTime + order);
    set(channel, "ctx", socket.pipeline().firstContext());
    // Mirror P2pChannelInitializer's normal-channel close callback without opening real sockets.
    socket.closeFuture().addListener(future -> {
      channel.markDisconnected();
      ChannelManager.notifyDisconnect(channel);
    });
    return channel;
  }

  private void hello(Channel channel, int idByte) throws Exception {
    byte[] id = new byte[64];
    Arrays.fill(id, (byte) idByte);
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(id)).setAddress(ByteString.copyFromUtf8("127.0.0.2"))
        .setPort(18888).build();
    Connect.HelloMessage hello = Connect.HelloMessage.newBuilder().setFrom(endpoint)
        .setNetworkId(11111).setVersion(1).setTimestamp(System.currentTimeMillis()).build();
    handshake.processMessage(channel, new HelloMessage(hello.toByteArray()));
  }

  private void assertRegistered(Channel... expected) {
    List<Channel> actual = ChannelManager.getAllChannels();
    Assert.assertEquals(expected.length, actual.size());
    Assert.assertEquals(expected.length, ChannelManager.getChannelCount());
    Assert.assertEquals(expected.length, ChannelManager.getConnectionNum(REMOTE.getAddress()));
    for (Channel channel : expected) {
      Assert.assertTrue(actual.stream().anyMatch(registered -> registered == channel));
    }
  }

  private void assertCounts(int active, int passive) {
    Assert.assertEquals(active, pool.getActivePeersCount().get());
    Assert.assertEquals(passive, pool.getPassivePeersCount().get());
  }

  private static void set(Channel channel, String name, Object value) throws Exception {
    Field field = Channel.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(channel, value);
  }

  private byte[] buildHelloMessageBytes() {
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888)
        .build();
    Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(1)
        .setCode(DisconnectCode.NORMAL.getValue())
        .setVersion(1)
        .setTimestamp(System.currentTimeMillis())
        .build();
    return ArrayUtils.add(hello.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType());
  }
}
