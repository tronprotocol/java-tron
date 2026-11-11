package org.tron.p2p.connection.socket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect;

public class DecoderDisconnectTest {

  private final List<EmbeddedChannel> sockets = new ArrayList<>();
  private P2pConfig previousConfig;
  private Map<Byte, P2pEventHandler> previousHandlers;

  @Before
  public void setUp() {
    previousConfig = Parameter.p2pConfig;
    previousHandlers = Parameter.handlerMap;
    Parameter.handlerMap = new HashMap<>();
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp("127.0.0.1");
    Parameter.p2pConfig.setIpv6("");
  }

  @After
  public void tearDown() {
    try {
      sockets.forEach(EmbeddedChannel::finishAndReleaseAll);
    } finally {
      Parameter.p2pConfig = previousConfig;
      Parameter.handlerMap = previousHandlers;
    }
  }

  @Test
  public void firstRejectedFrameStopsTheRemainingReadBatch() {
    DeferredCloseChannel peer = newPeer();
    EmbeddedChannel socket = sockets.get(0);
    ByteBuf batch = Unpooled.buffer(256 * 1024);
    while (batch.isWritable()) {
      batch.writeByte(1).writeByte(0x80);
    }

    socket.writeInbound(batch);

    Assert.assertTrue(peer.isDisconnect());
    Assert.assertTrue(socket.isActive());
    Assert.assertEquals(1, peer.exceptions);
    Assert.assertEquals(1, peer.messagesSent);
    Assert.assertEquals(0, batch.refCnt());
  }

  @Test
  public void registeredApplicationFramesAreRejectedOnlyOncePerReadBatch() throws Exception {
    Parameter.handlerMap.put((byte) 0x01, new P2pEventHandler() {
      @Override
      public void onMessage(Channel channel, byte[] data) {
        Assert.fail("Application messages must not be dispatched before HELLO");
      }
    });
    DeferredCloseChannel peer = newPeer();
    EmbeddedChannel socket = sockets.get(0);
    ByteBuf batch = Unpooled.buffer(256 * 1024);
    while (batch.isWritable()) {
      batch.writeByte(1).writeByte(0x01);
    }

    socket.writeInbound(batch);

    Assert.assertTrue(peer.isDisconnect());
    Assert.assertFalse(peer.isFinishHandshake());
    Assert.assertTrue(socket.isActive());
    Assert.assertEquals(0, peer.exceptions);
    Assert.assertEquals(1, peer.closeCalls);
    Assert.assertEquals(1, peer.messagesSent);
    Assert.assertEquals(MessageType.DISCONNECT, peer.lastMessage.getType());
    Assert.assertEquals(Connect.DisconnectReason.BAD_PROTOCOL,
        Connect.P2pDisconnectMessage.parseFrom(peer.lastMessage.getData()).getReason());
    Assert.assertEquals(0, batch.refCnt());
  }

  @Test
  public void frameDecoderDiscardsDataAfterDisconnectOrTransportClose() {
    for (boolean closeTransport : new boolean[] {false, true}) {
      DeferredCloseChannel peer = newPeer();
      ChannelHandlerContext ctx = peer.getCtx().pipeline().context("protoDecode");
      P2pProtobufVarint32FrameDecoder decoder =
          (P2pProtobufVarint32FrameDecoder) ctx.handler();
      disconnect(peer, closeTransport);
      ByteBuf batch = Unpooled.wrappedBuffer(new byte[] {1, (byte) 0x80, 1, (byte) 0x80});
      List<Object> frames = new ArrayList<>();
      try {
        decoder.decode(ctx, batch, frames);

        Assert.assertFalse(batch.isReadable());
        Assert.assertTrue(frames.isEmpty());
      } finally {
        frames.forEach(ReferenceCountUtil::release);
        batch.release();
      }
    }
  }

  @Test
  public void messageHandlerDiscardsDecodedDataAfterDisconnectOrTransportClose() {
    for (boolean closeTransport : new boolean[] {false, true}) {
      DeferredCloseChannel peer = newPeer();
      ChannelHandlerContext ctx = peer.getCtx();
      MessageHandler handler = (MessageHandler) ctx.handler();
      disconnect(peer, closeTransport);
      ByteBuf message = Unpooled.wrappedBuffer(new byte[] {(byte) 0x80});
      try {
        handler.decode(ctx, message, new ArrayList<>());

        Assert.assertFalse(message.isReadable());
        Assert.assertEquals(0, peer.exceptions);
        Assert.assertEquals(0, peer.messagesSent);
      } finally {
        message.release();
      }
    }
  }

  private DeferredCloseChannel newPeer() {
    EmbeddedChannel socket = new EmbeddedChannel() {
      @Override
      protected SocketAddress remoteAddress0() {
        return new InetSocketAddress("127.0.0.2", 18888);
      }
    };
    sockets.add(socket);
    DeferredCloseChannel peer = new DeferredCloseChannel();
    peer.init(socket.pipeline(), "", false);
    peer.setChannelHandlerContext(socket.pipeline().context("messageHandler"));
    return peer;
  }

  private void disconnect(Channel peer, boolean closeTransport) {
    if (closeTransport) {
      peer.getCtx().channel().close();
      Assert.assertFalse(peer.isDisconnect());
    } else {
      peer.markDisconnected();
      Assert.assertTrue(peer.getCtx().channel().isActive());
    }
  }

  private static class DeferredCloseChannel extends Channel {
    private int exceptions;
    private int messagesSent;
    private int closeCalls;
    private Message lastMessage;

    @Override
    public void close() {
      // Keep the pipeline installed to model transport cleanup pending on the event loop.
      closeCalls++;
      markDisconnected();
    }

    @Override
    public void processException(Throwable throwable) {
      exceptions++;
      super.processException(throwable);
    }

    @Override
    public void send(Message message) {
      messagesSent++;
      lastMessage = message;
      super.send(message);
    }
  }
}
