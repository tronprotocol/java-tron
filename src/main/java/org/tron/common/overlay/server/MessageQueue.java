package org.tron.common.overlay.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.PingMessage;
import org.tron.common.overlay.message.PongMessage;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.TransactionsMessage;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

import java.util.Queue;
import java.util.concurrent.*;

@Slf4j(topic = "net")
@Component
@Scope("prototype")
public class MessageQueue {

  private volatile boolean sendMsgFlag = false;

  private volatile long sendTime;

  private volatile long sendPing;

  private Thread sendMsgThread;

  private Channel channel;

  private ChannelHandlerContext ctx = null;

  private Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();

  private BlockingQueue<Message> msgQueue = new LinkedBlockingQueue<>();

  private static ScheduledExecutorService sendTimer = Executors.
      newSingleThreadScheduledExecutor(r -> new Thread(r, "sendTimer"));

  private ScheduledFuture<?> sendTask;


  public void activate(ChannelHandlerContext ctx) {

    this.ctx = ctx;

    sendMsgFlag = true;

    sendTask = sendTimer.scheduleAtFixedRate(() -> {
      try {
        if (sendMsgFlag) {
          send();
        }
      } catch (Exception e) {
        logger.error("Unhandled exception", e);
      }
    }, 10, 10, TimeUnit.MILLISECONDS);

    sendMsgThread = new Thread(() -> {
      while (sendMsgFlag) {
        try {
          if (msgQueue.isEmpty()) {
            Thread.sleep(10);
            continue;
          }
          Message msg = msgQueue.take();
          ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess() && !channel.isDisconnect()) {
              logger.error("Fail send to {}, {}", ctx.channel().remoteAddress(), msg);
            }
          });
        } catch (Exception e) {
          logger.error("Fail send to {}, error info: {}", ctx.channel().remoteAddress(),
              e.getMessage());
        }
      }
    });
    sendMsgThread.setName("sendMsgThread-" + ctx.channel().remoteAddress());
    sendMsgThread.start();
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public boolean sendMessage(Message msg) {
    long now = System.currentTimeMillis();
    if (msg instanceof PingMessage) {
      if (now - sendTime < 10_000 && now - sendPing < 60_000) {
        return false;
      }
      sendPing = now;
    }
    if (needToLog(msg)) {
      logger.info("Send to {}, {} ", ctx.channel().remoteAddress(), msg);
    }
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(msg);
    sendTime = System.currentTimeMillis();
    if (msg.getAnswerMessage() != null) {
      requestQueue.add(new MessageRoundtrip(msg));
    } else {
      msgQueue.offer(msg);
    }
    return true;
  }

  public void receivedMessage(Message msg) {
    if (needToLog(msg)) {
      logger.info("Receive from {}, {}", ctx.channel().remoteAddress(), msg);
    }
    channel.getNodeStatistics().messageStatistics.addTcpInMessage(msg);
    MessageRoundtrip rt = requestQueue.peek();
    if (rt != null && rt.getMsg().getAnswerMessage() == msg.getClass()) {
      requestQueue.remove();
      if (rt.getMsg() instanceof PingMessage) {
        channel.getNodeStatistics().pingMessageLatency
            .add(System.currentTimeMillis() - rt.getTime());
      }
    }
  }

  public void close() {
    sendMsgFlag = false;
    if (sendTask != null && !sendTask.isCancelled()) {
      sendTask.cancel(false);
      sendTask = null;
    }
    if (sendMsgThread != null) {
      try {
        sendMsgThread.join(20);
        sendMsgThread = null;
      } catch (Exception e) {
        logger.warn("Join send thread failed, peer {}", ctx.channel().remoteAddress());
      }
    }
  }

  private boolean needToLog(Message msg) {
    if (msg instanceof PingMessage ||
        msg instanceof PongMessage ||
        msg instanceof TransactionsMessage) {
      return false;
    }

    if (msg instanceof InventoryMessage &&
        ((InventoryMessage) msg).getInventoryType().equals(InventoryType.TRX)) {
      return false;
    }

    return true;
  }

  private void send() {
    MessageRoundtrip rt = requestQueue.peek();
    if (!sendMsgFlag || rt == null) {
      return;
    }
    if (rt.getRetryTimes() > 0 && !rt.hasToRetry()) {
      return;
    }
    if (rt.getRetryTimes() > 0) {
      channel.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.PING_TIMEOUT);
      logger.warn("Wait {} timeout. close channel {}.",
          rt.getMsg().getAnswerMessage(), ctx.channel().remoteAddress());
      channel.close();
      return;
    }

    Message msg = rt.getMsg();

    ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
      if (!future.isSuccess()) {
        logger.error("Fail send to {}, {}", ctx.channel().remoteAddress(), msg);
      }
    });

    rt.incRetryTimes();
    rt.saveTime();
  }

}
