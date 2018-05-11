package org.tron.common.overlay.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.ReasonCode;

@Component
@Scope("prototype")
public class MessageQueue {

  private static final Logger logger = LoggerFactory.getLogger("MessageQueue");

  private volatile boolean sendMsgFlag = false;

  private Thread sendMsgThread;

  private Channel channel;

  private ChannelHandlerContext ctx = null;

  private Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();

  private BlockingQueue<Message> msgQueue = new LinkedBlockingQueue<>();

  private static ScheduledExecutorService sendTimer =
          Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "sendTimer"));

  private ScheduledFuture<?> sendTask;


  public void activate(ChannelHandlerContext ctx) {

    this.ctx = ctx;

    sendMsgFlag = true;

    sendTask = sendTimer.scheduleAtFixedRate(() -> {
      try {
        if (sendMsgFlag){
          send();
        }
      } catch (Exception e) {
        logger.error("Unhandled exception", e);
      }
    }, 10, 10, TimeUnit.MILLISECONDS);

    sendMsgThread = new Thread(()->{
     while (sendMsgFlag) {
       try {
         if (msgQueue.isEmpty()){
           Thread.sleep(10);
           continue;
         }
         Message msg = msgQueue.take();
         ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
           if (!future.isSuccess()) {
             logger.error("send {} to {} fail", msg.getType(), ctx.channel().remoteAddress());
           }
         });
       }catch (Exception e) {
         logger.error("Send message failed, {}, error info: {}", ctx.channel().remoteAddress(), e.getMessage());
       }
     }
    });
    sendMsgThread.setName("sendMsgThread-" + ctx.channel().remoteAddress());
    sendMsgThread.start();
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void sendMessage(Message msg) {
    logger.info("send {} to {}", msg.getType(), ctx.channel().remoteAddress());
    if (msg.getAnswerMessage() != null)
      requestQueue.add(new MessageRoundtrip(msg));
    else
      msgQueue.offer(msg);
  }

  public void receivedMessage(Message msg){
    logger.info("rcv {} from {}", msg.getType(), ctx.channel().remoteAddress());
    MessageRoundtrip messageRoundtrip = requestQueue.peek();
    if (messageRoundtrip != null && messageRoundtrip.getMsg().getAnswerMessage() == msg.getClass()){
      requestQueue.remove();
    }
  }

  public void close() {
    sendMsgFlag = false;
    if(sendTask != null && !sendTask.isCancelled()){
      sendTask.cancel(false);
      sendTask = null;
    }
    if (sendMsgThread != null){
      try{
        sendMsgThread.join(20);
        sendMsgThread = null;
      }catch (Exception e){
        logger.warn("Join send thread failed, peer {}", ctx.channel().remoteAddress());
      }
    }
  }

  private void send() {
    MessageRoundtrip messageRoundtrip = requestQueue.peek();
    if (!sendMsgFlag || messageRoundtrip == null){
      return;
    }
    if (messageRoundtrip.getRetryTimes() > 0 && !messageRoundtrip.hasToRetry()){
      return;
    }
    if (messageRoundtrip.getRetryTimes() > 0){
      channel.getNodeStatistics().nodeDisconnectedLocal(ReasonCode.PING_TIMEOUT);
      logger.warn("Wait {} timeout. close channel {}.", messageRoundtrip.getMsg().getAnswerMessage(), ctx.channel().remoteAddress());
      channel.close();
      return;
    }

    Message msg = messageRoundtrip.getMsg();

    ctx.writeAndFlush(msg.getSendData()).addListener((ChannelFutureListener) future -> {
      if (!future.isSuccess()) {
        logger.error("send {} to {} fail", msg.getType(), ctx.channel().remoteAddress());
      }
    });

    messageRoundtrip.incRetryTimes();
    messageRoundtrip.saveTime();
  }

}
