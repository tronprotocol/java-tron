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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.core.net.node.NodeImpl;
import org.tron.core.net.peer.PeerConnection;

/**
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Component
@Scope("prototype")
public class TronChannelInitializer extends ChannelInitializer<NioSocketChannel> {

  private static final Logger logger = LoggerFactory.getLogger("TronChannelInitializer");

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  ChannelManager channelManager;

  private NodeImpl p2pNode;

  private String remoteId;

  private boolean peerDiscoveryMode = false;

  public TronChannelInitializer(String remoteId) {
    this.remoteId = remoteId;
  }

  @Override
  public void initChannel(NioSocketChannel ch) throws Exception {
    try {
      final Channel channel = ctx.getBean(PeerConnection.class);

      channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager, p2pNode);

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

  private boolean isInbound() {
    return remoteId == null || remoteId.isEmpty();
  }

  public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
    this.peerDiscoveryMode = peerDiscoveryMode;
  }

  public void setNodeImpl(NodeImpl p2pNode) {
    this.p2pNode = p2pNode;
  }
}
