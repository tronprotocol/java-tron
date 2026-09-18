package org.tron.core.net.service.statistics;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class MessageCountTest {

  private static final int THREAD_COUNT = 8;
  private static final int ITERATIONS = 50_000;

  @Test(timeout = 10_000)
  public void windowOperationsUseOneMonitor() throws Exception {
    MessageCount messageCount = new MessageCount();
    Method update = MessageCount.class.getDeclaredMethod("update");
    update.setAccessible(true);
    List<CheckedRunnable> operations = Arrays.asList(
        messageCount::add,
        () -> messageCount.add(2),
        () -> messageCount.getCount(1),
        messageCount::getTotalCount,
        messageCount::reset,
        messageCount::toString,
        () -> update.invoke(messageCount));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      for (CheckedRunnable operation : operations) {
        assertUsesMonitor(messageCount, executor, operation);
      }
    } finally {
      executor.shutdownNow();
      Assert.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }
  }

  @Test(timeout = 10_000)
  public void concurrentAddsDoNotLoseUpdates() throws Exception {
    MessageCount messageCount = new MessageCount();
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<?>[] futures = new Future<?>[THREAD_COUNT];
      for (int thread = 0; thread < THREAD_COUNT; thread++) {
        futures[thread] = executor.submit(() -> {
          ready.countDown();
          try {
            start.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
          for (int i = 0; i < ITERATIONS; i++) {
            if ((i & 1) == 0) {
              messageCount.add();
            } else {
              messageCount.add(2);
            }
          }
        });
      }

      Assert.assertTrue(ready.await(1, TimeUnit.SECONDS));
      start.countDown();
      for (Future<?> future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }

      int expected = THREAD_COUNT * ITERATIONS / 2 * 3;
      Assert.assertEquals(expected, messageCount.getTotalCount());
      Assert.assertEquals(expected, messageCount.getCount(60));
    } finally {
      start.countDown();
      executor.shutdownNow();
      Assert.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }
  }

  private void assertUsesMonitor(MessageCount messageCount, ExecutorService executor,
      CheckedRunnable operation) throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch finished = new CountDownLatch(1);
    Future<?> future;
    synchronized (messageCount) {
      future = executor.submit(() -> {
        started.countDown();
        try {
          operation.run();
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          finished.countDown();
        }
      });
      Assert.assertTrue(started.await(1, TimeUnit.SECONDS));
      Assert.assertFalse(finished.await(100, TimeUnit.MILLISECONDS));
    }
    future.get(1, TimeUnit.SECONDS);
  }

  @FunctionalInterface
  private interface CheckedRunnable {

    void run() throws Exception;
  }
}
