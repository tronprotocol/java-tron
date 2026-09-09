package org.tron.common.logsfilter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.tron.common.logsfilter.nativequeue.NativeMessageQueue;
import org.tron.common.utils.PublicMethod;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class NativeMessageQueueTest {

  @After
  public void tearDown() {
    NativeMessageQueue.getInstance().stop();
  }

  @Test
  public void invalidBindPort() {
    assertDefaultConfiguration(-1111, 0, 0);
  }

  @Test
  public void invalidSendLength() {
    assertDefaultConfiguration(0, -2222, 1000);
  }

  private void assertDefaultConfiguration(int port, int queueLength, int expectedQueueLength) {
    ZMQ.Socket publisher = mock(ZMQ.Socket.class);
    when(publisher.bind("tcp://*:5555")).thenReturn(true);
    // Check fallback values without reserving the shared default port in a test JVM.
    try (MockedConstruction<ZContext> contexts = mockConstruction(ZContext.class,
        (context, construction) -> when(context.createSocket(SocketType.PUB))
            .thenReturn(publisher))) {
      try {
        Assert.assertTrue(NativeMessageQueue.getInstance().start(port, queueLength));
        verify(publisher).bind("tcp://*:5555");
        verify(contexts.constructed().get(0)).setSndHWM(expectedQueueLength);
      } finally {
        NativeMessageQueue.getInstance().stop();
      }
      verify(publisher).close();
      verify(contexts.constructed().get(0)).close();
    }
  }

  @Test(timeout = 15_000)
  public void publishTrigger() {
    int bindPort = PublicMethod.chooseRandomPort();
    String dataToSend = "################";
    String topic = "testTopic";
    Assert.assertTrue(NativeMessageQueue.getInstance().start(bindPort, 0));

    try (ZContext context = new ZContext()) {
      ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
      subscriber.setReceiveTimeOut(100);
      Assert.assertTrue(subscriber.subscribe(topic));
      Assert.assertTrue(subscriber.connect(String.format("tcp://127.0.0.1:%d", bindPort)));

      // PUB/SUB subscription setup is asynchronous. Bound the wait and assert on this thread.
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (System.nanoTime() < deadline) {
        NativeMessageQueue.getInstance().publishTrigger(dataToSend, topic);
        String receivedTopic = subscriber.recvStr();
        if (receivedTopic != null) {
          Assert.assertEquals(topic, receivedTopic);
          Assert.assertTrue(subscriber.hasReceiveMore());
          Assert.assertEquals(dataToSend, subscriber.recvStr());
          Assert.assertFalse(subscriber.hasReceiveMore());
          return;
        }
      }
      Assert.fail("Timed out waiting for the published trigger");
    }
  }
}
