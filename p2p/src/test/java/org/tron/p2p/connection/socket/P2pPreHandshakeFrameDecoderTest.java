package org.tron.p2p.connection.socket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect;

public class P2pPreHandshakeFrameDecoderTest {

  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private P2pConfig previousConfig;

  @Before
  public void setUp() {
    previousConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
  }

  @After
  public void tearDown() {
    try {
      sockets.forEach(EmbeddedChannel::finishAndReleaseAll);
    } finally {
      Parameter.p2pConfig = previousConfig;
    }
  }

  @Test
  public void oversizedPreHandshakeDeclarationIsRejectedBeforeReceivingPayload() throws Exception {
    for (int length : new int[]{Parameter.MAX_PRE_HANDSHAKE_LENGTH,
        Parameter.MAX_PRE_HANDSHAKE_LENGTH + 1, Parameter.MAX_MESSAGE_LENGTH - 1}) {
      TestChannel peer = newPeer();
      EmbeddedChannel socket = sockets.get(sockets.size() - 1);
      ByteBuf declaration = frameLength(length);

      Assert.assertFalse(socket.writeInbound(declaration));

      assertBadMessage(peer, socket);
      Assert.assertEquals(0, declaration.refCnt());
    }
  }

  @Test
  public void incompleteLengthPrefixIsCheckedWhenTheDeclarationCompletes() throws Exception {
    TestChannel peer = newPeer();
    EmbeddedChannel socket = sockets.get(0);

    Assert.assertFalse(socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{(byte) 0x80})));
    Assert.assertTrue(socket.isActive());
    Assert.assertNull(peer.rejection);

    Assert.assertFalse(socket.writeInbound(Unpooled.wrappedBuffer(new byte[]{8})));

    assertBadMessage(peer, socket);
  }

  @Test
  public void framesBelowPreHandshakeLimitAreDecoded() {
    newPeer();
    EmbeddedChannel socket = sockets.get(0);
    int length = Parameter.MAX_PRE_HANDSHAKE_LENGTH - 1;

    Assert.assertTrue(socket.writeInbound(frame(length)));

    assertDecodedLength(socket, length);
    Assert.assertTrue(socket.isActive());
  }

  @Test
  public void trustedPeersStillHaveThePreHandshakeFrameLimit() throws Exception {
    Parameter.p2pConfig.getTrustNodes().add(new InetSocketAddress("127.0.0.2", 18888).getAddress());
    TestChannel peer = newPeer();
    EmbeddedChannel socket = sockets.get(0);
    Assert.assertTrue(peer.isTrustPeer());

    Assert.assertFalse(socket.writeInbound(frameLength(Parameter.MAX_PRE_HANDSHAKE_LENGTH)));

    assertBadMessage(peer, socket);
  }

  @Test
  public void completedHandshakeUsesTheExistingFrameLimit() throws Exception {
    TestChannel peer = newPeer();
    EmbeddedChannel socket = sockets.get(0);
    peer.setFinishHandshake(true);
    int length = Parameter.MAX_PRE_HANDSHAKE_LENGTH + 1;

    Assert.assertTrue(socket.writeInbound(frame(length)));

    assertDecodedLength(socket, length);
    Assert.assertTrue(socket.isActive());
    Assert.assertFalse(socket.writeInbound(frameLength(Parameter.MAX_MESSAGE_LENGTH)));
    assertBadMessage(peer, socket);
  }

  @Test
  public void handshakeCompletionAppliesToTheNextFrameInTheSameReadBatch() {
    TestChannel peer = newPeer();
    EmbeddedChannel socket = sockets.get(0);
    AtomicInteger frames = new AtomicInteger();
    socket.pipeline().addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (frames.incrementAndGet() == 1) {
          peer.setFinishHandshake(true);
        }
        ctx.fireChannelRead(msg);
      }
    });
    ByteBuf batch = frame(1);
    ByteBuf next = frame(Parameter.MAX_PRE_HANDSHAKE_LENGTH + 1);
    try {
      batch.writeBytes(next);
    } finally {
      next.release();
    }

    Assert.assertTrue(socket.writeInbound(batch));

    Assert.assertEquals(2, frames.get());
    assertDecodedLength(socket, 1);
    assertDecodedLength(socket, Parameter.MAX_PRE_HANDSHAKE_LENGTH + 1);
    Assert.assertTrue(socket.isActive());
  }

  private TestChannel newPeer() {
    TestChannel peer = new TestChannel();
    EmbeddedChannel socket = new EmbeddedChannel(new P2pProtobufVarint32FrameDecoder(peer)) {
      @Override
      protected SocketAddress remoteAddress0() {
        return new InetSocketAddress("127.0.0.2", 18888);
      }
    };
    sockets.add(socket);
    peer.setChannelHandlerContext(socket.pipeline().firstContext());
    return peer;
  }

  private ByteBuf frameLength(int length) {
    ByteBuf bytes = Unpooled.buffer();
    while ((length & ~0x7f) != 0) {
      bytes.writeByte((length & 0x7f) | 0x80);
      length >>>= 7;
    }
    bytes.writeByte(length);
    return bytes;
  }

  private ByteBuf frame(int length) {
    return frameLength(length).writeZero(length);
  }

  private void assertDecodedLength(EmbeddedChannel socket, int length) {
    ByteBuf decoded = socket.readInbound();
    Assert.assertNotNull(decoded);
    try {
      Assert.assertEquals(length, decoded.readableBytes());
    } finally {
      decoded.release();
    }
  }

  private void assertBadMessage(TestChannel peer, EmbeddedChannel socket) throws Exception {
    Assert.assertTrue(peer.isDisconnect());
    Assert.assertFalse(socket.isActive());
    Assert.assertNotNull(peer.rejection);
    Assert.assertEquals(MessageType.DISCONNECT, peer.rejection.getType());
    Assert.assertEquals(Connect.DisconnectReason.BAD_MESSAGE,
        Connect.P2pDisconnectMessage.parseFrom(peer.rejection.getData()).getReason());
    Assert.assertNull(socket.readInbound());
  }

  private static class TestChannel extends Channel {
    private Message rejection;

    @Override
    public void send(Message message) {
      rejection = message;
    }

    @Override
    public void close() {
      markDisconnected();
      getCtx().close();
    }
  }
}
