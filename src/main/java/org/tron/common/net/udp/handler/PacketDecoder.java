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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.tron.common.net.udp.message.Message;

public class PacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("PacketDecoder");

  private final int maxSize = 2048;

  @Override
  public void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
    ByteBuf buf = packet.content();
    int length = buf.readableBytes();
    if (length <= 1 || length >= maxSize) {
      logger.error("UDP rcv bad packet, from {} length = {}", ctx.channel().remoteAddress(), length);
      return;
    }
    byte[] encoded = new byte[length];
    buf.readBytes(encoded);
    try {
      UdpEvent event = new UdpEvent(Message.parse(encoded), packet.sender());
      out.add(event);
    } catch (Exception e) {
      logger.error("Parse msg failed, type {}, len {}, address {}", encoded[0], encoded.length,
          packet.sender());
    }
  }
}
