package org.tron.p2p.connection.socket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.protos.Connect.DisconnectReason;

/**
 * MessageHandler turns a decode failure into a specific DisconnectReason before
 * tearing the channel down. That mapping is what a peer sees when it sends us
 * something we cannot read, so each branch is pinned here.
 */
public class MessageHandlerTest {

  private P2pConfig saved;

  /** Captures the disconnect message the handler sends instead of writing it out. */
  private static class RecordingChannel extends Channel {
    final List<Message> sent = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    @Override
    public void send(Message message) {
      sent.add(message);
    }

    @Override
    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
      // EmbeddedChannel's remoteAddress is an EmbeddedSocketAddress, which the
      // real implementation casts straight to InetSocketAddress. Keep the ctx
      // without the cast so channelActive does not blow up before decode runs.
      try {
        Field field = Channel.class.getDeclaredField("ctx");
        field.setAccessible(true);
        field.set(this, ctx);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void processException(Throwable throwable) {
      exceptions.add(throwable);
    }

    DisconnectReason onlyReason() throws Exception {
      Assert.assertEquals("expected exactly one message", 1, sent.size());
      Message message = sent.get(0);
      Assert.assertEquals(MessageType.DISCONNECT, message.getType());
      P2pDisconnectMessage parsed = new P2pDisconnectMessage(message.getData());
      Field field = P2pDisconnectMessage.class.getDeclaredField("p2pDisconnectMessage");
      field.setAccessible(true);
      return ((org.tron.p2p.protos.Connect.P2pDisconnectMessage) field.get(parsed)).getReason();
    }
  }

  private static void setAddress(Channel channel) throws Exception {
    Field field = Channel.class.getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(channel, new InetSocketAddress("127.0.0.1", 18888));
  }

  private static RecordingChannel feed(byte[] payload) throws Exception {
    RecordingChannel channel = new RecordingChannel();
    setAddress(channel);
    EmbeddedChannel netty = new EmbeddedChannel(new MessageHandler(channel));
    netty.writeInbound(Unpooled.wrappedBuffer(payload));
    netty.finishAndReleaseAll();
    return channel;
  }

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setPort(18888);
    config.setIp("127.0.0.1");
    config.setNetworkId(11111);
    Parameter.p2pConfig = config;
  }

  @After
  public void tearDown() {
    Parameter.p2pConfig = saved;
  }

  @Test
  public void unknownTypeByteMapsToNoSuchMessage() throws Exception {
    // 0x80 is MessageType.UNKNOWN and falls through Message.parse's default.
    RecordingChannel channel = feed(new byte[] {(byte) 0x80, 1, 2, 3});
    Assert.assertEquals(DisconnectReason.NO_SUCH_MESSAGE, channel.onlyReason());
    Assert.assertEquals(1, channel.exceptions.size());
  }

  @Test
  public void unparseableBodyMapsToBadMessage() throws Exception {
    // A known type byte with a body protobuf cannot decode.
    RecordingChannel channel = feed(
        new byte[] {MessageType.KEEP_ALIVE_PING.getType(), (byte) 0xFF, (byte) 0xFF, 0x7F});
    Assert.assertEquals(DisconnectReason.BAD_MESSAGE, channel.onlyReason());
  }

  @Test
  public void unregisteredApplicationTypeAlsoMapsToNoSuchMessage() throws Exception {
    // data[0] >= 0 routes to handMessage, which looks the byte up in the
    // registered handler map rather than going through Message.parse. With no
    // handler registered for 0x01 that path raises NO_SUCH_MESSAGE too, so a
    // peer probing unused type bytes is disconnected the same way.
    RecordingChannel channel = feed(new byte[] {0x01, 1, 2, 3});
    Assert.assertEquals(DisconnectReason.NO_SUCH_MESSAGE, channel.onlyReason());
  }

  @Test
  public void exceptionCaughtIsForwardedToTheChannel() throws Exception {
    RecordingChannel channel = new RecordingChannel();
    setAddress(channel);
    EmbeddedChannel netty = new EmbeddedChannel(new MessageHandler(channel));
    RuntimeException boom = new RuntimeException("boom");
    netty.pipeline().fireExceptionCaught(boom);
    netty.finishAndReleaseAll();

    Assert.assertEquals(1, channel.exceptions.size());
    Assert.assertSame(boom, channel.exceptions.get(0));
  }
}
