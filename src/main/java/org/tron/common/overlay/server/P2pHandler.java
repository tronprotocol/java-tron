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
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.ReasonCode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.tron.common.overlay.message.StaticMessages.PING_MESSAGE;
import static org.tron.common.overlay.message.StaticMessages.PONG_MESSAGE;

@Component
@Scope("prototype")
public class P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

  private final static Logger logger = LoggerFactory.getLogger("P2pHandler");

  private static ScheduledExecutorService pingTimer =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "P2pPingTimer"));

  private MessageQueue msgQueue;

  private Channel channel;

  private ScheduledFuture<?> pingTask;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    msgQueue.activate(ctx);
    pingTask = pingTimer.scheduleAtFixedRate(() -> {
      try {
        msgQueue.sendMessage(PING_MESSAGE);
      } catch (Throwable t) {
        logger.error("startTimers exception", t);
      }
    }, 2, 10, TimeUnit.SECONDS);
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, P2pMessage msg) throws InterruptedException {
    switch (msg.getType()) {
      case P2P_DISCONNECT:
        msgQueue.receivedMessage(msg);
        channel.getNodeStatistics()
            .nodeDisconnectedRemote(ReasonCode.fromInt(((DisconnectMessage) msg).getReason()));
        closeChannel(ctx);
        break;
      case P2P_PING:
        msgQueue.receivedMessage(msg);
        msgQueue.sendMessage(PONG_MESSAGE);
        break;
      case P2P_PONG:
        msgQueue.receivedMessage(msg);
        channel.getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
        break;
      default:
        logger.info("Receive error msg, {}", ctx.channel().remoteAddress());
        closeChannel(ctx);
        break;
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info("channel inactive {}", ctx.channel().remoteAddress());
    closeChannel(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.info("exception caught, {} {}", ctx.channel().remoteAddress(), cause);
    closeChannel(ctx);
  }

  public void closeChannel(ChannelHandlerContext ctx) {
    ctx.close();
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