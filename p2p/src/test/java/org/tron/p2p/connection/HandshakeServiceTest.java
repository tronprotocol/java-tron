package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.utils.ByteArray;

public class HandshakeServiceTest {

  private HandshakeService handshakeService;

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new java.util.HashMap<>();
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    handshakeService = new HandshakeService();
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.handlerList = new ArrayList<>();
  }

  @Test
  public void testStartHandshake() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.1", 100, "nodeA", true);
    handshakeService.startHandshake(channel);
    // Should send a hello message without throwing
  }

  @Test
  public void testProcessMessageFinishedHandshake() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.2", 100, "nodeB", true);
    channel.setFinishHandshake(true);

    HelloMessage msg = createHelloMessage(
        DisconnectCode.NORMAL, Parameter.p2pConfig.getNetworkId(), new byte[64]);
    handshakeService.processMessage(channel, msg);

    // Should close channel due to duplicate handshake
    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessMessageActiveChannelNormalCode() throws Exception {
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);

    Channel channel = createChannelWithMockCtx("10.0.0.3", 100, "nodeC", true);

    // Create a HelloMessage with a DIFFERENT nodeId so updateNodeId won't detect "myself"
    byte[] otherNodeId = new byte[64];
    otherNodeId[0] = 0x01;
    HelloMessage msg = createHelloMessage(
        DisconnectCode.NORMAL, Parameter.p2pConfig.getNetworkId(), otherNodeId);
    handshakeService.processMessage(channel, msg);

    // Should finish handshake for active channel with normal code and matching networkId
    Assert.assertTrue(channel.isFinishHandshake());
  }

  @Test
  public void testProcessMessageActiveChannelBadCode() throws Exception {
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);

    Channel channel = createChannelWithMockCtx("10.0.0.4", 100, "nodeD", true);

    byte[] otherNodeId = new byte[64];
    otherNodeId[0] = 0x02;
    HelloMessage msg = createHelloMessage(
        DisconnectCode.TOO_MANY_PEERS, Parameter.p2pConfig.getNetworkId(), otherNodeId);
    handshakeService.processMessage(channel, msg);

    // Should close because code != NORMAL
    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessMessagePassiveChannelNormal() throws Exception {
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);

    // Passive channel (isActive = false, nodeId empty)
    Channel channel = createChannelWithMockCtx("10.0.0.5", 100, "", false);

    byte[] otherNodeId = new byte[64];
    otherNodeId[0] = 0x03;
    HelloMessage msg = createHelloMessage(
        DisconnectCode.NORMAL, Parameter.p2pConfig.getNetworkId(), otherNodeId);
    handshakeService.processMessage(channel, msg);

    // Should finish handshake and reply with hello
    Assert.assertTrue(channel.isFinishHandshake());
  }

  @Test
  public void testProcessMessagePassiveChannelDifferentNetworkId() throws Exception {
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);

    byte[] otherNodeId = new byte[64];
    otherNodeId[0] = 0x04;
    // Create a hello message with networkId=1 (default)
    HelloMessage msg = createHelloMessage(DisconnectCode.NORMAL, 1, otherNodeId);

    // Now change networkId and recreate handshake service so it captures the new networkId
    Parameter.p2pConfig.setNetworkId(999);
    handshakeService = new HandshakeService();

    Channel channel = createChannelWithMockCtx("10.0.0.6", 100, "", false);

    handshakeService.processMessage(channel, msg);

    // Should close due to different network id
    Assert.assertTrue(channel.isDisconnect());

    // Restore
    Parameter.p2pConfig.setNetworkId(1);
  }

  @Test
  public void testProcessMessageProcessPeerRejectsNonActive() throws Exception {
    // Fill up connections to trigger TOO_MANY_PEERS
    Parameter.p2pConfig.setMaxConnections(0);

    Channel channel = createChannelWithMockCtx("10.0.0.7", 100, "", false);

    byte[] otherNodeId = new byte[64];
    otherNodeId[0] = 0x05;
    HelloMessage msg = createHelloMessage(
        DisconnectCode.NORMAL, Parameter.p2pConfig.getNetworkId(), otherNodeId);
    handshakeService.processMessage(channel, msg);

    // processPeer should return TOO_MANY_PEERS, passive channel gets hello reply then close
    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessMessageActiveChannelDifferentNetworkAndVersion() throws Exception {
    Parameter.p2pConfig.setMaxConnections(50);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);

    Channel channel = createChannelWithMockCtx("10.0.0.8", 100, "nodeE", true);

    byte[] otherNodeId = new byte[64];
    otherNodeId[0] = 0x06;
    // Create hello with different networkId AND version (so both checks fail)
    HelloMessage msg = createHelloMessageFull(
        DisconnectCode.NORMAL, 999, 999, otherNodeId);
    handshakeService.processMessage(channel, msg);

    // Should close because networkId != ours and version != ours
    Assert.assertTrue(channel.isDisconnect());
  }

  /**
   * Create a HelloMessage with a custom nodeId to avoid "myself" detection.
   */
  private HelloMessage createHelloMessage(
      DisconnectCode code, int networkId, byte[] nodeId) throws Exception {
    return createHelloMessageFull(code, networkId, Parameter.version, nodeId);
  }

  private HelloMessage createHelloMessageFull(
      DisconnectCode code, int networkId, int version, byte[] nodeId) throws Exception {
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(nodeId))
        .setPort(18888)
        .setAddress(ByteString.copyFrom(ByteArray.fromString("10.0.0.99")))
        .build();

    Connect.HelloMessage proto = Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(networkId)
        .setCode(code.getValue())
        .setVersion(version)
        .setTimestamp(System.currentTimeMillis())
        .build();

    return new HelloMessage(proto.toByteArray());
  }

  private Channel createChannelWithMockCtx(
      String ip, int port, String nodeId, boolean active) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress addr = new InetSocketAddress(ip, port);
    setFieldValue(channel, "inetSocketAddress", addr);
    setFieldValue(channel, "inetAddress", addr.getAddress());
    if (active) {
      setFieldValue(channel, "isActive", true);
    }
    if (nodeId != null && !nodeId.isEmpty()) {
      channel.setNodeId(nodeId);
    }

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
}
