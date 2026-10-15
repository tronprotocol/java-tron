package org.tron.p2p.discover.socket;

import io.netty.channel.Channel;
import java.lang.reflect.Field;
import java.net.BindException;
import java.net.DatagramSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;

public class DiscoverServerTest {

  @Test(timeout = 15000)
  public void shutdownBeforeStartupCompletesInitialBindExceptionally() {
    P2pConfig previousConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setPort(0);
    DiscoverServer server = new DiscoverServer();
    try {
      server.close();
      IllegalStateException error = Assert.assertThrows(IllegalStateException.class,
          () -> server.init(new NoopEventHandler()));
      Assert.assertEquals("Discovery server startup was cancelled", error.getMessage());
    } finally {
      server.close();
      Parameter.p2pConfig = previousConfig;
    }
  }

  @Test(timeout = 15000)
  public void initialUdpBindFailureReachesCaller() throws Exception {
    P2pConfig previousConfig = Parameter.p2pConfig;
    try (DatagramSocket occupied = new DatagramSocket(0)) {
      Parameter.p2pConfig = new P2pConfig();
      Parameter.p2pConfig.setPort(occupied.getLocalPort());
      DiscoverServer server = new DiscoverServer();
      try {
        IllegalStateException error = Assert.assertThrows(IllegalStateException.class,
            () -> server.init(new NoopEventHandler()));
        Assert.assertTrue(error.getCause() instanceof BindException);
      } finally {
        server.close();
      }
    } finally {
      Parameter.p2pConfig = previousConfig;
    }
  }

  @Test(timeout = 15000)
  public void closeDuringBindReleasesChannelAndEventLoop() throws Exception {
    P2pConfig previousConfig = Parameter.p2pConfig;
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setPort(0);
    DiscoverServer server = new DiscoverServer();
    CountDownLatch initializing = new CountDownLatch(1);
    CountDownLatch continueBind = new CountDownLatch(1);
    AtomicReference<Channel> udpChannel = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    ExecutorService executor = Executors.newSingleThreadExecutor(
        BasicThreadFactory.builder().namingPattern("discovery-startup-test").build());
    try {
      EventHandler eventHandler = new EventHandler() {
        @Override
        public void channelActivated() {
        }

        @Override
        public void handleEvent(UdpEvent event) {
        }

        @Override
        public void setMessageSender(Consumer<UdpEvent> sender) {
          try {
            Field field = MessageHandler.class.getDeclaredField("channel");
            field.setAccessible(true);
            udpChannel.set((Channel) field.get(sender));
            initializing.countDown();
            Assert.assertTrue(continueBind.await(5, TimeUnit.SECONDS));
          } catch (Throwable error) {
            failure.set(error);
          }
        }
      };
      Future<?> startup = executor.submit(() -> {
        try {
          server.init(eventHandler);
        } catch (Throwable error) {
          failure.set(error);
        }
      });
      Assert.assertTrue(initializing.await(5, TimeUnit.SECONDS));
      // Pause before bind completes, when DiscoverServer.close() cannot yet see its channel.
      server.close();
      continueBind.countDown();
      startup.get(5, TimeUnit.SECONDS);
      Channel channel = udpChannel.get();
      Assert.assertTrue(channel.closeFuture().await(5, TimeUnit.SECONDS));
      Assert.assertTrue(channel.eventLoop().terminationFuture().await(5, TimeUnit.SECONDS));
      Assert.assertFalse(channel.isOpen());
      Assert.assertTrue(failure.get() instanceof IllegalStateException);
      Assert.assertEquals("Discovery server startup was cancelled", failure.get().getMessage());
    } finally {
      continueBind.countDown();
      server.close();
      executor.shutdown();
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
      Parameter.p2pConfig = previousConfig;
    }
  }

  private static class NoopEventHandler implements EventHandler {
    @Override public void channelActivated() { }

    @Override public void handleEvent(UdpEvent event) { }

    @Override public void setMessageSender(Consumer<UdpEvent> sender) { }
  }
}
