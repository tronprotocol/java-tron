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
package org.tron.common.net.udp.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.discover.node.NodeManager;


public class MessageHandler extends SimpleChannelInboundHandler<UdpEvent>
    implements Consumer<UdpEvent> {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("MessageHandler");

  private Channel channel;

  private EventHandler eventHandler;

  public MessageHandler(NioDatagramChannel channel, EventHandler eventHandler) {
    this.channel = channel;
    this.eventHandler = eventHandler;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    eventHandler.channelActivated();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, UdpEvent udpEvent) {
    logger.debug("rcv udp msg type {}, len {} from {} ",
        udpEvent.getMessage().getType(),
        udpEvent.getMessage().getSendData().length,
        udpEvent.getAddress());
    eventHandler.handleEvent(udpEvent);
  }

  @Override
  public void accept(UdpEvent udpEvent) {
    logger.debug("send udp msg type {}, len {} to {} ",
        udpEvent.getMessage().getType(),
        udpEvent.getMessage().getSendData().length,
        udpEvent.getAddress());
    InetSocketAddress address = udpEvent.getAddress();
    sendPacket(udpEvent.getMessage().getSendData(), address);
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
