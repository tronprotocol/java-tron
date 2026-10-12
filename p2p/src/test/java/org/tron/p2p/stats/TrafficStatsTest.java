package org.tron.p2p.stats;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;

/**
 * TrafficStats sits at the head of both the TCP and UDP pipelines and is the
 * only source for the counters P2pService.getP2pStats() reports.
 */
public class TrafficStatsTest {

  private static final InetSocketAddress RECIPIENT =
      new InetSocketAddress("127.0.0.1", 18888);

  @Test
  public void byteBufTrafficIsCountedBothWays() {
    TrafficStats.TrafficStatHandler handler = new TrafficStats.TrafficStatHandler();
    long inPackets = handler.getInPackets().get();
    long inSize = handler.getInSize().get();
    long outPackets = handler.getOutPackets().get();
    long outSize = handler.getOutSize().get();

    EmbeddedChannel channel =
        new EmbeddedChannel(handler, new ChannelInboundHandlerAdapter());
    channel.writeInbound(Unpooled.wrappedBuffer(new byte[7]));
    channel.writeOutbound(Unpooled.wrappedBuffer(new byte[11]));

    Assert.assertEquals(inPackets + 1, handler.getInPackets().get());
    Assert.assertEquals(inSize + 7, handler.getInSize().get());
    Assert.assertEquals(outPackets + 1, handler.getOutPackets().get());
    Assert.assertEquals(outSize + 11, handler.getOutSize().get());
    channel.finishAndReleaseAll();
  }

  @Test
  public void datagramTrafficIsCountedByContentLength() {
    TrafficStats.TrafficStatHandler handler = new TrafficStats.TrafficStatHandler();
    long inSize = handler.getInSize().get();

    EmbeddedChannel channel =
        new EmbeddedChannel(handler, new ChannelInboundHandlerAdapter());
    ByteBuf content = Unpooled.wrappedBuffer(new byte[13]);
    channel.writeInbound(new DatagramPacket(content, RECIPIENT));

    // The datagram header is not counted, only the payload.
    Assert.assertEquals(inSize + 13, handler.getInSize().get());
    channel.finishAndReleaseAll();
  }

  @Test
  public void nonBufferMessagesCountAsPacketsWithoutSize() {
    TrafficStats.TrafficStatHandler handler = new TrafficStats.TrafficStatHandler();
    long inPackets = handler.getInPackets().get();
    long inSize = handler.getInSize().get();

    EmbeddedChannel channel =
        new EmbeddedChannel(handler, new ChannelInboundHandlerAdapter());
    channel.writeInbound("not a buffer");

    Assert.assertEquals(inPackets + 1, handler.getInPackets().get());
    Assert.assertEquals(inSize, handler.getInSize().get());
    channel.finishAndReleaseAll();
  }

  @Test
  public void tcpAndUdpHandlersAreSeparateInstances() {
    Assert.assertNotSame(TrafficStats.tcp, TrafficStats.udp);
  }
}
