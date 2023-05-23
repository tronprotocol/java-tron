package org.tron.common.es;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "common")
public class ExecutorServiceManager {

  public static ExecutorService newSingleThreadExecutor(String name) {
    return newSingleThreadExecutor(name, false);
  }

  public static ExecutorService newSingleThreadExecutor(String name, boolean isDaemon) {
    return Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(isDaemon).build());
  }


  public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
    return newSingleThreadScheduledExecutor(name, false);
  }

  public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name,
                                                                          boolean isDaemon) {
    return Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(isDaemon).build());
  }

  public static void shutdownAndAwaitTermination(ExecutorService pool, String name) {
    if (pool == null) {
      return;
    }
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
          logger.warn("Pool {} did not terminate", name);
        }
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}
