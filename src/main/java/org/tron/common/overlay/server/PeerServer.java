/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.net.node.NodeImpl;

@Component
public class PeerServer {

  private static final Logger logger = LoggerFactory.getLogger("PeerServer");

  private Args args = Args.getInstance();

  private ApplicationContext ctx;

  public TronChannelInitializer tronChannelInitializer;

  private boolean listening;

  @Autowired
  private NodeImpl p2pNode;

  EventLoopGroup bossGroup;
  EventLoopGroup workerGroup;
  ChannelFuture channelFuture;

  @Autowired
  public PeerServer(final Args args, final ApplicationContext ctx) {
    this.ctx = ctx;
  }

  public void start(int port) {

    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup(args.getTcpNettyWorkThreadNum());
    tronChannelInitializer = ctx.getBean(TronChannelInitializer.class, "");

    tronChannelInitializer.setNodeImpl(p2pNode);

    try {
      ServerBootstrap b = new ServerBootstrap();

      b.group(bossGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);

      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.args.getNodeConnectionTimeout());

      b.handler(new LoggingHandler());
      b.childHandler(tronChannelInitializer);

      // Start the client.
      logger.info("TCP listener started, bind port {}", port);

      channelFuture = b.bind(port).sync();

      listening = true;

      // Wait until the connection is closed.
      channelFuture.channel().closeFuture().sync();

      logger.info("TCP listener is closed");

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
