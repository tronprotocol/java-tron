package org.tron.p2p.connection.socket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CorruptedFrameException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.connection.Channel;

/**
 * Covers the varint32 length-prefix decoder that fronts every p2p channel pipeline.
 * Framing bugs here corrupt every message on the wire, and the decoder must also be
 * safe against partial reads, since TCP can split a frame anywhere.
 *
 * <p>Expected varint encodings below are LEB128 by hand, not taken from the decoder:
 * 5 -> 05, 127 -> 7f, 128 -> 80 01, 300 -> ac 02, 16384 -> 80 80 01.
 */
public class P2pProtobufVarint32FrameDecoderTest {

  private static Method readRawVarint32;

  @BeforeClass
  public static void init() throws Exception {
    readRawVarint32 = P2pProtobufVarint32FrameDecoder.class
        .getDeclaredMethod("readRawVarint32", ByteBuf.class);
    readRawVarint32.setAccessible(true);
  }

  private int readVarint(int... bytes) throws Exception {
    byte[] data = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      data[i] = (byte) bytes[i];
    }
    ByteBuf buf = Unpooled.wrappedBuffer(data);
    try {
      return (Integer) readRawVarint32.invoke(null, buf);
    } finally {
      buf.release();
    }
  }

  @Test
  public void readsSingleByteVarints() throws Exception {
    Assert.assertEquals(0, readVarint(0x00));
    Assert.assertEquals(5, readVarint(0x05));
    Assert.assertEquals(127, readVarint(0x7f));
  }

  @Test
  public void readsMultiByteVarints() throws Exception {
    Assert.assertEquals(128, readVarint(0x80, 0x01));
    Assert.assertEquals(300, readVarint(0xac, 0x02));
    Assert.assertEquals(16384, readVarint(0x80, 0x80, 0x01));
    // four- and five-byte forms
    Assert.assertEquals(1 << 21, readVarint(0x80, 0x80, 0x80, 0x01));
    Assert.assertEquals(1 << 28, readVarint(0x80, 0x80, 0x80, 0x80, 0x01));
  }

  @Test
  public void returnsZeroOnEmptyBuffer() throws Exception {
    ByteBuf buf = Unpooled.buffer(0);
    try {
      Assert.assertEquals(0, ((Integer) readRawVarint32.invoke(null, buf)).intValue());
    } finally {
      buf.release();
    }
  }

  @Test
  public void truncatedVarintYieldsZeroAndRewinds() throws Exception {
    // every continuation byte says "more follows" but the buffer ends, so the
    // decoder must report 0 and leave the reader index untouched for the next read
    for (int len = 1; len <= 4; len++) {
      byte[] data = new byte[len];
      for (int i = 0; i < len; i++) {
        data[i] = (byte) 0x80;
      }
      ByteBuf buf = Unpooled.wrappedBuffer(data);
      try {
        Assert.assertEquals(0, ((Integer) readRawVarint32.invoke(null, buf)).intValue());
        Assert.assertEquals("reader index must be rewound for a truncated varint",
            0, buf.readerIndex());
      } finally {
        buf.release();
      }
    }
  }

  @Test
  public void rejectsMalformedFiveByteVarint() throws Exception {
    // a fifth byte with the continuation bit still set overflows an int
    try {
      readVarint(0x80, 0x80, 0x80, 0x80, 0x80);
      Assert.fail("expected a malformed varint to be rejected");
    } catch (InvocationTargetException e) {
      Assert.assertTrue(e.getCause() instanceof CorruptedFrameException);
    }
  }

  @Test
  public void decodesCompleteFrame() {
    EmbeddedChannel ch = new EmbeddedChannel(
        new P2pProtobufVarint32FrameDecoder(new Channel()));
    try {
      byte[] payload = new byte[] {1, 2, 3, 4, 5};
      ByteBuf in = Unpooled.buffer();
      in.writeByte(payload.length);
      in.writeBytes(payload);

      Assert.assertTrue(ch.writeInbound(in));
      ByteBuf out = ch.readInbound();
      Assert.assertNotNull(out);
      try {
        Assert.assertEquals(payload.length, out.readableBytes());
        byte[] got = new byte[out.readableBytes()];
        out.readBytes(got);
        Assert.assertArrayEquals(payload, got);
      } finally {
        out.release();
      }
    } finally {
      ch.finishAndReleaseAll();
    }
  }

  @Test
  public void waitsForTheRestOfASplitFrame() {
    EmbeddedChannel ch = new EmbeddedChannel(
        new P2pProtobufVarint32FrameDecoder(new Channel()));
    try {
      // length says 5 bytes but only 2 arrive: nothing may be emitted yet
      ByteBuf first = Unpooled.buffer();
      first.writeByte(5);
      first.writeBytes(new byte[] {1, 2});
      Assert.assertFalse(ch.writeInbound(first));
      Assert.assertNull(ch.readInbound());

      // the remaining 3 bytes complete the frame
      Assert.assertTrue(ch.writeInbound(Unpooled.wrappedBuffer(new byte[] {3, 4, 5})));
      ByteBuf out = ch.readInbound();
      Assert.assertNotNull(out);
      try {
        byte[] got = new byte[out.readableBytes()];
        out.readBytes(got);
        Assert.assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, got);
      } finally {
        out.release();
      }
    } finally {
      ch.finishAndReleaseAll();
    }
  }

  @Test
  public void emitsNothingForLengthPrefixAlone() {
    EmbeddedChannel ch = new EmbeddedChannel(
        new P2pProtobufVarint32FrameDecoder(new Channel()));
    try {
      Assert.assertFalse(ch.writeInbound(Unpooled.wrappedBuffer(new byte[] {5})));
      Assert.assertNull(ch.readInbound());
    } finally {
      ch.finishAndReleaseAll();
    }
  }

  @Test
  public void decodesTwoFramesFromOneBuffer() {
    EmbeddedChannel ch = new EmbeddedChannel(
        new P2pProtobufVarint32FrameDecoder(new Channel()));
    try {
      ByteBuf in = Unpooled.buffer();
      in.writeByte(2);
      in.writeBytes(new byte[] {1, 2});
      in.writeByte(3);
      in.writeBytes(new byte[] {3, 4, 5});

      Assert.assertTrue(ch.writeInbound(in));
      ByteBuf first = ch.readInbound();
      ByteBuf second = ch.readInbound();
      Assert.assertNotNull(first);
      Assert.assertNotNull(second);
      try {
        Assert.assertEquals(2, first.readableBytes());
        Assert.assertEquals(3, second.readableBytes());
      } finally {
        first.release();
        second.release();
      }
    } finally {
      ch.finishAndReleaseAll();
    }
  }
}
