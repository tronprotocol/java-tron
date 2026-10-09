package org.tron.p2p.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
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
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.protos.Connect.DisconnectReason;

/**
 * Channel is the per-peer object every other component holds. These cover the
 * parts that do not need a real socket: the send guard, latency averaging, the
 * pipeline layout, and the exception classification that decides how a failure
 * is logged before the peer is dropped.
 */
public class ChannelCoreTest {

  private static final InetSocketAddress ADDRESS =
      new InetSocketAddress("127.0.0.1", 18888);

  private P2pConfig saved;

  private static void attach(Channel channel, EmbeddedChannel netty) throws Exception {
    Field ctx = Channel.class.getDeclaredField("ctx");
    ctx.setAccessible(true);
    ctx.set(channel, netty.pipeline().firstContext());
    Field address = Channel.class.getDeclaredField("inetSocketAddress");
    address.setAccessible(true);
    address.set(channel, ADDRESS);
    // close() bans by InetAddress, and Guava's cache rejects a null key, so this
    // has to be populated the way setChannelHandlerContext would.
    Field inetAddress = Channel.class.getDeclaredField("inetAddress");
    inetAddress.setAccessible(true);
    inetAddress.set(channel, ADDRESS.getAddress());
  }

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @After
  public void tearDown() {
    // close() bans the peer's address for DEFAULT_BAN_TIME in a process-wide
    // cache. Left behind, that ban on 127.0.0.1 would make any later test in
    // this fork see a recently-disconnected peer.
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.p2pConfig = saved;
  }

  @Test
  public void initBuildsTheExpectedPipeline() {
    EmbeddedChannel netty = new EmbeddedChannel();
    Channel channel = new Channel();
    channel.init(netty.pipeline(), "abcdef", false);

    Assert.assertNotNull(netty.pipeline().get("readTimeoutHandler"));
    Assert.assertNotNull(netty.pipeline().get("protoPrepend"));
    Assert.assertNotNull(netty.pipeline().get("protoDecode"));
    Assert.assertNotNull(netty.pipeline().get("messageHandler"));
    // A non-empty node id means we initiated the connection.
    Assert.assertTrue(channel.isActive());
    Assert.assertFalse(channel.isDiscoveryMode());
    netty.finishAndReleaseAll();
  }

  @Test
  public void initWithoutANodeIdIsAnInboundChannel() {
    EmbeddedChannel netty = new EmbeddedChannel();
    Channel channel = new Channel();
    channel.init(netty.pipeline(), "", true);

    Assert.assertFalse(channel.isActive());
    Assert.assertTrue(channel.isDiscoveryMode());
    netty.finishAndReleaseAll();
  }

  @Test
  public void sendWritesTheFramedMessage() throws Exception {
    EmbeddedChannel netty = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    Channel channel = new Channel();
    attach(channel, netty);

    channel.send(new PingMessage());
    netty.flushOutbound();

    ByteBuf written = netty.readOutbound();
    Assert.assertNotNull("a ping should have been written", written);
    Assert.assertEquals(MessageType.KEEP_ALIVE_PING.getType(), written.getByte(0));
    Assert.assertTrue(channel.getLastSendTime() > 0);
    netty.finishAndReleaseAll();
  }

  @Test
  public void sendIsSuppressedOnceDisconnected() throws Exception {
    EmbeddedChannel netty = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    Channel channel = new Channel();
    attach(channel, netty);
    channel.setDisconnect(true);

    channel.send(new P2pDisconnectMessage(DisconnectReason.PEER_QUITING));
    netty.flushOutbound();

    Assert.assertNull("nothing may be written after disconnect", netty.readOutbound());
    netty.finishAndReleaseAll();
  }

  @Test
  public void updateAvgLatencyIsARunningMean() {
    Channel channel = new Channel();
    Assert.assertEquals(0, channel.getAvgLatency());

    channel.updateAvgLatency(10);
    Assert.assertEquals(10, channel.getAvgLatency());

    channel.updateAvgLatency(20);
    Assert.assertEquals(15, channel.getAvgLatency());

    channel.updateAvgLatency(30);
    Assert.assertEquals(20, channel.getAvgLatency());
  }

  @Test
  public void processExceptionClassifiesAndCloses() throws Exception {
    for (Throwable throwable : new Throwable[] {
        ReadTimeoutException.INSTANCE,
        new IOException("reset by peer"),
        new CorruptedFrameException("bad frame"),
        new P2pException(P2pException.TypeEnum.BAD_MESSAGE, "nope"),
        new RuntimeException("unexpected")}) {
      EmbeddedChannel netty = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
      Channel channel = new Channel();
      attach(channel, netty);

      channel.processException(throwable);

      // Whatever the classification, the peer is dropped.
      Assert.assertTrue("channel should be marked disconnected for " + throwable,
          channel.isDisconnect());
      netty.finishAndReleaseAll();
    }
  }

  @Test
  public void closeRecordsTheDisconnectTime() throws Exception {
    EmbeddedChannel netty = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    Channel channel = new Channel();
    attach(channel, netty);

    long before = System.currentTimeMillis();
    channel.close();

    Assert.assertTrue(channel.isDisconnect());
    Assert.assertTrue(channel.getDisconnectTime() >= before);
    netty.finishAndReleaseAll();
  }
}
