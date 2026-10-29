package org.tron.p2p.connection.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.base.Parameter;

/**
 * Enforces a fixed HELLO deadline after a non-discovery outbound socket becomes active.
 * Uses {@link Parameter#HANDSHAKE_TIMEOUT_SECONDS} without refreshing the deadline on reads.
 */
@Slf4j(topic = "net")
public final class OutboundHandshakeTimeoutHandler extends ChannelInboundHandlerAdapter {

  // All state is accessed on the channel's event loop.
  private ScheduledFuture<?> timeoutTask;
  private boolean handshakeComplete;

  /**
   * Called on the event loop after HELLO validation and peer admission succeed.
   * The caller closes the socket after releasing the admission lock if this returns false.
   */
  public static boolean handshakeCompleted(Channel socket) {
    OutboundHandshakeTimeoutHandler handler =
        socket.pipeline().get(OutboundHandshakeTimeoutHandler.class);
    if (!socket.isActive() || (handler != null && handler.isTimedOut())) {
      return false;
    }
    if (handler != null) {
      handler.handshakeComplete = true;
      socket.pipeline().remove(handler);
    }
    return true;
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
    // Enforce the deadline even if the scheduled task has not run yet.
    if (!ctx.channel().isActive() || isTimedOut()) {
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
    if (!handshakeComplete) {
      ctx.close();
    }
  }

  private void startTimeout(ChannelHandlerContext ctx) {
    if (timeoutTask == null && !handshakeComplete) {
      timeoutTask = ctx.executor().schedule(() -> {
        logger.debug("Close channel {}, outbound HELLO handshake timed out",
            ctx.channel().remoteAddress());
        ctx.close();
      }, Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
  }

  private boolean isTimedOut() {
    return timeoutTask != null && timeoutTask.getDelay(TimeUnit.NANOSECONDS) <= 0;
  }

  private void cancelTimeout() {
    if (timeoutTask != null) {
      timeoutTask.cancel(false);
    }
  }
}
