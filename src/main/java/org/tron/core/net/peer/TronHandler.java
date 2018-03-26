package org.tron.core.net.peer;

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


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.MessageQueue;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol.Block;

/**
 * Process the messages between peers with 'eth' capability on the network<br>
 * Contains common logic to all supported versions
 * delegating version specific stuff to its descendants
 *
 */
public class TronHandler extends SimpleChannelInboundHandler<TronMessage> {

  private final static Logger logger = LoggerFactory.getLogger("TronHandler");

  protected PeerConnection peer;

  private MessageQueue msgQueue = null;

  protected boolean peerDiscoveryMode = false;

  protected Block bestBlock;

  public PeerConnectionDelegate peerDel;

  public TronHandler() {

  }

  public void setPeerDel(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, TronMessage msg) throws InterruptedException {
    //logger.info("tron handle recv msg:" + msg);
    peer.getNodeStatistics().ethInbound.add();
    msgQueue.receivedMessage(msg);

    //handle message
    peerDel.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("Tron handling failed", cause);
    ctx.close();
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    logger.info("handlerRemoved: kill timers in TronHandler");
//    ethereumListener.removeListener(listener);
//    onShutdown();
  }

  public void activate() {
    logger.info("Tron protocol activated");
    peerDel.onConnectPeer(peer);
//    ethereumListener.trace("ETH protocol activated");

  }

  protected void disconnect(ReasonCode reason) {
    msgQueue.disconnect(reason);
    peer.getNodeStatistics().nodeDisconnectedLocal(reason);
  }

  protected void sendMessage(TronMessage message) {
    msgQueue.sendMessage(message);
    peer.getNodeStatistics().ethOutbound.add();
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
    this.peerDiscoveryMode = peerDiscoveryMode;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}