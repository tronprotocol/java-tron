package org.tron.p2p.discover.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Constant;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.kad.PingMessage;

public class MessageHandlerTest {

  private NioDatagramChannel channel;
  private EventHandler eventHandler;
  private MessageHandler messageHandler;
  private ChannelHandlerContext ctx;

  @Before
  public void init() {
    Parameter.p2pConfig = new P2pConfig();
    channel = Mockito.mock(NioDatagramChannel.class);
    eventHandler = Mockito.mock(EventHandler.class);
    messageHandler = new MessageHandler(channel, eventHandler);
    ctx = Mockito.mock(ChannelHandlerContext.class);

    Mockito.when(channel.write(Mockito.any())).thenReturn(null);
  }

  @Test
  public void testChannelActive() throws Exception {
    messageHandler.channelActive(ctx);
    Mockito.verify(eventHandler).channelActivated();
  }

  @Test
  public void testChannelRead0() {
    byte[] nodeId1 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId1, (byte) 0x01);
    Node fromNode = new Node(nodeId1, "192.168.1.1", null, 18888);

    byte[] nodeId2 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId2, (byte) 0x02);
    Node toNode = new Node(nodeId2, "192.168.1.2", null, 18889);

    PingMessage ping = new PingMessage(fromNode, toNode);
    InetSocketAddress address = new InetSocketAddress("192.168.1.1", 18888);
    UdpEvent event = new UdpEvent(ping, address);

    messageHandler.channelRead0(ctx, event);

    ArgumentCaptor<UdpEvent> captor = ArgumentCaptor.forClass(UdpEvent.class);
    Mockito.verify(eventHandler).handleEvent(captor.capture());
    Assert.assertEquals(event, captor.getValue());
  }

  @Test
  public void testAcceptSendsPacket() {
    byte[] nodeId1 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId1, (byte) 0x01);
    Node fromNode = new Node(nodeId1, "192.168.1.1", null, 18888);

    byte[] nodeId2 = new byte[Constant.NODE_ID_LEN];
    Arrays.fill(nodeId2, (byte) 0x02);
    Node toNode = new Node(nodeId2, "192.168.1.2", null, 18889);

    PingMessage ping = new PingMessage(fromNode, toNode);
    InetSocketAddress address = new InetSocketAddress("192.168.1.2", 18889);
    UdpEvent event = new UdpEvent(ping, address);

    messageHandler.accept(event);

    Mockito.verify(channel).write(Mockito.any());
    Mockito.verify(channel).flush();
  }

  @Test
  public void testChannelReadComplete() {
    messageHandler.channelReadComplete(ctx);
    Mockito.verify(ctx).flush();
  }

  @Test
  public void testExceptionCaught() {
    Channel nettyChannel = Mockito.mock(Channel.class);
    Mockito.when(ctx.channel()).thenReturn(nettyChannel);
    Mockito.when(nettyChannel.remoteAddress())
        .thenReturn(new InetSocketAddress("192.168.1.1", 18888));

    messageHandler.exceptionCaught(ctx, new RuntimeException("test error"));

    Mockito.verify(ctx).close();
  }
}
