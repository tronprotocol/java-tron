package org.tron.p2p.connection.business.keepalive;

import static org.tron.p2p.base.Parameter.PONG_TIMEOUT_MILLIS;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.keepalive.PongMessage;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Connect.DisconnectReason;

public class KeepAliveRaceTest {

  private final KeepAliveService service = new KeepAliveService();
  private final ManualScheduler scheduler = new ManualScheduler();
  private final TestChannel channel = new TestChannel();
  private List<Channel> channels;
  private List<Channel> previousChannels;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    Field executor = KeepAliveService.class.getDeclaredField("executor");
    executor.setAccessible(true);
    ((ScheduledExecutorService) executor.get(service)).shutdown();
    executor.set(service, scheduler);
    Field registry = ChannelManager.class.getDeclaredField("channels");
    registry.setAccessible(true);
    channels = (List<Channel>) registry.get(null);
    previousChannels = new ArrayList<>(channels);
    channels.clear();
    channels.add(channel);
    channel.setFinishHandshake(true);
    channel.setLastSendTime(0);
    channel.pingSent = 1;
    service.init();
  }

  @After
  public void tearDown() throws Exception {
    try {
      service.close();
      scheduler.shutdownNow();
      Assert.assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
    } finally {
      if (previousChannels != null) {
        channels.clear();
        channels.addAll(previousChannels);
      }
    }
  }

  @Test
  public void pingStateIsPublishedBeforeSend() {
    long before = System.currentTimeMillis();

    scheduler.task.run();

    Assert.assertTrue("PING must publish its timestamp before it can be sent",
        channel.timestampAtSend >= before);
    Assert.assertTrue("PING must register its expected response before it can be sent",
        channel.waitingAtSend);
    Assert.assertTrue(channel.waitForPong);
    Assert.assertEquals(1, channel.sent.size());
    Assert.assertEquals(MessageType.KEEP_ALIVE_PING, channel.sent.get(0).getType());
  }

  @Test
  public void pongBeforeSendReturnsDoesNotCauseTimeout() throws Exception {
    channel.blockPing = true;
    ExecutorService sender = Executors.newSingleThreadExecutor();
    try {
      Future<?> sending = sender.submit(scheduler.task);
      Assert.assertTrue(channel.pingStarted.await(5, TimeUnit.SECONDS));

      // Process a valid response on this thread while the keepalive sender is paused.
      receivePong();
      Assert.assertFalse(channel.waitForPong);
      channel.releasePing.countDown();
      sending.get(5, TimeUnit.SECONDS);
      long sentTime = channel.pingSent;

      expirePing();
      scheduler.task.run();

      assertNoTimeout();
      Assert.assertEquals("The PONG handler must see the current PING's timestamp",
          sentTime, channel.timestampAtSend);
    } finally {
      channel.releasePing.countDown();
      sender.shutdownNow();
      Assert.assertTrue(sender.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  public void pongAfterSendReturnsDoesNotCauseTimeout() {
    scheduler.task.run();
    Assert.assertTrue(channel.waitForPong);

    receivePong();
    expirePing();
    scheduler.task.run();

    assertNoTimeout();
  }

  @Test
  public void missingPongStillClosesConnection() throws Exception {
    scheduler.task.run();
    scheduler.task.run();
    Assert.assertTrue(channel.waitForPong);
    Assert.assertEquals("Waiting for PONG must not send another PING", 1, channel.sent.size());
    Assert.assertEquals(0, channel.closeCount);

    expirePing();
    scheduler.task.run();

    Assert.assertTrue(channel.isDisconnect());
    Assert.assertEquals(1, channel.closeCount);
    Assert.assertEquals(2, channel.sent.size());
    Assert.assertEquals(MessageType.DISCONNECT, channel.sent.get(1).getType());
    Assert.assertEquals(DisconnectReason.PING_TIMEOUT,
        Connect.P2pDisconnectMessage.parseFrom(channel.sent.get(1).getData()).getReason());
    scheduler.task.run();
    Assert.assertEquals(1, channel.closeCount);
  }

  private void receivePong() {
    PongMessage pong = new PongMessage();
    Assert.assertTrue(pong.valid());
    service.processMessage(channel, pong);
  }

  private void expirePing() {
    // Advance the timeout's elapsed time without sleeping or making the channel idle again.
    channel.pingSent = System.currentTimeMillis() - PONG_TIMEOUT_MILLIS - 1000;
    channel.setLastSendTime(System.currentTimeMillis());
  }

  private void assertNoTimeout() {
    Assert.assertEquals("A peer that returned PONG must not be closed", 0, channel.closeCount);
    Assert.assertFalse(channel.isDisconnect());
    Assert.assertFalse(channel.waitForPong);
    Assert.assertEquals(1, channel.sent.size());
  }

  private static final class ManualScheduler extends ScheduledThreadPoolExecutor {

    private Runnable task;

    private ManualScheduler() {
      super(1);
      setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
        long delay, TimeUnit unit) {
      task = command;
      // Capture the real scheduled loop and invoke it explicitly in each test.
      return super.schedule(command, 1, TimeUnit.DAYS);
    }
  }

  private static final class TestChannel extends Channel {

    private final List<Message> sent = new ArrayList<>();
    private final CountDownLatch pingStarted = new CountDownLatch(1);
    private final CountDownLatch releasePing = new CountDownLatch(1);
    private boolean blockPing;
    private boolean waitingAtSend;
    private long timestampAtSend;
    private int closeCount;

    @Override
    public void send(Message message) {
      sent.add(message);
      setLastSendTime(System.currentTimeMillis());
      if (message.getType() == MessageType.KEEP_ALIVE_PING) {
        waitingAtSend = waitForPong;
        timestampAtSend = pingSent;
        pingStarted.countDown();
        if (blockPing) {
          try {
            Assert.assertTrue("Timed out waiting for PONG processing",
                releasePing.await(5, TimeUnit.SECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
          }
        }
      }
    }

    @Override
    public void close() {
      closeCount++;
      markDisconnected();
    }
  }
}
