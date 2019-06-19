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
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

  private static ScheduledExecutorService pingTimer =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "P2pPingTimer"));

  private MessageQueue msgQueue;

  private Channel channel;

  private ScheduledFuture<?> pingTask;

  private volatile boolean hasPing = false;

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    pingTask = pingTimer.scheduleAtFixedRate(() -> {
      if (!hasPing) {
        hasPing = msgQueue.sendMessage(PING_MESSAGE);
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, P2pMessage msg) {

    msgQueue.receivedMessage(msg);
    MessageStatistics messageStatistics = channel.getNodeStatistics().messageStatistics;
    switch (msg.getType()) {
      case P2P_PING:
        int count = messageStatistics.p2pInPing.getCount(10);
        if (count > 3) {
          logger.warn("TCP attack found: {} with ping count({})", ctx.channel().remoteAddress(),
              count);
          channel.disconnect(ReasonCode.BAD_PROTOCOL);
          return;
        }
        msgQueue.sendMessage(PONG_MESSAGE);
        break;
      case P2P_PONG:
        if (messageStatistics.p2pInPong.getTotalCount() > messageStatistics.p2pOutPing
            .getTotalCount()) {
          logger.warn("TCP attack found: {} with ping count({}), pong count({})",
              ctx.channel().remoteAddress(),
              messageStatistics.p2pOutPing.getTotalCount(),
              messageStatistics.p2pInPong.getTotalCount());
          channel.disconnect(ReasonCode.BAD_PROTOCOL);
          return;
        }
        hasPing = false;
        channel.getNodeStatistics().lastPongReplyTime.set(System.currentTimeMillis());
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
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    channel.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void close() {
    if (pingTask != null && !pingTask.isCancelled()) {
      pingTask.cancel(false);
    }
  }
}