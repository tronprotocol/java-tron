package org.tron.p2p.connection;

import com.google.protobuf.CodedOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/** Bounds pending TCP writes and closes connections whose writes stall. */
@Slf4j(topic = "net")
final class WriteBufferHandler extends WriteTimeoutHandler {

  private static final Budget GLOBAL_BUDGET = new Budget(200L * 1024 * 1024, 500L * 1024 * 1024);

  private final Channel channel;
  private final Budget budget;
  // Guarded by budget, including sends originating outside the channel's event loop.
  private long pendingBytes;
  private boolean closing;

  WriteBufferHandler(Channel channel) {
    this(channel, GLOBAL_BUDGET);
  }

  WriteBufferHandler(Channel channel, Budget budget) {
    super(10);
    this.channel = channel;
    this.budget = budget;
  }

  ChannelFuture writeAndFlush(ChannelHandlerContext ctx, ByteBuf data) {
    // Reserve before Netty can enqueue a write task on the event loop. Include the frame prefix
    // that protoPrepend adds later, after the payload has already been compressed by Channel.send.
    long size = (long) data.readableBytes()
        + CodedOutputStream.computeUInt32SizeNoTag(data.readableBytes());
    ChannelPromise promise = ctx.newPromise();
    promise.setUncancellable();
    if (!budget.reserve(this, size)) {
      data.release();
      return promise.setFailure(new ClosedChannelException());
    }
    promise.addListener(future -> budget.release(this, size));
    if (channel.isDisconnect()) {
      data.release();
      return promise.setFailure(new ClosedChannelException());
    }
    return ctx.writeAndFlush(data, promise);
  }

  private void disconnect() {
    logger.warn("Close peer {}, pending outbound data limit exceeded",
        channel.getInetSocketAddress());
    channel.close();
  }

  /** Shared by TCP connections; the global limit triggers eviction rather than blocking I/O. */
  static final class Budget {

    private final long peerLimit;
    private final long globalLimit;
    private final Set<WriteBufferHandler> peers = new LinkedHashSet<>();
    private long pendingBytes;
    private WriteBufferHandler evicting;

    Budget(long peerLimit, long globalLimit) {
      this.peerLimit = peerLimit;
      this.globalLimit = globalLimit;
    }

    private boolean reserve(WriteBufferHandler handler, long size) {
      WriteBufferHandler victim;
      synchronized (this) {
        if (handler.closing || handler.channel.isDisconnect()) {
          return false;
        }
        handler.pendingBytes += size;
        pendingBytes += size;
        peers.add(handler);
        if (handler.pendingBytes > peerLimit) {
          handler.closing = true;
          victim = handler;
          if (evicting == null) {
            evicting = handler;
          }
        } else {
          victim = selectVictim();
        }
      }
      // Closing may immediately complete writes and re-enter release(). Never close under the lock.
      if (victim != null) {
        victim.disconnect();
      }
      return true;
    }

    private void release(WriteBufferHandler handler, long size) {
      WriteBufferHandler victim;
      synchronized (this) {
        handler.pendingBytes -= size;
        pendingBytes -= size;
        if (handler.pendingBytes == 0) {
          peers.remove(handler);
          if (evicting == handler) {
            evicting = null;
          }
        }
        victim = selectVictim();
      }
      if (victim != null) {
        victim.disconnect();
      }
    }

    // Called with the budget lock held. Wait for the current victim's writes to be released
    // before selecting another, even if multiple producers observe the global limit concurrently.
    private WriteBufferHandler selectVictim() {
      if (evicting != null || pendingBytes < globalLimit) {
        return null;
      }
      WriteBufferHandler largest = null;
      for (WriteBufferHandler candidate : peers) {
        if (!candidate.closing && !candidate.channel.isDisconnect()
            && (largest == null || candidate.pendingBytes > largest.pendingBytes)) {
          largest = candidate;
        }
      }
      if (largest != null) {
        largest.closing = true;
        evicting = largest;
      }
      return largest;
    }
  }
}
