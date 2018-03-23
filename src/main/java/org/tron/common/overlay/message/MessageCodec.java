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
package org.tron.common.overlay.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.Channel;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessageFactory;


/**
 * The Netty codec which encodes/decodes RPLx frames to subprotocol Messages
 */
@Component
@Scope("prototype")
public class MessageCodec extends ByteToMessageDecoder {

  private static final Logger loggerNet = LoggerFactory.getLogger("net");

  private Channel channel;
  private P2pMessageFactory p2pMessageFactory = new P2pMessageFactory();
  private TronMessageFactory tronMessageFactory = new TronMessageFactory();

  @Autowired
  private MessageCodec(ApplicationContext ctx) {
    //setMaxFramePayloadSize(config.rlpxMaxFrameSize());
  }

  private Message decodeMessage(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException {


    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);
    byte code = encoded[0];
    byte[] payload = ArrayUtils.subarray(encoded, 1, encoded.length);

    Message msg;
    try {
      msg = createMessage(code, payload);
    } catch (Exception ex) {
      loggerNet.info("Incorrectly encoded message from: \t{}, dropping peer", channel);
      loggerNet.info(ex.getMessage());
      channel.disconnect(ReasonCode.BAD_PROTOCOL);
      return null;
    }

    if (loggerNet.isDebugEnabled()) {
      loggerNet.debug("From: {}    Recv:  {}", channel, msg.toString());
    }

    //TODO: let peer know.
    //ethereumListener.onRecvMessage(channel, msg);

    channel.getNodeStatistics().rlpxInMessages.add();
    return msg;
  }

  private Message createMessage(byte code, byte[] payload) {
    if (MessageTypes.inP2pRange(code)) {
      return p2pMessageFactory.create(code, payload);
    }

    if (MessageTypes.inTronRange(code)) {
      return tronMessageFactory.create(code, payload);
    }

    throw new IllegalArgumentException(
        "No such message: " + code + " [" + Hex.toHexString(payload) + "]");
  }


  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
      throws Exception {
    Message message = decodeMessage(ctx, msg);
    out.add(message);
  }


  public void setChannel(Channel channel) {
    this.channel = channel;
  }

}