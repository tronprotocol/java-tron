package org.tron.core.net;

import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.tron.common.TestConstants;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.PeerManagerStateResetter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;

/** Uses local service instances and resets the shared peer registry between tests. */
public class PeerDisconnectConcurrencyTest {

  @BeforeClass
  public static void initArgs() {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void clearArgs() {
    Args.clearParam();
  }

  @Before
  public void resetPeers() {
    PeerManagerStateResetter.reset();
  }

  @After
  public void clearPeers() {
    PeerManagerStateResetter.reset();
  }

  @Test
  public void disconnectCleanupDoesNotBlockOtherDisconnects() throws Exception {
    assertCleanupDoesNotBlockOtherDisconnects(false);
  }

  @Test
  public void timeoutCleanupDoesNotBlockOtherDisconnects() throws Exception {
    assertCleanupDoesNotBlockOtherDisconnects(true);
  }

  @Test
  public void sameAddressDisconnectDoesNotClearNextEffectiveCandidate() throws Exception {
    InetSocketAddress previous = new InetSocketAddress("192.0.2.10", 18888);
    InetSocketAddress next = new InetSocketAddress("192.0.2.20", 18888);
    AtomicInteger retries = new AtomicInteger();
    EffectiveCheckService effective = new EffectiveCheckService() {
      @Override
      public void triggerNext() {
        if (retries.incrementAndGet() == 1) {
          setCur(next);
        }
      }
    };
    effective.setCur(previous);
    CountDownLatch checked = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    Logger logger = (Logger) LoggerFactory.getLogger("net");
    Level originalLevel = logger.getLevel();
    UnsynchronizedAppenderBase<ILoggingEvent> pause =
        new UnsynchronizedAppenderBase<ILoggingEvent>() {
          @Override
          protected void append(ILoggingEvent event) {
            if (Thread.currentThread().getName().equals("effective-disconnect-first")
                && event.getMessage().startsWith("Close chosen peer:")) {
              // Pause after matching the old candidate, before it can be cleared.
              checked.countDown();
              try {
                resume.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
              }
            }
          }
        };
    pause.setContext(logger.getLoggerContext());
    pause.start();
    logger.setLevel(Level.WARN);
    logger.addAppender(pause);
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      Future<?> first = workers.submit(() -> {
        Thread.currentThread().setName("effective-disconnect-first");
        effective.onDisconnect(previous);
      });
      Assert.assertTrue(checked.await(5, TimeUnit.SECONDS));
      CountDownLatch secondStarted = new CountDownLatch(1);
      Future<?> second = workers.submit(() -> {
        secondStarted.countDown();
        effective.onDisconnect(previous);
      });
      Assert.assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
      try {
        second.get(5, TimeUnit.SECONDS);
      } catch (TimeoutException expected) {
        // A serialized callback waits until the first callback releases the service lock.
      }
      resume.countDown();
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
      Assert.assertEquals(next, effective.getCur());
      Assert.assertEquals(1, retries.get());
    } finally {
      resume.countDown();
      workers.shutdown();
      try {
        Assert.assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
      } finally {
        logger.detachAppender(pause);
        logger.setLevel(originalLevel);
        pause.stop();
      }
    }
  }

