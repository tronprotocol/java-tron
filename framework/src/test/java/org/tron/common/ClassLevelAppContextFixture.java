package org.tron.common;

import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.config.DefaultConfig;

/**
 * Shared class-level fixture for tests that manually manage a TronApplicationContext.
 */
public class ClassLevelAppContextFixture {

  private TronApplicationContext context;

  public TronApplicationContext createContext() {
    context = new TronApplicationContext(DefaultConfig.class);
    return context;
  }

  public TronApplicationContext createAndStart() {
    createContext();
    startApp();
    return context;
  }

  public void startApp() {
    ApplicationFactory.create(context).startup();
  }

  public TronApplicationContext getContext() {
    return context;
  }

  public void close() {
    if (context != null) {
      context.close();
      context = null;
    }
  }

  public static void shutdownChannel(ManagedChannel channel) {
    if (channel == null) {
      return;
    }
    try {
      channel.shutdown();
      if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
        channel.shutdownNow();
      }
    } catch (InterruptedException e) {
      channel.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public static void shutdownChannels(ManagedChannel... channels) {
    for (ManagedChannel channel : channels) {
      shutdownChannel(channel);
    }
  }
}
