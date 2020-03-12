package org.tron.common.overlay.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.core.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class TronChannelInitializer extends ChannelInitializer<NioSocketChannel> {

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private ChannelManager channelManager;

  private String remoteId;

  private boolean peerDiscoveryMode = false;

  public TronChannelInitializer(String remoteId) {
    this.remoteId = remoteId;
  }

  @Override
  public void initChannel(NioSocketChannel ch) throws Exception {
    try {
      final Channel channel = ctx.getBean(PeerConnection.class);

      channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager);

      // limit the size of receiving buffer to 1024
      ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
      ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
      ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

      // be aware of channel closing
      ch.closeFuture().addListener((ChannelFutureListener) future -> {
        logger.info("Close channel:" + channel);
        if (!peerDiscoveryMode) {
          channelManager.notifyDisconnect(channel);
        }
      });

    } catch (Exception e) {
      logger.error("Unexpected error: ", e);
    }
  }

  public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
    this.peerDiscoveryMode = peerDiscoveryMode;
  }
}
