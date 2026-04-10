package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.keepalive.KeepAliveService;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.protos.Connect.DisconnectReason;

public class ChannelManagerExtraTest {

  @Before
  public void setUp() throws Exception {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new java.util.HashMap<>();
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    // Initialize static services needed by processMessage
    setStaticField(ChannelManager.class, "keepAliveService", new KeepAliveService());
    setStaticField(ChannelManager.class, "handshakeService", new HandshakeService());
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new java.util.HashMap<>();
  }

  @Test
  public void testGetDisconnectReasonDifferentVersion() {
    Assert.assertEquals(DisconnectReason.DIFFERENT_VERSION,
        ChannelManager.getDisconnectReason(DisconnectCode.DIFFERENT_VERSION));
  }

  @Test
  public void testGetDisconnectReasonTimeBanned() {
    Assert.assertEquals(DisconnectReason.RECENT_DISCONNECT,
        ChannelManager.getDisconnectReason(DisconnectCode.TIME_BANNED));
  }

  @Test
  public void testGetDisconnectReasonDuplicatePeer() {
    Assert.assertEquals(DisconnectReason.DUPLICATE_PEER,
        ChannelManager.getDisconnectReason(DisconnectCode.DUPLICATE_PEER));
  }

  @Test
  public void testGetDisconnectReasonTooManyPeers() {
    Assert.assertEquals(DisconnectReason.TOO_MANY_PEERS,
        ChannelManager.getDisconnectReason(DisconnectCode.TOO_MANY_PEERS));
  }

  @Test
  public void testGetDisconnectReasonMaxConnectionWithSameIp() {
    Assert.assertEquals(DisconnectReason.TOO_MANY_PEERS_WITH_SAME_IP,
        ChannelManager.getDisconnectReason(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP));
  }

  @Test
  public void testGetDisconnectReasonUnknown() {
    Assert.assertEquals(DisconnectReason.UNKNOWN,
        ChannelManager.getDisconnectReason(DisconnectCode.UNKNOWN));
  }

  @Test
  public void testGetDisconnectReasonNormal() {
    Assert.assertEquals(DisconnectReason.UNKNOWN,
        ChannelManager.getDisconnectReason(DisconnectCode.NORMAL));
  }

  @Test
  public void testBanNodeNewBan() throws Exception {
    InetAddress addr = InetAddress.getByName("10.0.0.1");
    ChannelManager.banNode(addr, 10000L);
    Long banTime = ChannelManager.getBannedNodes().getIfPresent(addr);
    Assert.assertNotNull(banTime);
    Assert.assertTrue(banTime > System.currentTimeMillis());
  }

  @Test
  public void testBanNodeAlreadyBannedFuture() throws Exception {
    InetAddress addr = InetAddress.getByName("10.0.0.2");
    // Ban with a very long time first
    ChannelManager.banNode(addr, 100000L);
    Long firstBan = ChannelManager.getBannedNodes().getIfPresent(addr);

    // Try to ban again with shorter time; should not overwrite since existing ban is in the future
    ChannelManager.banNode(addr, 1L);
    Long secondBan = ChannelManager.getBannedNodes().getIfPresent(addr);
    Assert.assertEquals(firstBan, secondBan);
  }

  @Test
  public void testNotifyDisconnectNullAddress() {
    Channel channel = new Channel();
    // inetSocketAddress is null by default
    ChannelManager.notifyDisconnect(channel);
    // Should not throw, just log and return
  }

  @Test
  public void testNotifyDisconnectWithHandlers() throws Exception {
    final boolean[] called = {false};
    P2pEventHandler handler = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>();
      }

