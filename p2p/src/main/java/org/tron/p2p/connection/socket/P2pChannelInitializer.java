package org.tron.p2p.connection.socket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMaxMessagesRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.ConnectionPolicy;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.handshake.HelloMessage;

@Slf4j(topic = "net")
public class P2pChannelInitializer extends ChannelInitializer<NioSocketChannel> {

  private static final int REJECTION_WRITE_TIMEOUT_SECONDS = 2;

  private final String remoteId;

  // TCP STATUS probe mode, independent of UDP discovery.
  private boolean nodeDetectMode = false;

  // Whether regular outbound connection closure notifies the connection pool.
  private boolean trigger = true;

  public P2pChannelInitializer(String remoteId, boolean nodeDetectMode, boolean trigger) {
    this.remoteId = remoteId;
    this.nodeDetectMode = nodeDetectMode;
    this.trigger = trigger;
  }

  @Override
  public void initChannel(NioSocketChannel ch) {
    try {
      if (StringUtils.isEmpty(remoteId)) {
        if (ConnectionPolicy.isBlocked(ch.remoteAddress())) {
          ch.close();
          return;
        }
        DisconnectCode code = PendingInboundConnectionHandler.tryAdd(ch);
        if (code != DisconnectCode.NORMAL) {
          if (code == DisconnectCode.UNKNOWN) {
            ch.close();
          } else {
            sendRejection(ch, code);
          }
          return;
        }
      }
      final Channel channel = new Channel();
      channel.init(ch.pipeline(), remoteId, nodeDetectMode);

      // Keep incomplete pre-HELLO frames from retaining a normal 256 KiB receive buffer.
      ch.config().setRecvByteBufAllocator(new P2pRecvByteBufAllocator(channel));
      ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);

      // Route STATUS-probe and regular peer cleanup separately.
      ch.closeFuture().addListener((ChannelFutureListener) future -> {
        channel.markDisconnected();
        if (channel.isNodeDetectMode() && !channel.isRegisteredPeer()) {
          ChannelManager.getNodeDetectService().notifyDisconnect(channel);
        } else {
          try {
            logger.info("Close channel:{}", channel.getInetSocketAddress());
            ChannelManager.notifyDisconnect(channel);
          } finally {
            if (channel.getInetSocketAddress() != null && channel.isOutbound() && trigger) {
              ChannelManager.triggerConnect(channel.getInetSocketAddress());
            }
          }
        }
      });

    } catch (Exception e) {
      logger.error("Unexpected initChannel error", e);
      ch.close();
    }
  }

  private void sendRejection(NioSocketChannel ch, DisconnectCode code) {
    if (ConnectionPolicy.isBlocked(ch.remoteAddress())) {
      ch.close();
      return;
    }
    ch.config().setAutoRead(false);
    ch.pipeline().addLast("rejectionEncoder", new ProtobufVarint32LengthFieldPrepender());
    ch.pipeline().addLast("rejectionWriteTimeout",
        new WriteTimeoutHandler(REJECTION_WRITE_TIMEOUT_SECONDS));
    HelloMessage response = new HelloMessage(code, System.currentTimeMillis());
    logger.debug("Reject inbound connection from {}, reason: {}", ch.remoteAddress(), code);
    ch.writeAndFlush(Unpooled.wrappedBuffer(response.getSendData()))
        .addListener(ChannelFutureListener.CLOSE);
  }

  /** Limits receive allocations before HELLO without replacing Netty's cached read handle. */
  private static final class P2pRecvByteBufAllocator
      extends DefaultMaxMessagesRecvByteBufAllocator {

    private static final int NORMAL_RECEIVE_BUFFER_SIZE = 256 * 1024;

    private final Channel channel;

    private P2pRecvByteBufAllocator(Channel channel) {
      this.channel = channel;
    }

    @Override
    public Handle newHandle() {
      return new MaxMessageHandle() {
        @Override
        public int guess() {
          return channel.isFinishHandshake()
              ? NORMAL_RECEIVE_BUFFER_SIZE : Parameter.MAX_PRE_HANDSHAKE_LENGTH;
        }
      };
    }
  }
}
