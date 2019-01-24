package org.tron.core.db.fast.download;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import java.io.RandomAccessFile;

public class DataDownloadServerHandler extends SimpleChannelInboundHandler<String> {

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.writeAndFlush("HELLO: Type the path of the file to retrieve.\n");
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    RandomAccessFile raf = null;
    long length = -1;
    try {
      raf = new RandomAccessFile(msg, "r");
      length = raf.length();
    } catch (Exception e) {
      ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + '\n');
      return;
    } finally {
      if (length < 0 && raf != null) {
        raf.close();
      }
    }

    ctx.write("OK: " + raf.length() + '\n');
    if (ctx.pipeline().get(SslHandler.class) == null) {
      // SSL not enabled - can use zero-copy file transfer.
      ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
    } else {
      // SSL enabled - cannot use zero-copy file transfer.
      ctx.write(new ChunkedFile(raf));
    }
    ctx.writeAndFlush("\n");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();

    if (ctx.channel().isActive()) {
      ctx.writeAndFlush("ERR: " +
          cause.getClass().getSimpleName() + ": " +
          cause.getMessage() + '\n').addListener(ChannelFutureListener.CLOSE);
    }
  }
}
