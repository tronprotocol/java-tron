package org.tron.p2p.connection.socket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.base.Parameter;

@Slf4j(topic = "net")
public class PeerServer {

  private ChannelFuture channelFuture;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public void init() {
    int port = Parameter.p2pConfig.getPort();
    if (port > 0) {
      start(port);
    }
  }

  public void close() {
    try {
      if (channelFuture != null && channelFuture.channel().isOpen()) {
        logger.info("Closing TCP server...");
        channelFuture.channel().close().syncUninterruptibly();
      }
    } catch (Exception e) {
      logger.warn("Closing TCP server failed.", e);
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
      if (bossGroup != null) {
        bossGroup.shutdownGracefully();
      }
    }
  }

  public void start(int port) {
    try {
      bossGroup = new NioEventLoopGroup(1,
          BasicThreadFactory.builder().namingPattern("peerBoss").build());
      //if threads = 0, it is number of core * 2
      workerGroup = new NioEventLoopGroup(Parameter.TCP_NETTY_WORK_THREAD_NUM,
          BasicThreadFactory.builder().namingPattern("peerWorker-%d").build());
      P2pChannelInitializer p2pChannelInitializer = new P2pChannelInitializer("", false, true);
      ServerBootstrap b = new ServerBootstrap();

      b.group(bossGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);

      b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Parameter.NODE_CONNECTION_TIMEOUT);

      b.handler(new LoggingHandler());
      b.childHandler(p2pChannelInitializer);

      // Retain the channel before waiting so an interrupted bind can also be closed.
      channelFuture = b.bind(port);
      channelFuture.sync();
      logger.info("TCP listener started, bind port {}", port);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      close();
      throw new IllegalStateException("Failed to bind TCP listener on port " + port, e);
    }
  }

}
