package org.tron.common.overlay.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;

@Slf4j(topic = "net")
@Component
public class PeerServer {

  private CommonParameter parameter = CommonParameter.getInstance();

  private ApplicationContext ctx;

  private boolean listening;

  private ChannelFuture channelFuture;

  @Autowired
  public PeerServer(final Args args, final ApplicationContext ctx) {
    this.ctx = ctx;
  }

  public void start(int port) {

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup(parameter.getTcpNettyWorkThreadNum());
    TronChannelInitializer tronChannelInitializer = ctx.getBean(TronChannelInitializer.class, "");

    try {
      ServerBootstrap b = new ServerBootstrap();

      b.group(bossGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);

      b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.parameter.getNodeConnectionTimeout());

      b.handler(new LoggingHandler());
      b.childHandler(tronChannelInitializer);

      // Start the client.
      logger.info("TCP listener started, bind port {}", port);

      channelFuture = b.bind(port).sync();

      listening = true;

      // Wait until the connection is closed.
      channelFuture.channel().closeFuture().sync();

      logger.info("TCP listener closed");

    } catch (Exception e) {
      logger.error("Start TCP server failed.", e);
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
      listening = false;
    }
  }

  public void close() {
    if (listening && channelFuture != null && channelFuture.channel().isOpen()) {
      try {
        logger.info("Closing TCP server...");
        channelFuture.channel().close().sync();
      } catch (Exception e) {
        logger.warn("Closing TCP server failed.", e);
      }
    }
  }
}
