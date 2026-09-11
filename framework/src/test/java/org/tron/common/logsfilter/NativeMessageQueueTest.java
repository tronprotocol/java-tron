package org.tron.common.logsfilter;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.logsfilter.nativequeue.NativeMessageQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class NativeMessageQueueTest {

  public int bindPort = 5555;
  public String dataToSend = "################";
  public String topic = "testTopic";

  private ExecutorService subscriberExecutor;
  private final String zmqSubscriber = "zmq-subscriber";

  @After
  public void tearDown() {
    NativeMessageQueue.getInstance().stop();
    ExecutorServiceManager.shutdownAndAwaitTermination(subscriberExecutor, zmqSubscriber);
    subscriberExecutor = null;
  }

  @Test
  public void configuredSendQueueLengthIsAppliedToPublisherSocket() throws Exception {
    int sendQueueLength = 2000;

    Assert.assertTrue(NativeMessageQueue.getInstance().start(bindPort, sendQueueLength));

    Assert.assertEquals(sendQueueLength, getPublisher().getSndHWM());
  }

  @Test
  public void invalidBindPort() {
    boolean bRet = NativeMessageQueue.getInstance().start(-1111, 0);
    Assert.assertEquals(true, bRet);
    NativeMessageQueue.getInstance().stop();
  }

  @Test
  public void negativeSendQueueLengthUsesDefaultSndHWM() throws Exception {
    Assert.assertTrue(NativeMessageQueue.getInstance().start(bindPort, -1));

    Assert.assertEquals(1000, getPublisher().getSndHWM());
  }

  @Test
  public void zeroSendQueueLengthUsesDefaultSndHWM() throws Exception {
    Assert.assertTrue(NativeMessageQueue.getInstance().start(bindPort, 0));

    Assert.assertEquals(1000, getPublisher().getSndHWM());
  }

  @Test
  public void publishTrigger() {

    int sendLength = 0;
    boolean bRet = NativeMessageQueue.getInstance().start(bindPort, sendLength);
    Assert.assertEquals(true, bRet);

    startSubscribeThread();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    NativeMessageQueue.getInstance().publishTrigger(dataToSend, topic);

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    NativeMessageQueue.getInstance().stop();
  }

  public void startSubscribeThread() {
    subscriberExecutor = ExecutorServiceManager.newSingleThreadExecutor(zmqSubscriber);
    subscriberExecutor.execute(() -> {
      try (ZContext context = new ZContext()) {
        ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);

        Assert.assertTrue(subscriber.connect(String.format("tcp://localhost:%d", bindPort)));
        Assert.assertTrue(subscriber.subscribe(topic));

        while (!Thread.currentThread().isInterrupted()) {
          byte[] message = subscriber.recv();
          String triggerMsg = new String(message);

          Assert.assertTrue(triggerMsg.contains(dataToSend) || triggerMsg.contains(topic));
        }
        // ZMQ.Socket will be automatically closed when ZContext is closed
      }
    });
  }

  private ZMQ.Socket getPublisher() throws ReflectiveOperationException {
    Field publisherField = NativeMessageQueue.class.getDeclaredField("publisher");
    publisherField.setAccessible(true);
    return (ZMQ.Socket) publisherField.get(NativeMessageQueue.getInstance());
  }
}
