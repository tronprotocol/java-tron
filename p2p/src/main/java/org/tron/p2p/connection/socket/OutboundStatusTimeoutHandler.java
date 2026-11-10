package org.tron.p2p.connection.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.tron.p2p.base.Parameter;

/** Enforces a fixed STATUS probe deadline after an outbound discovery socket becomes active. */
public final class OutboundStatusTimeoutHandler extends ChannelInboundHandlerAdapter {

  // All state is accessed on the channel's event loop. Reads never restart the deadline.
  private ScheduledFuture<?> timeoutTask;

  /** Also checked before accepting STATUS when the scheduled timeout task has been delayed. */
  public static boolean isTimedOut(Channel socket) {
    OutboundStatusTimeoutHandler handler =
        socket.pipeline().get(OutboundStatusTimeoutHandler.class);
    return !socket.isActive() || (handler != null && handler.deadlineExpired());
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    if (ctx.channel().isActive()) {
      startTimeout(ctx);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    startTimeout(ctx);
    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!ctx.channel().isActive() || deadlineExpired()) {
      ReferenceCountUtil.release(msg);
      ctx.close();
      return;
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    cancelTimeout();
    ctx.fireChannelInactive();
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    cancelTimeout();
    ctx.close();
  }

  private void startTimeout(ChannelHandlerContext ctx) {
    if (timeoutTask == null) {
      timeoutTask = ctx.executor().schedule(() -> ctx.close(),
          Parameter.NODE_DETECT_TIMEOUT, TimeUnit.MILLISECONDS);
    }
  }

  private boolean deadlineExpired() {
    return timeoutTask != null && timeoutTask.getDelay(TimeUnit.NANOSECONDS) <= 0;
  }

  private void cancelTimeout() {
    if (timeoutTask != null) {
      timeoutTask.cancel(false);
    }
  }
}
