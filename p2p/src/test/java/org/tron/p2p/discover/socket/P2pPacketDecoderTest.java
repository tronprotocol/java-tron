package org.tron.p2p.discover.socket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.MessageType;
import org.tron.p2p.discover.message.kad.NeighborsMessage;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.message.kad.PongMessage;
import org.tron.p2p.utils.NetUtil;

/**
 * Every inbound discovery datagram lands here first. A remote peer controls the
 * bytes entirely, so the decoder has to swallow anything malformed rather than
 * let it escape up the pipeline.
 */
public class P2pPacketDecoderTest {

  private static final InetSocketAddress SENDER =
      new InetSocketAddress("127.0.0.1", 18888);
  private static final InetSocketAddress RECIPIENT =
      new InetSocketAddress("127.0.0.2", 18889);

  private P2pConfig saved;
  private Node from;

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setNetworkId(11111);
    Parameter.p2pConfig = config;
    from = new Node(NetUtil.getNodeId(), "127.0.0.1", null, 18888, 18888);
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = saved;
  }

  private static EmbeddedChannel channel() {
    return new EmbeddedChannel(new P2pPacketDecoder(), new ChannelInboundHandlerAdapter());
  }

  private static Object decode(byte[] wire) {
    EmbeddedChannel channel = channel();
    channel.writeInbound(new DatagramPacket(Unpooled.copiedBuffer(wire), RECIPIENT, SENDER));
    Object out = channel.readInbound();
    channel.finishAndReleaseAll();
    return out;
  }

  @Test
  public void wellFormedPingBecomesAUdpEvent() {
    Node to = new Node(NetUtil.getNodeId(), "127.0.0.2", null, 18889, 18889);
    Object out = decode(new PingMessage(from, to).getSendData());

    Assert.assertTrue(out instanceof UdpEvent);
    UdpEvent event = (UdpEvent) out;
    Assert.assertEquals(MessageType.KAD_PING, event.getMessage().getType());
    Assert.assertEquals(SENDER, event.getAddress());
  }

  @Test
  public void neighboursAndPongAlsoDecode() {
    Node to = new Node(NetUtil.getNodeId(), "127.0.0.2", null, 18889, 18889);
    Assert.assertTrue(decode(new PongMessage(from).getSendData()) instanceof UdpEvent);
    Assert.assertTrue(decode(
        new NeighborsMessage(from, Collections.singletonList(to), 1L).getSendData())
        instanceof UdpEvent);
  }

  @Test
  public void tooShortPacketsAreDropped() {
    // length <= 1 is rejected before any parse is attempted.
    Assert.assertNull(decode(new byte[0]));
    Assert.assertNull(decode(new byte[] {MessageType.KAD_PING.getType()}));
  }

  @Test
  public void oversizedPacketsAreDropped() {
    // MAXSIZE is 2048 and the check is >=, so 2048 bytes is already too big.
    byte[] huge = new byte[2048];
    huge[0] = MessageType.KAD_PING.getType();
    Assert.assertNull(decode(huge));
  }

  @Test
  public void unknownTypeByteIsSwallowed() {
    // Message.parse raises NO_SUCH_MESSAGE; the decoder must absorb it rather
    // than let it reach the handler and tear the shared socket down.
    Assert.assertNull(decode(new byte[] {0x7F, 1, 2, 3}));
  }

  @Test
  public void unparseableBodyIsSwallowed() {
    Assert.assertNull(decode(
        new byte[] {MessageType.KAD_PING.getType(), (byte) 0xFF, (byte) 0xFF, 0x7F}));
  }

  @Test
  public void aBadPacketDoesNotCloseTheChannel() {
    EmbeddedChannel channel = channel();
    channel.writeInbound(new DatagramPacket(
        Unpooled.copiedBuffer(new byte[] {0x7F, 1, 2, 3}), RECIPIENT, SENDER));

    Assert.assertTrue("the discovery socket is shared; one bad datagram must not close it",
        channel.isOpen());
    channel.finishAndReleaseAll();
  }
}