  @Test
  public void disconnectDoesNotWaitForAnotherPeersHandshake() throws Exception {
    CountDownLatch handshaking = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    AtomicInteger cleaned = new AtomicInteger();
    PeerConnection connecting = new PeerConnection();
    Channel connectingChannel = channel(10001);
    PeerConnection disconnecting = new PeerConnection() {
      @Override
      public void onDisconnect() {
        cleaned.incrementAndGet();
      }
    };
    disconnecting.setChannel(channel(10002));
    register(disconnecting);
    ApplicationContext context = mock(ApplicationContext.class);
    Mockito.when(context.getBean(PeerConnection.class)).thenReturn(connecting);
    HandshakeService handshake = mock(HandshakeService.class);
    Mockito.doAnswer(invocation -> {
      handshaking.countDown();
      resume.await();
      return null;
    }).when(handshake).startHandshake(connecting);
    P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
    ReflectUtils.setFieldValue(handler, "ctx", context);
    ReflectUtils.setFieldValue(handler, "handshakeService", handshake);
    ReflectUtils.setFieldValue(handler, "effectiveCheckService", new EffectiveCheckService());
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      Future<?> connect = workers.submit(() -> handler.onConnect(connectingChannel));
      Assert.assertTrue(handshaking.await(5, TimeUnit.SECONDS));
      Future<?> disconnect = workers.submit(
          () -> handler.onDisconnect(disconnecting.getChannel()));
      disconnect.get(5, TimeUnit.SECONDS);
      Assert.assertEquals(1, cleaned.get());
      Assert.assertNull(PeerManager.getPeerConnection(disconnecting.getChannel()));
      Assert.assertSame(connecting, PeerManager.getPeerConnection(connectingChannel));
      Assert.assertEquals(1, PeerManager.getPassivePeersCount().get());
      resume.countDown();
      connect.get(5, TimeUnit.SECONDS);
    } finally {
      resume.countDown();
      workers.shutdown();
      Assert.assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
      PeerManager.remove(connectingChannel);
    }
  }

  @Test
  public void disconnectRetryDoesNotWaitForFetchAndRunsOnNextScheduledPass() throws Exception {
    BlockingQueue<Message> sent = new LinkedBlockingQueue<>();
    PeerConnection failed = new PeerConnection();
    failed.setChannel(channel(10001));
    PeerConnection replacement = new PeerConnection() {
      @Override
      public void sendMessage(Message message) {
        sent.add(message);
      }
    };
    replacement.setChannel(channel(10002));
    Item item = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
    failed.getAdvInvRequest().put(item, System.currentTimeMillis());
    replacement.getAdvInvReceive().put(item, System.currentTimeMillis());
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(Collections.singletonList(replacement));
    AdvService adv = new AdvService();
    ReflectUtils.setFieldValue(adv, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(adv, "fetchBlockService", mock(FetchBlockService.class));
    ExecutorService worker = Executors.newSingleThreadExecutor();
    try {
      synchronized (adv) {
        Future<?> disconnect = worker.submit(() -> adv.onDisconnect(failed));
        disconnect.get(5, TimeUnit.SECONDS);
      }
      Map<Item, Long> pending = (Map<Item, Long>) ReflectUtils.getFieldObject(adv, "invToFetch");
      Assert.assertTrue(pending.containsKey(item));
      Assert.assertTrue(replacement.getAdvInvRequest().isEmpty());
      Assert.assertTrue(sent.isEmpty());

      adv.init();
      Message message = sent.poll(5, TimeUnit.SECONDS);
      Assert.assertTrue(message instanceof FetchInvDataMessage);
      Assert.assertEquals(Collections.singletonList(item.getHash()),
          ((FetchInvDataMessage) message).getHashList());
      Assert.assertTrue(replacement.getAdvInvRequest().containsKey(item));
      Assert.assertFalse(pending.containsKey(item));
    } finally {
      worker.shutdownNow();
      Assert.assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
      adv.close();
    }
  }

  private void assertCleanupDoesNotBlockOtherDisconnects(boolean timedOut) throws Exception {
    CountDownLatch cleaning = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    AtomicInteger cleaned = new AtomicInteger();
    Channel firstChannel = channel(10001);
    ReflectUtils.setFieldValue(firstChannel, "disconnectTime",
        System.currentTimeMillis() - 120_000);
    PeerConnection first = new PeerConnection() {
      @Override
      public void onDisconnect() {
        cleaning.countDown();
        try {
          resume.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AssertionError(e);
        }
        cleaned.incrementAndGet();
      }
    };
    first.setChannel(firstChannel);
    PeerConnection second = new PeerConnection() {
      @Override
      public void onDisconnect() {
        cleaned.incrementAndGet();
      }
    };
    second.setChannel(channel(10002));
    register(first);
    register(second);
    P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
    ReflectUtils.setFieldValue(handler, "effectiveCheckService", new EffectiveCheckService());
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      Future<?> cleanup = workers.submit(() -> {
        if (timedOut) {
          Method check = PeerManager.class.getDeclaredMethod("check");
          check.setAccessible(true);
          check.invoke(null);
        } else {
          handler.onDisconnect(firstChannel);
        }
        return null;
      });
      Assert.assertTrue(cleaning.await(5, TimeUnit.SECONDS));
      Future<?> otherDisconnect = workers.submit(() -> {
        handler.onDisconnect(firstChannel);
        handler.onDisconnect(second.getChannel());
      });
      otherDisconnect.get(5, TimeUnit.SECONDS);
      Assert.assertEquals(1, cleaned.get());
      Assert.assertNull(PeerManager.getPeerConnection(firstChannel));
      Assert.assertNull(PeerManager.getPeerConnection(second.getChannel()));
      Assert.assertEquals(0, PeerManager.getActivePeersCount().get());
      Assert.assertEquals(0, PeerManager.getPassivePeersCount().get());
      resume.countDown();
      cleanup.get(5, TimeUnit.SECONDS);
      Assert.assertEquals(2, cleaned.get());
    } finally {
      resume.countDown();
      workers.shutdown();
      Assert.assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static Channel channel(int port) {
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
    Channel channel = new Channel();
    ReflectUtils.setFieldValue(channel, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(channel, "inetAddress", address.getAddress());
    return channel;
  }

  private static void register(PeerConnection peer) {
    ApplicationContext context = mock(ApplicationContext.class);
    Mockito.when(context.getBean(PeerConnection.class)).thenReturn(peer);
    Assert.assertSame(peer, PeerManager.add(context, peer.getChannel()));
  }
}
