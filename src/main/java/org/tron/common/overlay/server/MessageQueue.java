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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.*;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains the logic for sending messages in a queue
 *
 * Messages open by send and answered by receive of appropriate message
 *      PING by PONG
 *      GET_PEERS by PEERS
 *      GET_TRANSACTIONS by TRANSACTIONS
 *      GET_BLOCK_HASHES by BLOCK_HASHES
 *      GET_BLOCKS by BLOCKS
 *
 * The following messages will not be answered:
 *      PONG, PEERS, HELLO, STATUS, TRANSACTIONS, BLOCKS
 *
 * @author Roman Mandeleil
 */
@Component
@Scope("prototype")
public class MessageQueue {

  private static final Logger logger = LoggerFactory.getLogger("MessageQueue");

  private boolean sendMsgFlag = false;

  private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
    private AtomicInteger cnt = new AtomicInteger(0);

    public Thread newThread(Runnable r) {
      return new Thread(r, "MessageQueueTimer-" + cnt.getAndIncrement());
    }
  });

  private static Thread sendMsgThread;

  private Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();
  //private Queue<MessageRoundtrip> respondQueue = new ConcurrentLinkedQueue<>();

  private BlockingQueue<Message> msgQueue = new LinkedBlockingQueue<>();

  private ChannelHandlerContext ctx = null;

  boolean hasPing = false;
  private ScheduledFuture<?> timerTask;
  private Channel channel;

  public MessageQueue() {
  }

  public void activate(ChannelHandlerContext ctx) {

    this.ctx = ctx;

    sendMsgFlag = true;

    timerTask = timer.scheduleAtFixedRate(() -> {
      try {
        nudgeQueue();
      } catch (Throwable t) {
        logger.error("Unhandled exception", t);
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
         ctx.writeAndFlush(msg.getSendData())
                 .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
         logger.debug("send {} to {}", msg.getType(), ctx.channel().remoteAddress());
       }catch (Exception e) {
         logger.error("send message failed, {}, error info: {}", ctx.channel().remoteAddress(), e.getMessage());
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
    if (msg instanceof PingMessage) {
      if (hasPing) return;
      hasPing = true;
    }

    if (msg.getAnswerMessage() != null)
      requestQueue.add(new MessageRoundtrip(msg));
    else
      msgQueue.offer(msg);
  }

  public void disconnect(ReasonCode reason) {
    if (sendMsgFlag){
      ctx.writeAndFlush(new DisconnectMessage(reason).getSendData());
      ctx.close();
    }
  }

  public void receivedMessage(Message msg) throws InterruptedException {

    logger.debug("rcv {} from {}",msg.getType(), ctx.channel().remoteAddress());

    if (requestQueue.peek() != null) {
      MessageRoundtrip messageRoundtrip = requestQueue.peek();
      Message waitingMessage = messageRoundtrip.getMsg();

      if (waitingMessage instanceof PingMessage) hasPing = false;

      if (waitingMessage.getAnswerMessage() != null
          && msg.getClass() == waitingMessage.getAnswerMessage()) {
        logger.info("rcv {} from {}",msg.getType(), ctx.channel().remoteAddress());
        messageRoundtrip.answer();
        channel.getPeerStats().pong(messageRoundtrip.lastTimestamp);
      }
    }
  }

  private void removeAnsweredMessage(MessageRoundtrip messageRoundtrip) {
    if (messageRoundtrip != null && messageRoundtrip.isAnswered()) 
      requestQueue.remove();
  }

  private void nudgeQueue() {
    removeAnsweredMessage(requestQueue.peek());
    sendToWire(requestQueue.peek());
  }

  private void sendToWire(MessageRoundtrip messageRoundtrip) {

    if (!sendMsgFlag || messageRoundtrip == null){
      return;
    }

    if (messageRoundtrip.getRetryTimes() > 0 && !messageRoundtrip.hasToRetry()){
      return;
    }

    if (messageRoundtrip.getRetryTimes() > 0){
      logger.warn("wait {} timeout. close channel {}.", messageRoundtrip.getMsg().getAnswerMessage(), ctx.channel().remoteAddress());
      ctx.close();
      return;
    }

    Message msg = messageRoundtrip.getMsg();

    ctx.writeAndFlush(msg.getSendData())
            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

    logger.info("send {} to {}", msg.getType(), ctx.channel().remoteAddress());

    if (msg.getAnswerMessage() != null) {
      messageRoundtrip.incRetryTimes();
      messageRoundtrip.saveTime();
    }
  }

  public void close() {

    sendMsgFlag = false;

    timerTask.cancel(false);
  }

}
