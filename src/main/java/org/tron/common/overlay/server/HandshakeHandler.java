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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.P2pMessageFactory;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

  private static final Logger logger = LoggerFactory.getLogger("HandshakeHandler");

  private byte[] remoteId;

  private Channel channel;

  private final NodeManager nodeManager;

  private final ChannelManager channelManager;

  private Manager manager;

  private  P2pMessageFactory messageFactory = new P2pMessageFactory();

  @Autowired
  private SyncPool syncPool;

  @Autowired
  public HandshakeHandler(final NodeManager nodeManager, final ChannelManager channelManager,
      final Manager manager) {
    this.nodeManager = nodeManager;
    this.channelManager = channelManager;
    this.manager = manager;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("channel active, {}", ctx.channel().remoteAddress());
    channel.setChannelHandlerContext(ctx);
    if (remoteId.length == 64) {
      channel.initNode(remoteId, ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
      sendHelloMsg(ctx, System.currentTimeMillis());
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);
    P2pMessage msg = messageFactory.create(encoded);

    logger.info("Handshake Receive from {}, {}", ctx.channel().remoteAddress(), msg);

    switch (msg.getType()) {
      case P2P_HELLO:
        handleHelloMsg(ctx, (HelloMessage)msg);
        break;
      case P2P_DISCONNECT:
        if (channel.getNodeStatistics() != null){
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

  private void sendHelloMsg(ChannelHandlerContext ctx, long time){

    HelloMessage message = new HelloMessage(nodeManager.getPublicHomeNode(), time,
            manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    ctx.writeAndFlush(message.getSendData());
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
    logger.info("Handshake Send to {}, {} ", ctx.channel().remoteAddress(), message);
  }

  private void handleHelloMsg(ChannelHandlerContext ctx, HelloMessage msg) {
    if (remoteId.length != 64) {
      channel.initNode(msg.getFrom().getId(), msg.getFrom().getPort());
      InetAddress address = ((InetSocketAddress)ctx.channel().remoteAddress()).getAddress();
      if (!channelManager.getTrustPeers().keySet().contains(address) && !syncPool.isCanConnect()) {
        channel.disconnect(ReasonCode.TOO_MANY_PEERS);
        return;
      }
    }

    if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
      logger.info("Peer {} different p2p version, peer->{}, me->{}",
              ctx.channel().remoteAddress(), msg.getVersion(), Args.getInstance().getNodeP2pVersion());
      channel.disconnect(ReasonCode.INCOMPATIBLE_VERSION);
      return;
    }

    if (!Arrays.equals(manager.getGenesisBlockId().getBytes(), msg.getGenesisBlockId().getBytes())){
      logger.info("Peer {} different genesis block, peer->{}, me->{}", ctx.channel().remoteAddress(),
              msg.getGenesisBlockId().getString(), manager.getGenesisBlockId().getString());
      channel.disconnect(ReasonCode.INCOMPATIBLE_CHAIN);
      return;
    }

    if (manager.getSolidBlockId().getNum() >= msg.getSolidBlockId().getNum() && !manager.containBlockInMainChain(msg.getSolidBlockId())){
      logger.info("Peer {} different solid block, peer->{}, me->{}", ctx.channel().remoteAddress(),
              msg.getSolidBlockId().getString(), manager.getSolidBlockId().getString());
      channel.disconnect(ReasonCode.FORKED);
      return;
    }

    ((PeerConnection)channel).setHelloMessage(msg);

    channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);

    channel.publicHandshakeFinished(ctx, msg);
    if (!channelManager.processPeer(channel)) {
      return;
    }

    if (remoteId.length != 64) {
      sendHelloMsg(ctx, msg.getTimestamp());
    }

    syncPool.onConnect(channel);
  }
}
