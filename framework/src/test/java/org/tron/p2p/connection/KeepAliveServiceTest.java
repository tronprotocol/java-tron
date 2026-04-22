package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.keepalive.KeepAliveService;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.message.keepalive.PongMessage;

public class KeepAliveServiceTest {

  private KeepAliveService keepAliveService;

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    keepAliveService = new KeepAliveService();
  }

  @Test
  public void testProcessPingMessage() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.1", 100);

    PingMessage ping = new PingMessage();
    keepAliveService.processMessage(channel, ping);
    // Should send pong back - verify ctx.writeAndFlush was called
    verify(channel.getCtx()).writeAndFlush(org.mockito.Mockito.any());
  }

  @Test
  public void testProcessPongMessage() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.2", 100);
    channel.waitForPong = true;
    channel.pingSent = System.currentTimeMillis() - 50;

    PongMessage pong = new PongMessage();
    keepAliveService.processMessage(channel, pong);

    Assert.assertFalse(channel.waitForPong);
    Assert.assertTrue(channel.getAvgLatency() >= 0);
  }

  @Test
  public void testProcessUnknownMessageType() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.3", 100);

    // Create a message with DISCONNECT type (not handled by keepalive)
    org.tron.p2p.connection.message.base.P2pDisconnectMessage disconnectMsg =
        new org.tron.p2p.connection.message.base.P2pDisconnectMessage(
            org.tron.p2p.protos.Connect.DisconnectReason.UNKNOWN);

    keepAliveService.processMessage(channel, disconnectMsg);
    // Should fall through to default case, nothing happens
  }

  @Test
  public void testClose() {
    keepAliveService.init();
    keepAliveService.close();
    // Should not throw
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
    setFieldValue(channel, "ctx", mockCtx);

    return channel;
  }

  private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }
}
