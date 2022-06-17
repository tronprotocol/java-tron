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
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.P2pMessageFactory;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

  private Channel channel;

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private Manager manager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private FastForward fastForward;

  private byte[] remoteId;

  private P2pMessageFactory messageFactory = new P2pMessageFactory();

  @Autowired
  private SyncPool syncPool;

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
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
      throws Exception {
    byte[] encoded = new byte[buffer.readableBytes()];
    buffer.readBytes(encoded);
    P2pMessage msg = messageFactory.create(encoded);

    logger.info("Handshake receive from {}, {}", ctx.channel().remoteAddress(), msg);

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
    HelloMessage message = new HelloMessage(
            nodeManager.getPublicHomeNode(), time, chainBaseManager);
    fastForward.fillHelloMessage(message, channel);
    ((PeerConnection) channel).setHelloMessageSend(message);
    ctx.writeAndFlush(message.getSendData());
    int length = message.getSendData().readableBytes();
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
    MetricsUtil.meterMark(MetricsKey.NET_TCP_OUT_TRAFFIC, length);
    Metrics.histogramObserve(MetricKeys.Histogram.TCP_BYTES, length,
        MetricLabels.Histogram.TRAFFIC_OUT);

    logger.info("Handshake send to {}, {} ", ctx.channel().remoteAddress(), message);
  }

  private void handleHelloMsg(ChannelHandlerContext ctx, HelloMessage msg) {
    channel.initNode(msg.getFrom().getId(), msg.getFrom().getPort());

    if (!msg.valid()) {
      logger.warn("Peer {} invalid hello message parameters, "
                      + "GenesisBlockId: {}, SolidBlockId: {}, HeadBlockId: {}",
              ctx.channel().remoteAddress(),
              ByteArray.toHexString(msg.getInstance().getGenesisBlockId().getHash().toByteArray()),
              ByteArray.toHexString(msg.getInstance().getSolidBlockId().getHash().toByteArray()),
              ByteArray.toHexString(msg.getInstance().getHeadBlockId().getHash().toByteArray()));
      channel.disconnect(ReasonCode.UNEXPECTED_IDENTITY);
      return;
    }

    channel.setAddress(msg.getHelloMessage().getAddress());

    if (!fastForward.checkHelloMessage(msg, channel)) {
      channel.disconnect(ReasonCode.UNEXPECTED_IDENTITY);
      return;
    }

    InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
    if (remoteId.length != 64
        && channelManager.getTrustNodes().getIfPresent(address) == null
        && !syncPool.isCanConnect()) {
      channel.disconnect(ReasonCode.TOO_MANY_PEERS);
      return;
    }

    long headBlockNum = chainBaseManager.getHeadBlockNum();
    long lowestBlockNum =  msg.getLowestBlockNum();
    if (lowestBlockNum > headBlockNum) {
      logger.info("Peer {} miss block, lowestBlockNum:{}, headBlockNum:{}",
              ctx.channel().remoteAddress(), lowestBlockNum, headBlockNum);
      channel.disconnect(ReasonCode.LIGHT_NODE_SYNC_FAIL);
      return;
    }

    if (msg.getVersion() != Args.getInstance().getNodeP2pVersion()) {
      logger.info("Peer {} different p2p version, peer->{}, me->{}",
          ctx.channel().remoteAddress(), msg.getVersion(), Args.getInstance().getNodeP2pVersion());
      channel.disconnect(ReasonCode.INCOMPATIBLE_VERSION);
      return;
    }

    if (!Arrays
        .equals(chainBaseManager.getGenesisBlockId().getBytes(),
            msg.getGenesisBlockId().getBytes())) {
      logger
          .info("Peer {} different genesis block, peer->{}, me->{}", ctx.channel().remoteAddress(),
              msg.getGenesisBlockId().getString(),
              chainBaseManager.getGenesisBlockId().getString());
      channel.disconnect(ReasonCode.INCOMPATIBLE_CHAIN);
      return;
    }

    if (chainBaseManager.getSolidBlockId().getNum() >= msg.getSolidBlockId().getNum()
        && !chainBaseManager.containBlockInMainChain(msg.getSolidBlockId())) {
      logger.info("Peer {} different solid block, peer->{}, me->{}", ctx.channel().remoteAddress(),
          msg.getSolidBlockId().getString(), chainBaseManager.getSolidBlockId().getString());
      channel.disconnect(ReasonCode.FORKED);
      return;
    }

    if (msg.getFrom().getHost().equals(address.getHostAddress())) {
      channelManager.getHelloMessageCache().put(msg.getFrom().getHost(), msg.getHelloMessage());
    }

    ((PeerConnection) channel).setHelloMessageReceive(msg);

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
