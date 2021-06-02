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

package org.tron.core.ibc.connect;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.net.InetSocketAddress;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.P2pMessageFactory;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;

@Slf4j(topic = "net-cross")
@Component
@Scope("prototype")
public class CrossChainHandshakeHandler extends ByteToMessageDecoder {

  private Channel channel;

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  private byte[] remoteId;

  private P2pMessageFactory messageFactory = new P2pMessageFactory();

  @Autowired
  private CrossChainConnectPool crossChainConnectPool;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("cross chain channel active, {}", ctx.channel().remoteAddress());
    channel.setChannelHandlerContext(ctx);
    if (remoteId.length == 64) {
      channel.initNode(remoteId, ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
      sendHelloMsg(ctx, System.currentTimeMillis());
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
      throws Exception {
    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);
    P2pMessage msg = messageFactory.create(encoded);

    logger.info("cross chain handshake Receive from {}, {}", ctx.channel().remoteAddress(), msg);

    switch (msg.getType()) {
      case P2P_HELLO:
        handleHelloMsg(ctx, (HelloMessage) msg);
        break;
      case P2P_DISCONNECT:
        if (channel.getNodeStatistics() != null) {
          channel.getNodeStatistics()
              .nodeDisconnectedRemote(((DisconnectMessage) msg).getReasonCode());
        }
        channel.close();
        break;
      default:
        channel.close();
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    channel.processException(cause);
  }

  public void setChannel(Channel channel, String remoteId) {
    this.channel = channel;
    this.remoteId = Hex.decode(remoteId);
  }

  protected void sendHelloMsg(ChannelHandlerContext ctx, long time) {
    Node homeNode = nodeManager.getPublicHomeNode();
    int crossChainPort = Args.getInstance().getCrossChainPort();
    Node node = new Node(homeNode.getId(), homeNode.getHost(), crossChainPort);
    HelloMessage message = new HelloMessage(node, time,
        chainBaseManager.getGenesisBlockId(), chainBaseManager.getSolidBlockId(),
        chainBaseManager.getHeadBlockId(), true);
    ctx.writeAndFlush(message.getSendData());
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
    logger.info("cross chain handshake Send to {}, {} ", ctx.channel().remoteAddress(), message);
  }

  private synchronized void handleHelloMsg(ChannelHandlerContext ctx, HelloMessage msg) {

    channel.initNode(msg.getFrom().getId(), msg.getFrom().getPort());

    //todo:verify message，Disconnect if the conditions are not met

    ((PeerConnection) channel).setHelloMessage(msg);

    channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);

    channel.publicHandshakeFinished(ctx, msg);
    if (!channelManager.processCrossChainPeer(msg.getChainId(), channel)) {
      return;
    }

    if (remoteId.length != 64) {
      sendHelloMsg(ctx, msg.getTimestamp());
    }

    crossChainConnectPool.onConnect(msg.getChainId(), channel);
  }
}
