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

import static org.tron.common.overlay.message.StaticMessages.PING_MESSAGE;
import static org.tron.common.overlay.message.StaticMessages.PONG_MESSAGE;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.P2pMessageCodes;
import org.tron.common.overlay.message.ReasonCode;
import org.tron.core.config.args.Args;


/**
 * Process the basic protocol messages between every peer on the network.
 *
 * Peers can send/receive <ul> <li>HELLO       :   Announce themselves to the network</li>
 * <li>DISCONNECT  :   Disconnect themselves from the network</li> <li>GET_PEERS   :   Request a
 * list of other knows peers</li> <li>PEERS       :   Send a list of known peers</li> <li>PING
 *  :   Check if another peer is still alive</li> <li>PONG        :   Confirm that they themselves
 * are still alive</li> </ul>
 */
@Component
@Scope("prototype")
public class
P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

  public final static byte VERSION = 5;

  private final static Logger logger = LoggerFactory.getLogger("P2pHandler");

  private static ScheduledExecutorService pingTimer =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "P2pPingTimer"));

  private MessageQueue msgQueue;

  private boolean peerDiscoveryMode = false;

  //private HelloMessage handshakeHelloMessage = null;

  private int ethInbound;
  private int ethOutbound;

  @Autowired
  Args args;

  private Channel channel;
  private ScheduledFuture<?> pingTask;


  public P2pHandler() {
    this.peerDiscoveryMode = false;
  }

    public P2pHandler(MessageQueue msgQueue, boolean peerDiscoveryMode) {
        this.msgQueue = msgQueue;
        this.peerDiscoveryMode = peerDiscoveryMode;
    }


  public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
    this.peerDiscoveryMode = peerDiscoveryMode;
  }


  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    logger.info("P2P protocol activated");
    msgQueue.activate(ctx);
    //tronListener.trace("P2P protocol activated");

    startTimers();
  }


  @Override
  public void channelRead0(final ChannelHandlerContext ctx, P2pMessage msg)
      throws InterruptedException {


    logger.info("rcv p2p msg *************************************************");

    if (P2pMessageCodes.inRange(msg.getCommand().asByte())) {
      logger.trace("P2PHandler invoke: [{}]", msg.getCommand());
    }

    //tronListener.trace(String.format("P2PHandler invoke: [%s]", msg.getCommand()));

    switch (msg.getCommand()) {
      case HELLO:
        msgQueue.receivedMessage(msg);
        logger.info("p2p hello");
        setHandshake((HelloMessage) msg, ctx);
        break;
      case DISCONNECT:
        msgQueue.receivedMessage(msg);
        channel.getNodeStatistics()
            .nodeDisconnectedRemote(ReasonCode.fromInt(((DisconnectMessage) msg).getReason()));
        processDisconnect(ctx, (DisconnectMessage) msg);
        break;
      case PING:
        msgQueue.receivedMessage(msg);
        ctx.writeAndFlush(PONG_MESSAGE);
        break;
      case PONG:
        msgQueue.receivedMessage(msg);
        channel.getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
        break;
      default:
        ctx.fireChannelRead(msg);
        break;
    }
  }

  private void disconnect(ReasonCode reasonCode) {
    msgQueue.sendMessage(new DisconnectMessage(reasonCode));
    channel.getNodeStatistics().nodeDisconnectedLocal(reasonCode);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("channel inactive: ", ctx.toString());
    this.killTimers();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("P2p handling failed", cause);
    ctx.close();
    killTimers();
  }

  private void processDisconnect(ChannelHandlerContext ctx, DisconnectMessage msg) {

    if (logger.isInfoEnabled() && ReasonCode.fromInt(msg.getReason()) == ReasonCode.USELESS_PEER) {

      if (channel.getNodeStatistics().ethInbound.get() - ethInbound > 1 ||
          channel.getNodeStatistics().ethOutbound.get() - ethOutbound > 1) {

        // it means that we've been disconnected
        // after some incorrect action from our peer
        // need to log this moment
        logger.debug("From: \t{}\t [DISCONNECT reason=BAD_PEER_ACTION]", channel);
      }
    }
    ctx.close();
    killTimers();
  }


  public void setHandshake(HelloMessage msg, ChannelHandlerContext ctx) {

    channel.getNodeStatistics().setClientId(msg.getClientId());
//        channel.getNodeStatistics().capabilities.clear();
//        channel.getNodeStatistics().capabilities.addAll(msg.getCapabilities());

    this.ethInbound = (int) channel.getNodeStatistics().ethInbound.get();
    this.ethOutbound = (int) channel.getNodeStatistics().ethOutbound.get();

//        this.handshakeHelloMessage = msg;

//        List<Capability> capInCommon = getSupportedCapabilities(msg);
//        channel.initMessageCodes(capInCommon);

    channel.activateTron(ctx);

    //todo: init peer's block status and sync
    //tronListener.onHandShakePeer(channel, msg);
  }

  /**
   * submit transaction to the network
   */

  public void sendDisconnect() {
        msgQueue.disconnect();
  }

  private void startTimers() {

    logger.info(args.getNodeP2pPingInterval() + "");
    logger.info(args.getNodeP2pPingInterval() + "");
    // sample for pinging in background
    pingTask = pingTimer.scheduleAtFixedRate(() -> {
      try {
         msgQueue.sendMessage(PING_MESSAGE);
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
      }
    }, 2, 10, TimeUnit.SECONDS);
  }

  public void killTimers() {
    pingTask.cancel(false);
      msgQueue.close();
  }


  public void setMsgQueue(MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

}