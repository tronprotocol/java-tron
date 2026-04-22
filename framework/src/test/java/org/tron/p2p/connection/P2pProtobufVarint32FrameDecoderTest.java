package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.socket.P2pProtobufVarint32FrameDecoder;

public class P2pProtobufVarint32FrameDecoderTest {

  private P2pProtobufVarint32FrameDecoder decoder;
  private Channel channel;
  private ChannelHandlerContext mockCtx;

  @Before
  public void setUp() throws Exception {
    Parameter.p2pConfig = new P2pConfig();
    channel = new Channel();
    decoder = new P2pProtobufVarint32FrameDecoder(channel);

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

  @Test
  public void testDecodeSmallMessage() throws Exception {
    // Create a buffer: varint length=3, followed by 3 bytes of data
    ByteBuf in = Unpooled.buffer();
    in.writeByte(3); // varint for length 3
    in.writeBytes(new byte[]{0x01, 0x02, 0x03});

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(1, out.size());
    ByteBuf result = (ByteBuf) out.get(0);
    Assert.assertEquals(3, result.readableBytes());
    result.release();
    in.release();
  }

  @Test
  public void testDecodeEmptyBuffer() throws Exception {
    ByteBuf in = Unpooled.buffer(0);
    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);
    Assert.assertEquals(0, out.size());
    in.release();
  }

  @Test
  public void testDecodeNotEnoughData() throws Exception {
    // Write varint indicating length=10 but only provide 3 bytes
    ByteBuf in = Unpooled.buffer();
    in.writeByte(10); // varint length=10
    in.writeBytes(new byte[]{0x01, 0x02, 0x03}); // only 3 bytes

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(0, out.size());
    // reader index should be reset
    Assert.assertEquals(0, in.readerIndex());
    in.release();
  }

  @Test(expected = CorruptedFrameException.class)
  public void testDecodeNegativeLength() throws Exception {
    // Construct a varint that decodes to a negative value
    // A 5-byte varint with high bit set in last byte = CorruptedFrameException
    ByteBuf in = Unpooled.buffer();
    in.writeByte(0x80);
    in.writeByte(0x80);
    in.writeByte(0x80);
    in.writeByte(0x80);
    in.writeByte(0x80); // 5th byte with high bit set -> malformed
    List<Object> out = new ArrayList<>();
    try {
      invokeProtectedDecode(mockCtx, in, out);
    } finally {
      in.release();
    }
  }

  @Test
  public void testDecodeMessageTooLarge() throws Exception {
    // Create a varint that represents a very large number (> MAX_MESSAGE_LENGTH)
    // MAX_MESSAGE_LENGTH = 5 * 1024 * 1024 = 5242880
    // Encode 6000000 as varint: need multi-byte varint
    ByteBuf in = Unpooled.buffer();
    writeVarint32(in, 6000000);
    // Add some dummy data
    in.writeBytes(new byte[10]);

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    // Should clear buffer and close channel
    Assert.assertEquals(0, out.size());
    Assert.assertTrue(channel.isDisconnect());
    in.release();
  }

  @Test
  public void testDecodeTwoByteVarint() throws Exception {
    // Length 200 requires 2-byte varint: 0xC8 0x01
    ByteBuf in = Unpooled.buffer();
    writeVarint32(in, 200);
    byte[] payload = new byte[200];
    for (int i = 0; i < 200; i++) {
      payload[i] = (byte) (i & 0xFF);
    }
    in.writeBytes(payload);

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(1, out.size());
    ByteBuf result = (ByteBuf) out.get(0);
    Assert.assertEquals(200, result.readableBytes());
    result.release();
    in.release();
  }

  @Test
  public void testDecodeThreeByteVarint() throws Exception {
    // Length 20000 requires 3-byte varint
    ByteBuf in = Unpooled.buffer();
    writeVarint32(in, 20000);
    byte[] payload = new byte[20000];
    in.writeBytes(payload);

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(1, out.size());
    ByteBuf result = (ByteBuf) out.get(0);
    Assert.assertEquals(20000, result.readableBytes());
    result.release();
    in.release();
  }

  @Test
  public void testDecodeTwoByteVarintIncompleteSecondByte() throws Exception {
    // Write only the first byte of a multi-byte varint
    ByteBuf in = Unpooled.buffer();
    in.writeByte(0x80); // continuation bit set, no more bytes

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(0, out.size());
    // Reader index should be reset
    Assert.assertEquals(0, in.readerIndex());
    in.release();
  }

  @Test
  public void testDecodeThreeByteVarintIncomplete() throws Exception {
    ByteBuf in = Unpooled.buffer();
    in.writeByte(0x80); // continuation
    in.writeByte(0x80); // continuation, no third byte

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(0, out.size());
    Assert.assertEquals(0, in.readerIndex());
    in.release();
  }

  @Test
  public void testDecodeFourByteVarintIncomplete() throws Exception {
    ByteBuf in = Unpooled.buffer();
    in.writeByte(0x80);
    in.writeByte(0x80);
    in.writeByte(0x80);
    // Missing 4th byte

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(0, out.size());
    Assert.assertEquals(0, in.readerIndex());
    in.release();
  }

  @Test
  public void testDecodeFiveByteVarintIncomplete() throws Exception {
    ByteBuf in = Unpooled.buffer();
    in.writeByte(0x80);
    in.writeByte(0x80);
    in.writeByte(0x80);
    in.writeByte(0x80);
    // Missing 5th byte

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    Assert.assertEquals(0, out.size());
    Assert.assertEquals(0, in.readerIndex());
    in.release();
  }

  @Test
  public void testDecodeZeroLengthMessage() throws Exception {
    // Varint encoding of 0 is just byte 0x00
    ByteBuf in = Unpooled.buffer();
    in.writeByte(0);

    List<Object> out = new ArrayList<>();
    invokeProtectedDecode(mockCtx, in, out);

    // preIndex == in.readerIndex() check: varint returns 0, but reader advances
    // Actually readRawVarint32 returns 0 for positive byte=0, so length=0
    // preIndex (0) != readerIndex (1), length=0, readableBytes >= 0, so reads 0-length slice
    Assert.assertEquals(1, out.size());
    ByteBuf result = (ByteBuf) out.get(0);
    Assert.assertEquals(0, result.readableBytes());
    result.release();
    in.release();
  }

  private void invokeProtectedDecode(
      ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    Method decodeMethod = P2pProtobufVarint32FrameDecoder.class.getDeclaredMethod(
        "decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
    decodeMethod.setAccessible(true);
    try {
      decodeMethod.invoke(decoder, ctx, in, out);
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

  private void writeVarint32(ByteBuf buf, int value) {
    while (true) {
      if ((value & ~0x7F) == 0) {
        buf.writeByte(value);
        return;
      }
      buf.writeByte((value & 0x7F) | 0x80);
      value >>>= 7;
    }
  }

  private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }
}
