package org.tron.p2p.connection.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.handshake.DisconnectCode;

@Slf4j(topic = "net")
public final class PendingInboundConnectionHandler extends ChannelInboundHandlerAdapter {

  private static final Map<InetAddress, Integer> pendingByIp = new HashMap<>();
  private static int pendingConnections;

  private final InetAddress address;
  private final AtomicBoolean released = new AtomicBoolean();
  private volatile ScheduledFuture<?> timeoutTask;

  private PendingInboundConnectionHandler(InetAddress address) {
    this.address = address;
  }

  static DisconnectCode tryAdd(Channel socket) {
    InetSocketAddress remoteAddress = (InetSocketAddress) socket.remoteAddress();
    if (remoteAddress == null || remoteAddress.getAddress() == null) {
      return DisconnectCode.UNKNOWN;
    }
    InetAddress address = remoteAddress.getAddress();
    boolean trusted = Parameter.p2pConfig.getTrustNodes().contains(address);
    // Trusted IPs retain the existing ban exemption used by peer admission.
    if (!trusted && ChannelManager.isBanned(address)) {
      return DisconnectCode.TIME_BANNED;
    }
    PendingInboundConnectionHandler handler;
    synchronized (PendingInboundConnectionHandler.class) {
      int count = pendingByIp.getOrDefault(address, 0);
      int limit = trusted ? Parameter.MAX_TOTAL_PENDING_INBOUND_CONNECTIONS
          : Parameter.MAX_PENDING_INBOUND_CONNECTIONS;
      if (count >= Parameter.MAX_PENDING_INBOUND_CONNECTIONS_WITH_SAME_IP
          || pendingConnections >= limit) {
        logger.debug(
            "Reject pending inbound connection from {}, trusted: {}, pending: {}, same IP: {}",
            remoteAddress, trusted, pendingConnections, count);
        return count >= Parameter.MAX_PENDING_INBOUND_CONNECTIONS_WITH_SAME_IP
            ? DisconnectCode.MAX_CONNECTION_WITH_SAME_IP : DisconnectCode.TOO_MANY_PEERS;
      }
      handler = new PendingInboundConnectionHandler(address);
      pendingConnections++;
      pendingByIp.put(address, count + 1);
    }

    // Attach cleanup before installing any protocol handlers, including this one.
    socket.closeFuture().addListener(future -> handler.release());
    socket.pipeline().addLast("pendingInboundHandshake", handler);
    return socket.isOpen() ? DisconnectCode.NORMAL : DisconnectCode.UNKNOWN;
  }

  /**
   * Called on the channel's event loop after HELLO validation and peer admission succeed.
   * Returns false if the connection has closed or its handshake deadline has elapsed.
   * The caller closes the socket after releasing the admission lock on failure.
   */
  public static boolean handshakeCompleted(Channel socket) {
    PendingInboundConnectionHandler handler =
        socket.pipeline().get(PendingInboundConnectionHandler.class);
    if (!socket.isActive() || (handler != null && handler.isTimedOut())) {
      return false;
    }
    if (handler != null) {
      handler.release();
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
    if (!ctx.channel().isActive() || isTimedOut()) {
      ReferenceCountUtil.release(msg);
      ctx.close();
      return;
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    // Removing an unfinished guard must not leave an unaccounted socket open.
    if (!released.get()) {
      ctx.close();
    }
  }

  private void startTimeout(ChannelHandlerContext ctx) {
    if (timeoutTask != null || released.get()) {
      return;
    }
    timeoutTask = ctx.executor().schedule(() -> {
      if (!released.get()) {
        logger.debug("Close channel {}, inbound HELLO handshake timed out",
            ctx.channel().remoteAddress());
        ctx.close();
      }
    }, Parameter.HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (released.get()) {
      timeoutTask.cancel(false);
    }
  }

  private boolean isTimedOut() {
    // Check the deadline even when the event loop has not run the scheduled task yet.
    ScheduledFuture<?> task = timeoutTask;
    return !released.get() && task != null && task.getDelay(TimeUnit.NANOSECONDS) <= 0;
  }

  private void release() {
    if (!released.compareAndSet(false, true)) {
      return;
    }
    ScheduledFuture<?> task = timeoutTask;
    if (task != null) {
      task.cancel(false);
    }
    synchronized (PendingInboundConnectionHandler.class) {
      pendingConnections--;
      int remaining = pendingByIp.get(address) - 1;
      if (remaining == 0) {
        pendingByIp.remove(address);
      } else {
        pendingByIp.put(address, remaining);
      }
    }
  }
}