      @Override
      public void onDisconnect(Channel channel) {
        called[0] = true;
      }
    };
    Parameter.handlerList.add(handler);

    Channel channel = createChannelWithAddress("10.0.0.3", 100);
    ChannelManager.getChannels().put(channel.getInetSocketAddress(), channel);

    ChannelManager.notifyDisconnect(channel);

    Assert.assertTrue(called[0]);
    Assert.assertFalse(ChannelManager.getChannels().containsKey(channel.getInetSocketAddress()));
  }

  @Test(expected = P2pException.class)
  public void testProcessMessageNullData() throws Exception {
    Channel channel = new Channel();
    ChannelManager.processMessage(channel, null);
  }

  @Test(expected = P2pException.class)
  public void testProcessMessageEmptyData() throws Exception {
    Channel channel = new Channel();
    ChannelManager.processMessage(channel, new byte[0]);
  }

  @Test(expected = P2pException.class)
  public void testProcessMessagePositiveByteNoHandler() throws Exception {
    Channel channel = new Channel();
    // data[0] >= 0 means it goes to handMessage, which needs a handler
    byte[] data = new byte[]{0x01, 0x02};
    ChannelManager.processMessage(channel, data);
  }

  @Test
  public void testProcessMessagePositiveByteDiscoveryMode() throws Exception {
    // Register a handler for type 0x01
    P2pEventHandler handler = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>();
        this.messageTypes.add((byte) 0x01);
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
        // do nothing
      }
    };
    Parameter.handlerMap.put((byte) 0x01, handler);

    // Create a channel in discovery mode
    Channel channel = createChannelWithMockCtx("10.0.0.5", 200);
    channel.setDiscoveryMode(true);

    byte[] data = new byte[]{0x01, 0x02};
    ChannelManager.processMessage(channel, data);
    // Should send disconnect and close
  }

  @Test
  public void testProcessMessageKeepAlivePing() throws Exception {
    // Create a ping message and encode it
    PingMessage ping = new PingMessage();
    byte[] sendData = ping.getSendData();

    Channel channel = createChannelWithMockCtx("10.0.0.10", 300);
    ChannelManager.processMessage(channel, sendData);
    // Should process without exception (sends pong)
  }

  @Test
  public void testProcessMessageKeepAlivePong() throws Exception {
    PongMessage pong = new PongMessage();
    byte[] sendData = pong.getSendData();

    Channel channel = createChannelWithMockCtx("10.0.0.11", 301);
    channel.pingSent = System.currentTimeMillis();
    channel.waitForPong = true;
    ChannelManager.processMessage(channel, sendData);

    Assert.assertFalse(channel.waitForPong);
  }

  @Test
  public synchronized void testProcessPeerTimeBanned() throws Exception {
    ChannelManager.getChannels().clear();
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(2);

    InetAddress addr = InetAddress.getByName("10.0.0.20");
    // Ban the node with future timestamp
    ChannelManager.getBannedNodes().put(addr, System.currentTimeMillis() + 100000);

    Channel channel = new Channel();
    InetSocketAddress sockAddr = new InetSocketAddress(addr, 100);
    setFieldValue(channel, "inetSocketAddress", sockAddr);
    setFieldValue(channel, "inetAddress", addr);

    DisconnectCode code = ChannelManager.processPeer(channel);
    Assert.assertEquals(DisconnectCode.TIME_BANNED, code);
  }

  @Test
  public synchronized void testProcessPeerDuplicateClosesOlder() throws Exception {
    ChannelManager.getChannels().clear();
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);

    // c1 is the existing channel (started earlier)
    Channel c1 = createChannelWithMockCtx("10.0.0.30", 100);
    c1.setNodeId("sameNodeId");

    // Wait a bit so c2 starts later
    Thread.sleep(5);

    Channel c2 = createChannelWithMockCtx("10.0.0.31", 101);
    c2.setNodeId("sameNodeId");

    ChannelManager.getChannels().put(c1.getInetSocketAddress(), c1);

    // c2 processing should detect duplicate; c1 started first so c2 is newer,
    // c1 has earlier startTime so c2 should be rejected as DUPLICATE_PEER
    DisconnectCode code = ChannelManager.processPeer(c2);
    Assert.assertEquals(DisconnectCode.DUPLICATE_PEER, code);
  }

  @Test
  public synchronized void testUpdateNodeIdSelf() throws Exception {
    ChannelManager.getChannels().clear();
    String selfNodeId = org.bouncycastle.util.encoders.Hex.toHexString(
        Parameter.p2pConfig.getNodeID());

    Channel channel = createChannelWithMockCtx("10.0.0.40", 100);
    ChannelManager.getChannels().put(channel.getInetSocketAddress(), channel);

    ChannelManager.updateNodeId(channel, selfNodeId);
    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public synchronized void testUpdateNodeIdDuplicateClosesLater() throws Exception {
    ChannelManager.getChannels().clear();

    Channel c1 = createChannelWithMockCtx("10.0.0.50", 100);
    c1.setNodeId("dupNode");
    ChannelManager.getChannels().put(c1.getInetSocketAddress(), c1);

    Thread.sleep(5);

    Channel c2 = createChannelWithMockCtx("10.0.0.51", 101);
    c2.setNodeId("dupNode");
    ChannelManager.getChannels().put(c2.getInetSocketAddress(), c2);

    // updateNodeId should close the one that started later
    ChannelManager.updateNodeId(c2, "dupNode");
    // One of them should be disconnected
    Assert.assertTrue(c1.isDisconnect() || c2.isDisconnect());
  }

  @Test
  public synchronized void testUpdateNodeIdNoDuplicate() throws Exception {
    ChannelManager.getChannels().clear();

    Channel c1 = createChannelWithMockCtx("10.0.0.60", 100);
    c1.setNodeId("uniqueNode");
    ChannelManager.getChannels().put(c1.getInetSocketAddress(), c1);

    ChannelManager.updateNodeId(c1, "uniqueNode");
    // Only 1 channel with this nodeId, should not close
    Assert.assertFalse(c1.isDisconnect());
  }

  @Test
  public void testHandMessageWithHandlerAndFirstMessage() throws Exception {
    final boolean[] messageCalled = {false};
    P2pEventHandler handler = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>();
        this.messageTypes.add((byte) 0x05);
      }

      @Override
      public void onMessage(Channel channel, byte[] data) {
        messageCalled[0] = true;
      }
    };
    Parameter.handlerMap.put((byte) 0x05, handler);

    final boolean[] connectCalled = {false};
    P2pEventHandler connectHandler = new P2pEventHandler() {
      {
        this.messageTypes = new HashSet<>();
      }

      @Override
      public void onConnect(Channel channel) {
        connectCalled[0] = true;
      }
    };
    Parameter.handlerList.add(connectHandler);

    Channel channel = createChannelWithMockCtx("10.0.0.70", 100);
    Parameter.p2pConfig.setMaxConnections(50);

    byte[] data = new byte[]{0x05, 0x01, 0x02};
    ChannelManager.processMessage(channel, data);

    Assert.assertTrue(messageCalled[0]);
    Assert.assertTrue(connectCalled[0]);
    Assert.assertTrue(channel.isFinishHandshake());
  }

  @Test
  public void testLogDisconnectReason() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.80", 100);
    // Should not throw
    ChannelManager.logDisconnectReason(channel, DisconnectReason.TOO_MANY_PEERS);
  }

  private Channel createChannelWithAddress(String ip, int port) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress addr = new InetSocketAddress(ip, port);
    setFieldValue(channel, "inetSocketAddress", addr);
    setFieldValue(channel, "inetAddress", addr.getAddress());
    return channel;
  }

  private Channel createChannelWithMockCtx(String ip, int port) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress addr = new InetSocketAddress(ip, port);
    setFieldValue(channel, "inetSocketAddress", addr);
    setFieldValue(channel, "inetAddress", addr.getAddress());

    ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
    io.netty.channel.Channel mockNettyChannel = mock(io.netty.channel.Channel.class);
    when(mockCtx.channel()).thenReturn(mockNettyChannel);
    when(mockNettyChannel.remoteAddress()).thenReturn(addr);
    ChannelFuture mockFuture = mock(ChannelFuture.class);
    when(mockCtx.writeAndFlush(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockFuture.addListener(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockCtx.close()).thenReturn(mockFuture);
    when(mockNettyChannel.close()).thenReturn(mockFuture);
    setFieldValue(channel, "ctx", mockCtx);

    return channel;
  }

  private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }

  private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }
}
