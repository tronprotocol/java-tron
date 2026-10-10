package org.tron.common.logsfilter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.tron.common.logsfilter.nativequeue.NativeMessageQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class NativeMessageQueueTest {

  private NativeMessageQueue queue;
  private ZMQ.Socket publisher;
  private MockedConstruction<ZContext> contexts;

  @Before
  public void setUp() {
    publisher = mock(ZMQ.Socket.class);
    when(publisher.bind(anyString())).thenReturn(true);
    contexts = mockConstruction(ZContext.class, (context, construction) ->
        when(context.createSocket(SocketType.PUB)).thenReturn(publisher));
    queue = new NativeMessageQueue();
  }

  @After
  public void tearDown() {
    try {
      if (queue != null) {
        queue.stop();
      }
    } finally {
      if (contexts != null) {
        contexts.close();
      }
    }
  }

  @Test
  public void configuredSendQueueLengthIsAppliedBeforeSocketCreation() {
    assertStartup(6000, 2000, 6000, 2000);
  }

  @Test
  public void invalidBindPortUsesDefaultPort() {
    assertStartup(-1111, 1000, 5555, 1000);
  }

  @Test
  public void negativeSendQueueLengthUsesDefaultSndHWM() {
    assertStartup(6000, -1, 6000, 1000);
  }

  @Test
  public void zeroSendQueueLengthUsesDefaultSndHWM() {
    assertStartup(6000, 0, 6000, 1000);
  }

  @Test
  public void publishTriggerSendsTopicBeforePayload() {
    Assert.assertTrue(queue.start(6000, 1000));

    queue.publishTrigger("payload", "topic");

    InOrder delivery = inOrder(publisher);
    delivery.verify(publisher).bind("tcp://*:6000");
    delivery.verify(publisher).sendMore("topic");
    delivery.verify(publisher).send("payload");
    delivery.verifyNoMoreInteractions();
  }

  private void assertStartup(int port, int queueLength, int expectedPort, int expectedQueueLength) {
    Assert.assertTrue(queue.start(port, queueLength));
    Assert.assertEquals(1, contexts.constructed().size());
    ZContext context = contexts.constructed().get(0);

    // ZContext applies its defaults when creating the socket, so ordering matters.
    InOrder startup = inOrder(context, publisher);
    startup.verify(context).setSndHWM(expectedQueueLength);
    startup.verify(context).createSocket(SocketType.PUB);
    startup.verify(publisher).bind("tcp://*:" + expectedPort);
    startup.verifyNoMoreInteractions();
  }
}
