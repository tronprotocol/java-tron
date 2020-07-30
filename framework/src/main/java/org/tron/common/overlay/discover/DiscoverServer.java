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

package org.tron.common.overlay.discover;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.net.udp.handler.MessageHandler;
import org.tron.common.net.udp.handler.PacketDecoder;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.WireTrafficStats;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;

@Slf4j(topic = "discover")
@Component
public class DiscoverServer {

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private WireTrafficStats stats;

  private CommonParameter parameter = Args.getInstance();

  private int port = parameter.getNodeListenPort();

  private Channel channel;

  private DiscoveryExecutor discoveryExecutor;

  private volatile boolean shutdown = false;

  @Autowired
  public DiscoverServer(final NodeManager nodeManager) {
    this.nodeManager = nodeManager;
    if (parameter.isNodeDiscoveryEnable() && !parameter.isFastForward()) {
      if (port == 0) {
        logger.error("Discovery can't be started while listen port == 0");
      } else {
        new Thread(() -> {
          try {
            start();
          } catch (Exception e) {
            logger.error("Discovery server start failed.", e);
          }
        }, "DiscoverServer").start();
      }
    }
  }

  public void start() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(parameter.getUdpNettyWorkThreadNum());
    try {
      discoveryExecutor = new DiscoveryExecutor(nodeManager);
      discoveryExecutor.start();
      while (!shutdown) {
        Bootstrap b = new Bootstrap();
        b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<NioDatagramChannel>() {
              @Override
              public void initChannel(NioDatagramChannel ch)
                  throws Exception {
                ch.pipeline().addLast(stats.udp);
                ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                ch.pipeline().addLast(new PacketDecoder());
                MessageHandler messageHandler = new MessageHandler(ch, nodeManager);
                nodeManager.setMessageSender(messageHandler);
                ch.pipeline().addLast(messageHandler);
              }
            });

        channel = b.bind(port).sync().channel();

        logger.info("Discovery server started, bind port {}", port);

        channel.closeFuture().sync();
        if (shutdown) {
          logger.info("Shutdown discovery server");
          break;
        }
        logger.warn("Restart discovery server after 5 sec pause...");
        Thread.sleep(5000);
      }
    } catch (Exception e) {
      logger.error("Start discovery server with port {} failed.", port, e);
    } finally {
      group.shutdownGracefully().sync();
    }
  }

  public void close() {
    logger.info("Closing discovery server...");
    shutdown = true;
    if (channel != null) {
      try {
        channel.close().await(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        logger.info("Closing discovery server failed.", e);
      }
    }

    if (discoveryExecutor != null) {
      try {
        discoveryExecutor.close();
      } catch (Exception e) {
        logger.info("Closing discovery executor failed.", e);
      }
    }
  }

}
