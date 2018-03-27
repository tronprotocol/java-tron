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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.NodeManager;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.P2pMessageFactory;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.Args;

/**
 * The Netty handler which manages initial negotiation with peer (when either we initiating
 * connection or remote peer initiates)
 *
 * The initial handshake includes: - first AuthInitiate -> AuthResponse messages when peers exchange
 * with secrets - second P2P Hello messages when P2P protocol and subprotocol capabilities are
 * negotiated
 *
 * After the handshake is done this handler reports secrets and other data to the Channel which
 * installs further handlers depending on the protocol parameters. This handler is finally removed
 * from the pipeline.
 */
@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

  private static final Logger loggerWire = LoggerFactory.getLogger("HandshakeHandler");
  private static final Logger loggerNet = LoggerFactory.getLogger("HandshakeHandler");

  private final ECKey myKey;
  private byte[] remoteId;
  private byte[] initiatePacket;
  private Channel channel;
  private boolean isHandshakeDone;
  private boolean isInitiator = false;

  private final Args args = Args.getInstance();
  private final NodeManager nodeManager;

  @Autowired
  public HandshakeHandler(final Args args, final NodeManager nodeManager) {
    this.nodeManager = nodeManager;
    myKey = this.args.getMyKey();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    loggerWire.info("&&&&&&&&&&&&&& channelActive");
    channel.setInetSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());
    if (remoteId.length == 64) {
      channel.initWithNode(remoteId);
      initiate(ctx);
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    loggerWire.info("Decoding handshake... (" + in.readableBytes() + " bytes available)");
    decodeHandshake(ctx, in);
    if (isHandshakeDone) {
      loggerWire.debug("Handshake done, removing HandshakeHandler from pipeline.");
      ctx.pipeline().remove(this);
    }

  }

  public void initiate(ChannelHandlerContext ctx) throws Exception {
    loggerNet.debug("initiator activated");
    isInitiator = true;

    //TODO: send hello message here
//    final ByteBuf byteBufMsg = ctx.alloc().buffer(initiatePacket.length);
//    byteBufMsg.writeBytes(initiatePacket);
//    ctx.writeAndFlush(byteBufMsg).sync();
    channel.sendHelloMessage(ctx);

    channel.getNodeStatistics().rlpxAuthMessagesSent.add();
  }

  // consume handshake, producing no resulting message to upper layers
  private void decodeHandshake(final ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {

    P2pMessageFactory factory = new P2pMessageFactory();

    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);

    P2pMessage msg = factory.create(encoded);

    if (isInitiator) {
      loggerWire.debug("initiator");

      if (msg instanceof HelloMessage) {

        if (((HelloMessage) msg).getVersion() != Args.getInstance().getNodeP2pVersion()) {
          channel.disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
          return;
        }

        isHandshakeDone = true;
        this.channel.publicHandshakeFinished(ctx, (HelloMessage) msg);
      } else {
        channel.getNodeStatistics()
            .nodeDisconnectedRemote(ReasonCode.fromInt(((DisconnectMessage) msg).getReason()));
      }

    } else {
      loggerWire.debug("Not initiator.");

      if (msg instanceof DisconnectMessage) {
        loggerNet.debug("Active remote peer disconnected right after handshake.");
        return;
      }

      if (!(msg instanceof HelloMessage)) {
        throw new RuntimeException("The message type is not HELLO or DISCONNECT: " + msg);
      }

      final HelloMessage inboundHelloMessage = (HelloMessage) msg;

      if (((HelloMessage) msg).getVersion() != Args.getInstance().getNodeP2pVersion()) {
        channel.disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
        return;
      }

      this.remoteId = ByteArray.fromHexString(inboundHelloMessage.getPeerId());
      loggerNet.info("getPeerId:" + inboundHelloMessage.getPeerId());
      // now we know both remote nodeId and port
      // let's set node, that will cause registering node in NodeManager
      channel.initWithNode(remoteId, inboundHelloMessage.getListenPort());
      channel.sendHelloMessage(ctx);
      isHandshakeDone = true;
      this.channel.publicHandshakeFinished(ctx, inboundHelloMessage);
      channel.getNodeStatistics().rlpxInHello.add();
    }
  }

  public void setRemoteId(String remoteId, Channel channel) {
    this.remoteId = Hex.decode(remoteId);
    this.channel = channel;
  }

  public byte[] getRemoteId() {
    return remoteId;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (channel.isDiscoveryMode()) {
      loggerNet.info("Handshake failed: " + cause);
    } else {
      if (cause instanceof IOException || cause instanceof ReadTimeoutException) {
        loggerNet.info("Handshake failed: " + ctx.channel().remoteAddress() + ": " + cause);
      } else {
        loggerNet.info("Handshake failed: ", cause);
      }
    }
    ctx.close();
  }
}
