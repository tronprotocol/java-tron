package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.tron.p2p.connection.business.pool.ConnPoolService;

public class ConnPoolServiceExtraTest {

  private ConnPoolService connPoolService;

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new java.util.HashMap<>();
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    connPoolService = new ConnPoolService();
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.handlerList = new ArrayList<>();
  }

  @Test
  public void testOnConnectPassive() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.1", 100, false);
    connPoolService.onConnect(channel);
    Assert.assertEquals(1, connPoolService.getPassivePeersCount().get());
    Assert.assertEquals(0, connPoolService.getActivePeersCount().get());
  }

  @Test
  public void testOnConnectActive() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.2", 100, true);
    connPoolService.onConnect(channel);
    Assert.assertEquals(0, connPoolService.getPassivePeersCount().get());
    Assert.assertEquals(1, connPoolService.getActivePeersCount().get());
  }

  @Test
  public void testOnConnectDuplicate() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.3", 100, false);
    connPoolService.onConnect(channel);
    connPoolService.onConnect(channel); // duplicate add
    Assert.assertEquals(1, connPoolService.getPassivePeersCount().get());
  }

  @Test
  public void testOnDisconnectPassive() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.4", 100, false);
    connPoolService.onConnect(channel);
    Assert.assertEquals(1, connPoolService.getPassivePeersCount().get());

    connPoolService.onDisconnect(channel);
    Assert.assertEquals(0, connPoolService.getPassivePeersCount().get());
  }

  @Test
  public void testOnDisconnectActive() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.5", 100, true);
    connPoolService.onConnect(channel);
    Assert.assertEquals(1, connPoolService.getActivePeersCount().get());

    connPoolService.onDisconnect(channel);
    Assert.assertEquals(0, connPoolService.getActivePeersCount().get());
  }

  @Test
  public void testOnDisconnectNotInList() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.6", 100, false);
    // Disconnect without connect first
    connPoolService.onDisconnect(channel);
    Assert.assertEquals(0, connPoolService.getPassivePeersCount().get());
  }

  @Test
  public void testOnMessage() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.7", 100, false);
    connPoolService.onMessage(channel, new byte[]{0x01});
    // Should do nothing
  }

  @Test
  public void testTriggerConnectConfigActiveNode() throws Exception {
    InetSocketAddress addr = new InetSocketAddress("10.0.0.8", 100);
    Parameter.p2pConfig.getActiveNodes().add(addr);

    // Recreate ConnPoolService so configActiveNodes includes the address added above
    connPoolService = new ConnPoolService();

    connPoolService.triggerConnect(addr);
    // Should return early because it's a config active node
    // connectingPeersCount should not change
    Assert.assertEquals(0, connPoolService.getConnectingPeersCount().get());

    Parameter.p2pConfig.getActiveNodes().clear();
  }

  @Test
  public void testTriggerConnectNonConfigNode() throws Exception {
    InetSocketAddress addr = new InetSocketAddress("10.0.0.9", 100);
    connPoolService.getConnectingPeersCount().set(5);

    // This will decrement connecting peers count
    connPoolService.triggerConnect(addr);
    Assert.assertEquals(4, connPoolService.getConnectingPeersCount().get());
  }

  @Test
  public void testClose() throws Exception {
    // Add an active peer that is not disconnected
    Channel channel = createChannelWithMockCtx("10.0.0.10", 100, false);
    connPoolService.onConnect(channel);

    connPoolService.close();
    // Should send disconnect to all active peers and shutdown executors
  }

  @Test
  public void testCloseAlreadyDisconnected() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.11", 100, false);
    channel.setDisconnect(true);
    connPoolService.onConnect(channel);

    connPoolService.close();
    // Should skip sending disconnect to already disconnected channels
  }

  private Channel createChannelWithMockCtx(
      String ip, int port, boolean active) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress addr = new InetSocketAddress(ip, port);
    setFieldValue(channel, "inetSocketAddress", addr);
    setFieldValue(channel, "inetAddress", addr.getAddress());
    if (active) {
      setFieldValue(channel, "isActive", true);
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
