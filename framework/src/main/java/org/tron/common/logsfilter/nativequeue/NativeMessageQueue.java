package org.tron.common.logsfilter.nativequeue;

import java.util.Objects;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class NativeMessageQueue {

  private static final int DEFAULT_BIND_PORT = 5555;
  private static final int DEFAULT_QUEUE_LENGTH = 1000;
  private static NativeMessageQueue instance;
  private ZContext context = null;
  private ZMQ.Socket publisher = null;

  public static NativeMessageQueue getInstance() {
    if (Objects.isNull(instance)) {
      synchronized (NativeMessageQueue.class) {
        if (Objects.isNull(instance)) {
          instance = new NativeMessageQueue();
        }
      }
    }
    return instance;
  }

  public boolean start(int bindPort, int sendQueueLength) {
    context = new ZContext();
    publisher = context.createSocket(SocketType.PUB);

    if (Objects.isNull(publisher)) {
      return false;
    }

    if (bindPort == 0 || bindPort < 0) {
      bindPort = DEFAULT_BIND_PORT;
    }

    if (sendQueueLength < 0) {
      sendQueueLength = DEFAULT_QUEUE_LENGTH;
    }

    context.setSndHWM(sendQueueLength);

    String bindAddress = String.format("tcp://*:%d", bindPort);
    return publisher.bind(bindAddress);
  }

  public void stop() {
    if (Objects.nonNull(publisher)) {
      publisher.close();
    }

    if (Objects.nonNull(context)) {
      context.close();
    }
  }

  public void publishTrigger(String data, String topic) {
    if (Objects.isNull(publisher) || Objects.isNull(context.isClosed()) || context.isClosed()) {
      return;
    }

    publisher.sendMore(topic);
    publisher.send(data);
  }
}
