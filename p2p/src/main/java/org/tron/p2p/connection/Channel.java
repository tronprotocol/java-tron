package org.tron.p2p.connection;

import com.google.common.base.Throwables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.socket.MessageHandler;
import org.tron.p2p.connection.socket.OutboundHandshakeTimeoutHandler;
import org.tron.p2p.connection.socket.OutboundStatusTimeoutHandler;
import org.tron.p2p.connection.socket.P2pProtobufVarint32FrameDecoder;
import org.tron.p2p.discover.Node;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.stats.TrafficStats;
import org.tron.p2p.utils.ByteArray;

@Slf4j(topic = "net")
public class Channel {

  public volatile boolean waitForPong = false;
  public volatile long pingSent = System.currentTimeMillis();

  @Getter
  private HelloMessage helloMessage;
  @Getter
  private Node node;
  @Getter
  private int version;
  @Getter
  private ChannelHandlerContext ctx;
  @Getter
  private InetSocketAddress inetSocketAddress;
  @Getter
  private InetAddress inetAddress;
  @Getter
  private volatile long disconnectTime;
  @Getter
  private volatile boolean isDisconnect = false;
  @Getter
  @Setter
  private long lastSendTime = System.currentTimeMillis();
  @Getter
  private final long startTime = System.currentTimeMillis();
  /** Whether the local node initiated this connection. */
  @Getter
  private boolean outbound = false;
  @Getter
  private boolean isTrustPeer;
  @Getter
  @Setter
  private volatile boolean finishHandshake;
  // Remains true once registered as a peer, ensuring peer cleanup even if nodeDetectMode changes.
  @Getter
  private volatile boolean registeredPeer;
  private final AtomicBoolean disconnectNotified = new AtomicBoolean();
  @Getter
  @Setter
  private String nodeId;
  /** TCP STATUS probe mode, independent of the UDP discovery configuration. */
  @Setter
  @Getter
  private boolean nodeDetectMode;
  @Getter
  private long avgLatency;
  private long count;

  /** @deprecated Use {@link #isOutbound()} to query connection direction. */
  @Deprecated
  public boolean isActive() {
    return isOutbound();
  }

  /** @deprecated Use {@link #isNodeDetectMode()}. */
  @Deprecated
  public boolean isDiscoveryMode() {
    return isNodeDetectMode();
  }

  /** @deprecated Use {@link #setNodeDetectMode(boolean)}. */
  @Deprecated
  public void setDiscoveryMode(boolean nodeDetectMode) {
    setNodeDetectMode(nodeDetectMode);
  }

  public void init(ChannelPipeline pipeline, String nodeId, boolean nodeDetectMode) {
    this.nodeDetectMode = nodeDetectMode;
    this.nodeId = nodeId;
    this.outbound = StringUtils.isNotEmpty(nodeId);
    MessageHandler messageHandler = new MessageHandler(this);
    if (outbound) {
      if (nodeDetectMode) {
        pipeline.addLast("outboundStatusTimeout", new OutboundStatusTimeoutHandler());
      } else {
        pipeline.addLast("outboundHandshakeTimeout", new OutboundHandshakeTimeoutHandler());
      }
    }
    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60, TimeUnit.SECONDS));
    pipeline.addLast(TrafficStats.tcp);
    pipeline.addLast("protoPrepend", new ProtobufVarint32LengthFieldPrepender());
    pipeline.addLast("protoDecode", new P2pProtobufVarint32FrameDecoder(this));
    pipeline.addLast("messageHandler", messageHandler);
  }

  public void processException(Throwable throwable) {
    Throwable baseThrowable = throwable;
    try {
      baseThrowable = Throwables.getRootCause(baseThrowable);
    } catch (IllegalArgumentException e) {
      baseThrowable = e.getCause();
      logger.warn("Loop in causal chain detected");
    }
    SocketAddress address = ctx.channel().remoteAddress();
    if (throwable instanceof ReadTimeoutException
        || throwable instanceof IOException
        || throwable instanceof CorruptedFrameException) {
      logger.warn("Close peer {}, reason: {}", address, throwable.getMessage());
    } else if (baseThrowable instanceof P2pException) {
      logger.warn("Close peer {}, type: ({}), info: {}",
          address, ((P2pException) baseThrowable).getType(), baseThrowable.getMessage());
    } else {
      logger.error("Close peer {}, exception caught", address, throwable);
    }
    close();
  }

  public void setHelloMessage(HelloMessage helloMessage) {
    this.helloMessage = helloMessage;
    this.node = helloMessage.getFrom();
    this.nodeId = node.getHexId(); //update node id from handshake
    this.version = helloMessage.getVersion();
  }

  public void setChannelHandlerContext(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    this.inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    this.inetAddress = inetSocketAddress.getAddress();
    this.isTrustPeer = Parameter.p2pConfig.getTrustNodes().contains(inetAddress);
  }

  void markRegisteredPeer() {
    registeredPeer = true;
  }

  boolean markDisconnectNotified() {
    return disconnectNotified.compareAndSet(false, true);
  }

  /** Marks the channel as disconnected and records the first disconnect time. */
  public synchronized void markDisconnected() {
    isDisconnect = true;
    if (disconnectTime == 0) {
      disconnectTime = System.currentTimeMillis();
    }
  }

  /**
   * Closes with an explicit penalty, extending the IP ban if the requested expiry is later.
   *
   * @param banTime penalty duration in milliseconds, measured from this call
   */
  public void close(long banTime) {
    markDisconnected();
    ChannelManager.banNode(this.inetAddress, banTime);
    ctx.close();
  }

  /**
   * Closes with the default disconnect cooldown, preserving any active IP ban.
   * Unlike {@link #close(long)}, repeat disconnect won't extend the ban time if not expired.
   */
  public void close() {
    markDisconnected();
    ChannelManager.banNodeOnDisconnect(this.inetAddress);
    ctx.close();
  }

  public void send(Message message) {
    if (message.needToLog()) {
      logger.info("Send message to channel {}, {}", inetSocketAddress, message);
    } else {
      logger.debug("Send message to channel {}, {}", inetSocketAddress, message);
    }
    send(message.getSendData());
  }

  public void send(byte[] data) {
    try {
      byte type = data[0];
      if (isDisconnect) {
        logger.warn("Send to {} failed as channel has closed, message-type:{} ",
            ctx.channel().remoteAddress(), type);
        return;
      }

      if (finishHandshake) {
        data = UpgradeController.codeSendData(version, data);
      }

      ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
      ctx.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess() && !isDisconnect) {
          logger.warn("Send to {} failed, message-type:{}, cause:{}",
              ctx.channel().remoteAddress(), ByteArray.byte2int(type),
              future.cause().getMessage());
        }
      });
      setLastSendTime(System.currentTimeMillis());
    } catch (Exception e) {
      logger.warn("Send message to {} failed, {}", inetSocketAddress, e.getMessage());
      ctx.channel().close();
    }
  }

  public void updateAvgLatency(long latency) {
    long total = this.avgLatency * this.count;
    this.count++;
    this.avgLatency = (total + latency) / this.count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Channel channel = (Channel) o;
    return Objects.equals(inetSocketAddress, channel.inetSocketAddress);
  }

  @Override
  public int hashCode() {
    return inetSocketAddress.hashCode();
  }

  @Override
  public String toString() {
    return String.format("%s | %s", inetSocketAddress,
        StringUtils.isEmpty(nodeId) ? "<null>" : nodeId);
  }

}
