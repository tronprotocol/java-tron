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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.upgrade.UpgradeController;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.handshake.HelloMessage;
import org.tron.p2p.connection.socket.MessageHandler;
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
  @Setter
  private volatile boolean isDisconnect = false;
  @Getter
  @Setter
  private long lastSendTime = System.currentTimeMillis();
  @Getter
  private final long startTime = System.currentTimeMillis();
  @Getter
  private boolean isActive = false;
  @Getter
  private boolean isTrustPeer;
  @Getter
  @Setter
  private volatile boolean finishHandshake;
  @Getter
  @Setter
  private String nodeId;
  @Setter
  @Getter
  private boolean discoveryMode;
  @Getter
  private long avgLatency;
  private long count;

  public void init(ChannelPipeline pipeline, String nodeId, boolean discoveryMode) {
    this.discoveryMode = discoveryMode;
    this.nodeId = nodeId;
    this.isActive = StringUtils.isNotEmpty(nodeId);
    MessageHandler messageHandler = new MessageHandler(this);
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
      log.warn("Loop in causal chain detected");
    }
    SocketAddress address = ctx.channel().remoteAddress();
    if (throwable instanceof ReadTimeoutException
        || throwable instanceof IOException
        || throwable instanceof CorruptedFrameException) {
      log.warn("Close peer {}, reason: {}", address, throwable.getMessage());
    } else if (baseThrowable instanceof P2pException) {
      log.warn("Close peer {}, type: ({}), info: {}",
          address, ((P2pException) baseThrowable).getType(), baseThrowable.getMessage());
    } else {
      log.error("Close peer {}, exception caught", address, throwable);
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

  public void close(long banTime) {
    this.isDisconnect = true;
    this.disconnectTime = System.currentTimeMillis();
    ChannelManager.banNode(this.inetAddress, banTime);
    ctx.close();
  }

  public void close() {
    close(Parameter.DEFAULT_BAN_TIME);
  }

  public void send(Message message) {
    if (message.needToLog()) {
      log.info("Send message to channel {}, {}", inetSocketAddress, message);
    } else {
      log.debug("Send message to channel {}, {}", inetSocketAddress, message);
    }
    send(message.getSendData());
  }

  public void send(byte[] data) {
    try {
      byte type = data[0];
      if (isDisconnect) {
        log.warn("Send to {} failed as channel has closed, message-type:{} ",
            ctx.channel().remoteAddress(), type);
        return;
      }

      if (finishHandshake) {
        data = UpgradeController.codeSendData(version, data);
      }

      ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
      ctx.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess() && !isDisconnect) {
          log.warn("Send to {} failed, message-type:{}, cause:{}",
              ctx.channel().remoteAddress(), ByteArray.byte2int(type),
              future.cause().getMessage());
        }
      });
      setLastSendTime(System.currentTimeMillis());
    } catch (Exception e) {
      log.warn("Send message to {} failed, {}", inetSocketAddress, e.getMessage());
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
