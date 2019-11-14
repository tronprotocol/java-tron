package org.tron.common.overlay.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrxProtobufVarint32FrameDecoder extends ByteToMessageDecoder {

  private static final Logger logger = LoggerFactory
      .getLogger(TrxProtobufVarint32FrameDecoder.class);

  private static final int maxMsgLength = 5 * 1024 * 1024;//5M

  private Channel channel;

  public TrxProtobufVarint32FrameDecoder(Channel channel) {
    this.channel = channel;
  }

  private static int readRawVarint32(ByteBuf buffer) {
    if (!buffer.isReadable()) {
      return 0;
    }
    buffer.markReaderIndex();
    byte tmp = buffer.readByte();
    if (tmp >= 0) {
      return tmp;
    } else {
      int result = tmp & 127;
      if (!buffer.isReadable()) {
        buffer.resetReaderIndex();
        return 0;
      }
      if ((tmp = buffer.readByte()) >= 0) {
        result |= tmp << 7;
      } else {
        result |= (tmp & 127) << 7;
        if (!buffer.isReadable()) {
          buffer.resetReaderIndex();
          return 0;
        }
        if ((tmp = buffer.readByte()) >= 0) {
          result |= tmp << 14;
        } else {
          result |= (tmp & 127) << 14;
          if (!buffer.isReadable()) {
            buffer.resetReaderIndex();
            return 0;
          }
          if ((tmp = buffer.readByte()) >= 0) {
            result |= tmp << 21;
          } else {
            result |= (tmp & 127) << 21;
            if (!buffer.isReadable()) {
              buffer.resetReaderIndex();
              return 0;
            }
            result |= (tmp = buffer.readByte()) << 28;
            if (tmp < 0) {
              throw new CorruptedFrameException("malformed varint.");
            }
          }
        }
      }
      return result;
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    in.markReaderIndex();
    int preIndex = in.readerIndex();
    int length = readRawVarint32(in);
    if (length >= maxMsgLength) {
      logger.error("recv a big msg, host : {}, msg length is : {}", ctx.channel().remoteAddress(),
          length);
      in.clear();
      channel.close();
      return;
    }
    if (preIndex == in.readerIndex()) {
      return;
    }
    if (length < 0) {
      throw new CorruptedFrameException("negative length: " + length);
    }

    if (in.readableBytes() < length) {
      in.resetReaderIndex();
    } else {
      out.add(in.readRetainedSlice(length));
    }
  }
}

