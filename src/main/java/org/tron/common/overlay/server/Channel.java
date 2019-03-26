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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.discover.node.statistics.NodeStatistics;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.MessageCodec;
import org.tron.common.overlay.message.StaticMessages;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.exception.P2pException;
import org.tron.core.net.TronNetHandler;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class Channel {

  @Autowired
  protected MessageQueue msgQueue;

  @Autowired
  private MessageCodec messageCodec;

  @Autowired
  private NodeManager nodeManager;

  @Autowired
  private StaticMessages staticMessages;

  @Autowired
  private WireTrafficStats stats;

  @Autowired
  private HandshakeHandler handshakeHandler;

  @Autowired
  private P2pHandler p2pHandler;

  @Autowired
  private TronNetHandler tronNetHandler;

  private ChannelManager channelManager;

  private ChannelHandlerContext ctx;

  private InetSocketAddress inetSocketAddress;

  private Node node;

  private long startTime;

  private TronState tronState = TronState.INIT;

  protected NodeStatistics nodeStatistics;

  private boolean isActive;

  private volatile boolean isDisconnect;

  private String remoteId;

  private PeerStatistics peerStats = new PeerStatistics();

  private boolean isTrustPeer;

  private boolean isFastForwardPeer;

  public void init(ChannelPipeline pipeline, String remoteId, boolean discoveryMode,
      ChannelManager channelManager) {

    this.channelManager = channelManager;

    this.remoteId = remoteId;

    isActive = remoteId != null && !remoteId.isEmpty();

    startTime = System.currentTimeMillis();

    //TODO: use config here
    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60, TimeUnit.SECONDS));
    pipeline.addLast(stats.tcp);
    pipeline.addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
    pipeline.addLast("lengthDecode", new TrxProtobufVarint32FrameDecoder(this));

    //handshake first
    pipeline.addLast("handshakeHandler", handshakeHandler);

    messageCodec.setChannel(this);
    msgQueue.setChannel(this);
    handshakeHandler.setChannel(this, remoteId);
    p2pHandler.setChannel(this);
    tronNetHandler.setChannel(this);

    p2pHandler.setMsgQueue(msgQueue);
    tronNetHandler.setMsgQueue(msgQueue);
  }

  public void publicHandshakeFinished(ChannelHandlerContext ctx, HelloMessage msg) {
    isTrustPeer = channelManager.getTrustNodes().containsKey(getInetAddress());
    isFastForwardPeer = channelManager.getFastForwardNodes().containsKey(getInetAddress());
    ctx.pipeline().remove(handshakeHandler);
    msgQueue.activate(ctx);
    ctx.pipeline().addLast("messageCodec", messageCodec);
    ctx.pipeline().addLast("p2p", p2pHandler);
    ctx.pipeline().addLast("data", tronNetHandler);
    setStartTime(msg.getTimestamp());
    setTronState(TronState.HANDSHAKE_FINISHED);
    getNodeStatistics().p2pHandShake.add();
    logger.info("Finish handshake with {}.", ctx.channel().remoteAddress());
  }

  /**
   * Set node and register it in NodeManager if it is not registered yet.
   */
  public void initNode(byte[] nodeId, int remotePort) {
    node = new Node(nodeId, inetSocketAddress.getHostString(), remotePort);
    nodeStatistics = nodeManager.getNodeStatistics(node);
    nodeManager.getNodeHandler(node).setNode(node);
  }

  public void disconnect(ReasonCode reason) {
    this.isDisconnect = true;
    channelManager.processDisconnect(this, reason);
    DisconnectMessage msg = new DisconnectMessage(reason);
    logger.info("Send to {} online-time {}s, {}",
        ctx.channel().remoteAddress(),
        (System.currentTimeMillis() - startTime) / 1000,
        msg);
    getNodeStatistics().nodeDisconnectedLocal(reason);
    ctx.writeAndFlush(msg.getSendData()).addListener(future -> close());
  }

  public void processException(Throwable throwable) {
    Throwable baseThrowable = throwable;
    while (baseThrowable.getCause() != null) {
      baseThrowable = baseThrowable.getCause();
    }
    SocketAddress address = ctx.channel().remoteAddress();
    if (throwable instanceof ReadTimeoutException ||
        throwable instanceof IOException) {
      logger.warn("Close peer {}, reason: {}", address, throwable.getMessage());
    } else if (baseThrowable instanceof P2pException) {
      logger.warn("Close peer {}, type: {}, info: {}",
          address, ((P2pException) baseThrowable).getType(), baseThrowable.getMessage());
    } else {
      logger.error("Close peer {}, exception caught", address, throwable);
    }
    close();
  }

  public void close() {
    this.isDisconnect = true;
    p2pHandler.close();
    msgQueue.close();
    ctx.close();
  }

  public enum TronState {
    INIT,
    HANDSHAKE_FINISHED,
    START_TO_SYNC,
    SYNCING,
    SYNC_COMPLETED,
    SYNC_FAILED
  }

  public PeerStatistics getPeerStats() {
    return peerStats;
  }

  public Node getNode() {
    return node;
  }

  public byte[] getNodeId() {
    return node == null ? null : node.getId();
  }

  public ByteArrayWrapper getNodeIdWrapper() {
    return node == null ? null : new ByteArrayWrapper(node.getId());
  }

  public String getPeerId() {
    return node == null ? "<null>" : node.getHexId();
  }

  public void setChannelHandlerContext(ChannelHandlerContext ctx) {
    this.ctx = ctx;
    this.inetSocketAddress = ctx == null ? null : (InetSocketAddress) ctx.channel().remoteAddress();
  }

  public InetAddress getInetAddress() {
    return ctx == null ? null : ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
  }

  public NodeStatistics getNodeStatistics() {
    return nodeStatistics;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setTronState(TronState tronState) {
    this.tronState = tronState;
    logger.info("Peer {} status change to {}.", inetSocketAddress, tronState);
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isDisconnect() {
    return isDisconnect;
  }

  public boolean isTrustPeer() {
    return isTrustPeer;
  }

  public boolean isFastForwardPeer() {
    return isFastForwardPeer;
  }


  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Channel channel = (Channel) o;
    if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress)
        : channel.inetSocketAddress != null) {
      return false;
    }
    if (node != null ? !node.equals(channel.node) : channel.node != null) {
      return false;
    }
    return this == channel;
  }

  @Override
  public int hashCode() {
    int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
    result = 31 * result + (node != null ? node.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s | %s", inetSocketAddress, getPeerId());
  }

}

