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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.statistics.MessageStatistics;
import org.tron.common.overlay.discover.node.statistics.NodeStatistics;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
@Scope("prototype")
public class P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

  private static ScheduledExecutorService pingTimer =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "P2pPingTimer"));

  private MessageQueue msgQueue;

  private Channel channel;

  private ScheduledFuture<?> pingTask;

  private volatile boolean hasPing = false;

  private volatile long sendPingTime;

  private ChannelHandlerContext ctx;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;
    pingTask = pingTimer.scheduleAtFixedRate(() -> {
      if (!hasPing){
        sendPingTime = System.currentTimeMillis();
        hasPing = msgQueue.sendMessage(PING_MESSAGE);
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, P2pMessage msg) throws InterruptedException {

    msgQueue.receivedMessage(msg);
    MessageStatistics messageStatistics = channel.getNodeStatistics().messageStatistics;
    switch (msg.getType()) {
      case P2P_PING:
        int count = messageStatistics.p2pInPing.getCount(10);
        if (count > 3){
          logger.warn("TCP attack found: {} with ping count({})", ctx.channel().remoteAddress(), count);
          channel.disconnect(ReasonCode.BAD_PROTOCOL);
          return;
        }
        msgQueue.sendMessage(PONG_MESSAGE);
        break;
      case P2P_PONG:
        if (messageStatistics.p2pInPong.getTotalCount() > messageStatistics.p2pOutPing.getTotalCount()){
          logger.warn("TCP attack found: {} with ping count({}), pong count({})",
              ctx.channel().remoteAddress(),
              messageStatistics.p2pOutPing.getTotalCount(),
              messageStatistics.p2pInPong.getTotalCount());
          channel.disconnect(ReasonCode.BAD_PROTOCOL);
          return;
        }
        hasPing = false;
        channel.getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
        channel.getPeerStats().pong(sendPingTime);
        break;
      case P2P_DISCONNECT:
        channel.getNodeStatistics()
            .nodeDisconnectedRemote(((DisconnectMessage) msg).getReasonCode());
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

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void close() {
    if (pingTask != null && !pingTask.isCancelled()){
      pingTask.cancel(false);
    }
  }
}