package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.timeout.ReadTimeoutException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.exception.P2pException;

public class ChannelTest {

  private Channel channel;
  private ChannelHandlerContext mockCtx;
  private io.netty.channel.Channel mockNettyChannel;

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    channel = new Channel();
    mockCtx = mock(ChannelHandlerContext.class);
    mockNettyChannel = mock(io.netty.channel.Channel.class);
    when(mockCtx.channel()).thenReturn(mockNettyChannel);
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @Test
  public void testInitWithNodeId() throws Exception {
    io.netty.channel.ChannelPipeline mockPipeline =
        mock(io.netty.channel.ChannelPipeline.class);
    when(mockPipeline.addLast(
        org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
        .thenReturn(mockPipeline);

    channel.init(mockPipeline, "abc123", false);
    Assert.assertTrue(channel.isActive());
    Assert.assertFalse(channel.isDiscoveryMode());
    Assert.assertEquals("abc123", channel.getNodeId());
  }

  @Test
  public void testInitWithEmptyNodeId() throws Exception {
    io.netty.channel.ChannelPipeline mockPipeline =
        mock(io.netty.channel.ChannelPipeline.class);
    when(mockPipeline.addLast(
        org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
        .thenReturn(mockPipeline);

    channel.init(mockPipeline, "", false);
    Assert.assertFalse(channel.isActive());
  }

  @Test
  public void testInitWithDiscoveryMode() throws Exception {
    io.netty.channel.ChannelPipeline mockPipeline =
        mock(io.netty.channel.ChannelPipeline.class);
    when(mockPipeline.addLast(
        org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
        .thenReturn(mockPipeline);

    channel.init(mockPipeline, "nodeId", true);
    Assert.assertTrue(channel.isDiscoveryMode());
    Assert.assertTrue(channel.isActive());
  }

  @Test
  public void testSetChannelHandlerContext() {
    InetSocketAddress address = new InetSocketAddress("192.168.1.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);

    channel.setChannelHandlerContext(mockCtx);

    Assert.assertEquals(mockCtx, channel.getCtx());
    Assert.assertEquals(address, channel.getInetSocketAddress());
    Assert.assertEquals(address.getAddress(), channel.getInetAddress());
    Assert.assertFalse(channel.isTrustPeer());
  }

  @Test
  public void testSetChannelHandlerContextWithTrustNode() {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    Parameter.p2pConfig.getTrustNodes().add(address.getAddress());

    channel.setChannelHandlerContext(mockCtx);

    Assert.assertTrue(channel.isTrustPeer());
    Parameter.p2pConfig.getTrustNodes().clear();
  }

  @Test
  public void testSetHelloMessage() throws Exception {
    HelloMessage helloMsg = new HelloMessage(
        org.tron.p2p.connection.business.handshake.DisconnectCode.NORMAL,
        System.currentTimeMillis());

    channel.setHelloMessage(helloMsg);

    Assert.assertEquals(helloMsg, channel.getHelloMessage());
    Assert.assertNotNull(channel.getNode());
    Assert.assertNotNull(channel.getNodeId());
  }

  @Test
  public void testProcessExceptionReadTimeout() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    ReadTimeoutException ex = ReadTimeoutException.INSTANCE;
    channel.processException(ex);

    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessExceptionIOException() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    IOException ex = new IOException("connection reset");
    channel.processException(ex);

    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessExceptionCorruptedFrame() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    CorruptedFrameException ex = new CorruptedFrameException("bad frame");
    channel.processException(ex);

    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessExceptionP2pException() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    P2pException ex = new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "test");
    channel.processException(ex);

    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessExceptionGeneric() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    RuntimeException ex = new RuntimeException("unknown error");
    channel.processException(ex);

    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testProcessExceptionWithCausalLoop() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    // Create a causal loop: ex1 -> ex2 -> ex1
    Exception ex1 = new Exception("loop1");
    Exception ex2 = new Exception("loop2", ex1);
    ex1.initCause(ex2);

    channel.processException(ex1);
    Assert.assertTrue(channel.isDisconnect());
  }

  @Test
  public void testSendByteArrayWhenDisconnected() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    setCtxField(channel, mockCtx);
    setInetSocketAddressField(channel, address);

    channel.setDisconnect(true);
    channel.send(new byte[]{0x01, 0x02});
    // Should return early without writing; no NPE
  }

  @Test
  public void testSendByteArraySuccess() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    ChannelFuture mockFuture = mock(ChannelFuture.class);
    when(mockCtx.writeAndFlush(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockFuture.addListener(org.mockito.Mockito.any())).thenReturn(mockFuture);
    setCtxField(channel, mockCtx);
    setInetSocketAddressField(channel, address);

    channel.send(new byte[]{0x01, 0x02});
    verify(mockCtx).writeAndFlush(org.mockito.Mockito.any());
  }

  @Test
  public void testSendByteArrayException() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockCtx.writeAndFlush(org.mockito.Mockito.any()))
        .thenThrow(new RuntimeException("write error"));
    when(mockNettyChannel.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetSocketAddressField(channel, address);

    channel.send(new byte[]{0x01, 0x02});
    verify(mockNettyChannel).close();
  }

  @Test
  public void testUpdateAvgLatency() {
    channel.updateAvgLatency(100);
    Assert.assertEquals(100, channel.getAvgLatency());

    channel.updateAvgLatency(200);
    Assert.assertEquals(150, channel.getAvgLatency());

    channel.updateAvgLatency(300);
    Assert.assertEquals(200, channel.getAvgLatency());
  }

  @Test
  public void testCloseWithBanTime() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.1", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    channel.close(5000L);

    Assert.assertTrue(channel.isDisconnect());
    Assert.assertTrue(channel.getDisconnectTime() > 0);
    Assert.assertNotNull(ChannelManager.getBannedNodes().getIfPresent(address.getAddress()));
    verify(mockCtx).close();
  }

  @Test
  public void testCloseDefaultBanTime() throws Exception {
    InetSocketAddress address = new InetSocketAddress("10.0.0.2", 8080);
    when(mockNettyChannel.remoteAddress()).thenReturn(address);
    when(mockCtx.close()).thenReturn(mock(ChannelFuture.class));
    setCtxField(channel, mockCtx);
    setInetAddressField(channel, address);

    channel.close();

    Assert.assertTrue(channel.isDisconnect());
    verify(mockCtx).close();
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    Channel ch1 = new Channel();
    Channel ch2 = new Channel();
    InetSocketAddress addr = new InetSocketAddress("1.2.3.4", 100);

    setInetSocketAddressField(ch1, addr);
    setInetSocketAddressField(ch2, addr);

    Assert.assertEquals(ch1, ch2);
    Assert.assertEquals(ch1.hashCode(), ch2.hashCode());

    Assert.assertTrue(ch1.equals(ch1));
    Assert.assertFalse(ch1.equals(null));
    Assert.assertFalse(ch1.equals("not a channel"));
  }

  @Test
  public void testEqualsDifferentAddress() throws Exception {
    Channel ch1 = new Channel();
    Channel ch2 = new Channel();
    setInetSocketAddressField(ch1, new InetSocketAddress("1.2.3.4", 100));
    setInetSocketAddressField(ch2, new InetSocketAddress("1.2.3.5", 100));

    Assert.assertNotEquals(ch1, ch2);
  }

  @Test
  public void testToStringWithNodeId() throws Exception {
    InetSocketAddress addr = new InetSocketAddress("1.2.3.4", 100);
    setInetSocketAddressField(channel, addr);
    channel.setNodeId("abcdef");

    String result = channel.toString();
    Assert.assertTrue(result.contains("abcdef"));
    Assert.assertTrue(result.contains("1.2.3.4"));
  }

  @Test
  public void testToStringWithoutNodeId() throws Exception {
    InetSocketAddress addr = new InetSocketAddress("1.2.3.4", 100);
    setInetSocketAddressField(channel, addr);
    channel.setNodeId("");

    String result = channel.toString();
    Assert.assertTrue(result.contains("<null>"));
  }

  private void setCtxField(Channel ch, ChannelHandlerContext ctx) throws Exception {
    Field field = ch.getClass().getDeclaredField("ctx");
    field.setAccessible(true);
    field.set(ch, ctx);
  }

  private void setInetAddressField(Channel ch, InetSocketAddress addr) throws Exception {
    Field inetField = ch.getClass().getDeclaredField("inetAddress");
    inetField.setAccessible(true);
    inetField.set(ch, addr.getAddress());
    Field inetSockField = ch.getClass().getDeclaredField("inetSocketAddress");
    inetSockField.setAccessible(true);
    inetSockField.set(ch, addr);
  }

  private void setInetSocketAddressField(Channel ch, InetSocketAddress addr) throws Exception {
    Field field = ch.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(ch, addr);
  }
}
