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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;


public class MessageHandler extends SimpleChannelInboundHandler<DiscoveryEvent>
    implements Consumer<DiscoveryEvent> {

  static final org.slf4j.Logger logger = LoggerFactory.getLogger("MessageHandler");

  public Channel channel;

  NodeManager nodeManager;

  public MessageHandler(NioDatagramChannel ch, NodeManager nodeManager) {
    channel = ch;
    this.nodeManager = nodeManager;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    nodeManager.channelActivated();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, DiscoveryEvent discoveryEvent)
      throws Exception {
    logger.debug("rcv udp msg type {}, len {} from {} ",
        discoveryEvent.getMessage().getType(),
        discoveryEvent.getMessage().getSendData().length,
        discoveryEvent.getAddress());
    nodeManager.handleInbound(discoveryEvent);
  }

  @Override
  public void accept(DiscoveryEvent discoveryEvent) {
    logger.debug("send udp msg type {}, len {} to {} ",
        discoveryEvent.getMessage().getType(),
        discoveryEvent.getMessage().getSendData().length,
        discoveryEvent.getAddress());
    InetSocketAddress address = discoveryEvent.getAddress();
    sendPacket(discoveryEvent.getMessage().getSendData(), address);
  }

  void sendPacket(byte[] wire, InetSocketAddress address) {
    DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(wire), address);
    channel.write(packet);
    channel.flush();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.info("exception caught, {} {}", ctx.channel().remoteAddress(), cause.getMessage());
    ctx.close();
  }
}
