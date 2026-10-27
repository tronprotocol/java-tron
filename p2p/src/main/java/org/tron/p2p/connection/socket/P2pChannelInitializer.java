package org.tron.p2p.connection.socket;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;

@Slf4j(topic = "net")
public class P2pChannelInitializer extends ChannelInitializer<NioSocketChannel> {

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
      if (StringUtils.isEmpty(remoteId) && !PendingInboundConnectionHandler.tryAdd(ch)) {
        ch.close();
        return;
      }
      final Channel channel = new Channel();
      channel.init(ch.pipeline(), remoteId, nodeDetectMode);

      // limit the size of receiving buffer to 1024
      ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
      ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
      ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

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

}
