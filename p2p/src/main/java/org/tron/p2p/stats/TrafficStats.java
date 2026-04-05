package org.tron.p2p.stats;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

public class TrafficStats {
  public static final TrafficStatHandler tcp = new TrafficStatHandler();
  public static final TrafficStatHandler udp = new TrafficStatHandler();

  @ChannelHandler.Sharable
  static class TrafficStatHandler extends ChannelDuplexHandler {
    @Getter
    private AtomicLong outSize = new AtomicLong();
    @Getter
    private AtomicLong inSize = new AtomicLong();
    @Getter
    private AtomicLong outPackets = new AtomicLong();
    @Getter
    private AtomicLong inPackets = new AtomicLong();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      inPackets.incrementAndGet();
      if (msg instanceof ByteBuf) {
        inSize.addAndGet(((ByteBuf) msg).readableBytes());
      } else if (msg instanceof DatagramPacket) {
        inSize.addAndGet(((DatagramPacket) msg).content().readableBytes());
      }
      super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
      outPackets.incrementAndGet();
      if (msg instanceof ByteBuf) {
        outSize.addAndGet(((ByteBuf) msg).readableBytes());
      } else if (msg instanceof DatagramPacket) {
        outSize.addAndGet(((DatagramPacket) msg).content().readableBytes());
      }
      super.write(ctx, msg, promise);
    }
  }
}
