package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.HandshakeService;
import org.tron.p2p.connection.business.keepalive.KeepAliveService;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.connection.socket.MessageHandler;

public class MessageHandlerTest {

  private MessageHandler messageHandler;
  private Channel channel;
  private ChannelHandlerContext mockCtx;

  @Before
  public void setUp() throws Exception {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new java.util.HashMap<>();
    ChannelManager.getChannels().clear();
    // Initialize static services used by ChannelManager.processMessage
    initStaticServices();

    channel = new Channel();
    messageHandler = new MessageHandler(channel);

    mockCtx = mock(ChannelHandlerContext.class);
    io.netty.channel.Channel mockNettyChannel = mock(io.netty.channel.Channel.class);
    when(mockCtx.channel()).thenReturn(mockNettyChannel);
    InetSocketAddress addr = new InetSocketAddress("10.0.0.1", 100);
    when(mockNettyChannel.remoteAddress()).thenReturn(addr);

    ChannelFuture mockFuture = mock(ChannelFuture.class);
    when(mockCtx.writeAndFlush(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockFuture.addListener(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockCtx.close()).thenReturn(mockFuture);
    when(mockNettyChannel.close()).thenReturn(mockFuture);

    setFieldValue(channel, "ctx", mockCtx);
    setFieldValue(channel, "inetSocketAddress", addr);
    setFieldValue(channel, "inetAddress", addr.getAddress());
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    Parameter.handlerList = new ArrayList<>();
    Parameter.handlerMap = new java.util.HashMap<>();
  }

  @Test
  public void testHandlerAdded() {
    // handlerAdded is a no-op, just verify it doesn't throw
    messageHandler.handlerAdded(mockCtx);
  }

  @Test
  public void testChannelActivePassive() throws Exception {
    // Passive channel (not active, no nodeId)
    // Need to set up HandshakeService
    initHandshakeService();

    messageHandler.channelActive(mockCtx);
    // channel should now have ctx set
    Assert.assertNotNull(channel.getCtx());
    Assert.assertFalse(channel.isActive());
  }

  @Test
  public void testChannelActiveWithDiscoveryMode() throws Exception {
    // Make channel active + discovery mode
    setFieldValue(channel, "isActive", true);
    channel.setDiscoveryMode(true);

    messageHandler.channelActive(mockCtx);
    // Should send StatusMessage
    verify(mockCtx).writeAndFlush(org.mockito.Mockito.any());
  }

  @Test
  public void testChannelActiveWithHandshake() throws Exception {
    setFieldValue(channel, "isActive", true);
    channel.setDiscoveryMode(false);
    initHandshakeService();

    messageHandler.channelActive(mockCtx);
    // Should start handshake -> send HelloMessage
    verify(mockCtx).writeAndFlush(org.mockito.Mockito.any());
  }

  @Test
  public void testDecodeValidPingMessage() throws Exception {
    PingMessage ping = new PingMessage();
    byte[] sendData = ping.getSendData();

    ByteBuf buffer = Unpooled.wrappedBuffer(sendData);
    List<Object> out = new ArrayList<>();

    invokeProtectedDecode(mockCtx, buffer, out);
    // Should process without throwing
    buffer.release();
  }

  @Test
  public void testDecodeEmptyMessage() throws Exception {
    ByteBuf buffer = Unpooled.wrappedBuffer(new byte[0]);
    List<Object> out = new ArrayList<>();

    invokeProtectedDecode(mockCtx, buffer, out);
    // Should catch P2pException (EMPTY_MESSAGE) and call processException
    Assert.assertTrue(channel.isDisconnect());
    buffer.release();
  }

  @Test
  public void testDecodeInvalidMessageType() throws Exception {
    // Negative byte but not a valid message type
    ByteBuf buffer = Unpooled.wrappedBuffer(new byte[]{(byte) 0x80, 0x01, 0x02});
    List<Object> out = new ArrayList<>();

    invokeProtectedDecode(mockCtx, buffer, out);
    // Should catch P2pException (NO_SUCH_MESSAGE)
    Assert.assertTrue(channel.isDisconnect());
    buffer.release();
  }

  @Test
  public void testExceptionCaught() {
    RuntimeException ex = new RuntimeException("test error");
    messageHandler.exceptionCaught(mockCtx, ex);
    Assert.assertTrue(channel.isDisconnect());
  }

  private void invokeProtectedDecode(
      ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
    Method decodeMethod = MessageHandler.class.getDeclaredMethod(
        "decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
    decodeMethod.setAccessible(true);
    try {
      decodeMethod.invoke(messageHandler, ctx, buffer, out);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      if (e.getCause() instanceof Exception) {
        throw (Exception) e.getCause();
      }
      throw e;
    }
  }

  private void initStaticServices() throws Exception {
    Field hsField = ChannelManager.class.getDeclaredField("handshakeService");
    hsField.setAccessible(true);
    hsField.set(null, new HandshakeService());

    Field kaField = ChannelManager.class.getDeclaredField("keepAliveService");
    kaField.setAccessible(true);
    kaField.set(null, new KeepAliveService());
  }

  private void initHandshakeService() throws Exception {
    HandshakeService hs = new HandshakeService();
    Field field = ChannelManager.class.getDeclaredField("handshakeService");
    field.setAccessible(true);
    field.set(null, hs);
  }

  private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }
}
