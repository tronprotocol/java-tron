package org.tron.common.es;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.exit.ExitManager;

@Slf4j(topic = "common-executor")
public class ExecutorServiceManager {

  public static ExecutorService newSingleThreadExecutor(String name) {
    return newSingleThreadExecutor(name, false);
  }

  public static ExecutorService newSingleThreadExecutor(String name, boolean isDaemon) {
    return Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat(name).setDaemon(isDaemon).build());
  }


  public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name) {
    return newSingleThreadScheduledExecutor(name, false);
  }

  public static ScheduledExecutorService newSingleThreadScheduledExecutor(String name,
                                                                          boolean isDaemon) {
    return Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat(name).setDaemon(isDaemon).build());
  }

  public static ExecutorService newFixedThreadPool(String name, int fixThreads) {
    return newFixedThreadPool(name, fixThreads, false);
  }

  public static ExecutorService newFixedThreadPool(String name, int fixThreads, boolean isDaemon) {
    return Executors.newFixedThreadPool(fixThreads,
        new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(isDaemon).build());
  }

  public static ExecutorService newThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                                   long keepAliveTime, TimeUnit unit,
                                                   BlockingQueue<Runnable> workQueue,
                                                   String name) {
    return newThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
        name, false);
  }

  public static ExecutorService newThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                                   long keepAliveTime, TimeUnit unit,
                                                   BlockingQueue<Runnable> workQueue,
                                                   String name, boolean isDaemon) {
    return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
        new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(isDaemon).build());
  }

  public static void shutdownAndAwaitTermination(ExecutorService pool, String name) {
    if (pool == null) {
      return;
    }
    logger.info("Pool {} shutdown...", name);
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
    logger.info("Pool {} shutdown done", name);
  }

  public static Future<?> submit(ExecutorService es, Runnable task) {
    return es.submit(() -> {
      try {
        task.run();
      } catch (Throwable e) {
        ExitManager.findTronError(e).ifPresent(ExitManager::logAndExit);
        throw e;
      }
    });
  }

  public static ScheduledFuture<?> scheduleWithFixedDelay(ScheduledExecutorService es,
                                                   Runnable command,
                                                   long initialDelay,
                                                   long delay,
                                                   TimeUnit unit) {
    return es.scheduleWithFixedDelay(() -> {
      try {
        command.run();
      } catch (Throwable e) {
        ExitManager.findTronError(e).ifPresent(ExitManager::logAndExit);
        throw e;
      }
    }, initialDelay, delay, unit);
  }
}
