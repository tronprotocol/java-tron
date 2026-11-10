package org.tron.core.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.vm.PrecompiledContracts.PrecompiledContract;
import org.tron.core.vm.PrecompiledContracts.PrecompiledContract.ParallelTimeout;
import org.tron.core.vm.config.VMConfig;

public class PrecompiledParallelAwaitTest {

  @Test
  public void testCollectionCapturesDeadlineOnce() throws Exception {
    AtomicInteger reads = new AtomicInteger();
    long deadlineUs = System.nanoTime() / 1_000 + 5_000_000;
    PrecompiledContract contract = new PrecompiledContracts.Identity() {
      @Override
      public long getVmShouldEndInUs() {
        reads.incrementAndGet();
        return deadlineUs;
      }
    };
    FutureTask<Boolean> completed = new FutureTask<>(() -> Boolean.TRUE);
    completed.run();
    List<Future<Boolean>> futures = new ArrayList<>();
    futures.add(completed);

    withNode(true, false, false, () -> {
      assertEquals(Boolean.TRUE,
          contract.collectParallel(new CountDownLatch(0), futures, ParallelTimeout.MATCH_CPU_CHECK)
              .get(0));
      assertEquals(1, reads.get());
    });
  }

  @Test
  public void testCompletedWaitCollectsResults() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(3);
    try {
      CountDownLatch latch = new CountDownLatch(3);
      AtomicInteger started = new AtomicInteger();
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        futures.add(pool.submit(() -> {
          started.incrementAndGet();
          latch.countDown();
          return Boolean.TRUE;
        }));
      }
      boolean collect = await(latch, 5_000, ParallelTimeout.DROP);
      assertTrue(collect);
      assertEquals(3, started.get());
      for (Future<?> future : futures) {
        assertFalse(future.isCancelled());
        assertEquals(Boolean.TRUE, future.get());
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void testExpiredDeadlineCancelsWithoutReading() throws Exception {
    withNode(true, true, true, () -> {
      try (HeldWorkers workers = new HeldWorkers()) {
        workers.startOne();
        workers.queueTwo();
        List<Boolean> results = collect(workers.idleLatch(), workers.futures, -5,
            ParallelTimeout.MATCH_CPU_CHECK);
        assertNull(results);
        assertEquals(1, workers.started.get());
        assertTrue(workers.futures.get(1).isCancelled());
        assertTrue(workers.futures.get(2).isCancelled());
      }
    });
  }

  @Test
  public void testDropPolicyCancelsTimedOutWaitOnSolidity() throws Exception {
    withNode(true, false, true, () -> {
      try (HeldWorkers workers = new HeldWorkers()) {
        workers.startOne();
        workers.queueTwo();
        List<Boolean> results = collect(workers.idleLatch(), workers.futures, 40,
            ParallelTimeout.DROP);
        assertNull(results);
        assertEquals(1, workers.started.get());
        assertTrue(workers.futures.get(1).isCancelled());
        assertTrue(workers.futures.get(2).isCancelled());
      }
    });
  }

  @Test
  public void testSolidityKeepsResultAfterTimedOutWait() throws Exception {
    assertTimedOutWaitIsKept(false, true);
  }

  @Test
  public void testDebugKeepsResultAfterTimedOutWait() throws Exception {
    assertTimedOutWaitIsKept(true, false);
  }

  @Test
  public void testEnergyAdjustmentOffKeepsResultAfterTimedOutWait() throws Exception {
    withNode(false, false, false, () -> {
      try (HeldWorkers workers = new HeldWorkers()) {
        workers.startOne();
        boolean collect = await(workers.idleLatch(), 40, ParallelTimeout.MATCH_CPU_CHECK);
        assertTrue(collect);
        assertFalse(workers.futures.get(0).isCancelled());
        workers.release();
        assertEquals(Boolean.TRUE, workers.futures.get(0).get(5, TimeUnit.SECONDS));
      }
    });
  }

  @Test
  public void testEnergyAdjustmentOnDropsTimedOutWait() throws Exception {
    withNode(true, false, false, () -> {
      try (HeldWorkers workers = new HeldWorkers()) {
        workers.startOne();
        workers.queueTwo();
        List<Boolean> results = collect(workers.idleLatch(), workers.futures, 40,
            ParallelTimeout.MATCH_CPU_CHECK);
        assertNull(results);
        assertEquals(1, workers.started.get());
        assertTrue(workers.futures.get(1).isCancelled());
        assertTrue(workers.futures.get(2).isCancelled());
      }
    });
  }

  @Test
  public void testInterruptCancelsAndRestoresStatus() throws Exception {
    try (HeldWorkers workers = new HeldWorkers()) {
      workers.startOne();
      Thread worker = Thread.currentThread();
      Thread interrupter = new Thread(() -> {
        try {
          Thread.sleep(30);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
          return;
        }
        worker.interrupt();
      });
      interrupter.start();
      try {
        collect(workers.idleLatch(), workers.futures, 5_000, ParallelTimeout.MATCH_CPU_CHECK);
        fail("wait should have been interrupted");
      } catch (InterruptedException expected) {
        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(workers.futures.get(0).isCancelled());
      } finally {
        Thread.interrupted();
        interrupter.join(1_000);
      }
    }
  }

  @Test(timeout = 3_000)
  public void testCollectStopsWhenPublicationMissesOuterCheck() throws Exception {
    withNode(true, false, false, () -> {
      AtomicInteger started = new AtomicInteger();
      try (LatePublication publication = new LatePublication()) {
        publication.queueAnother(started);
        List<Boolean> results = collect(publication.latch, publication.futures, 40,
            ParallelTimeout.MATCH_CPU_CHECK);
        assertNull(results);
        assertEquals(0, started.get());
        assertTrue(publication.futures.get(1).isCancelled());
      }
    });
  }

  @Test(timeout = 3_000)
  public void testSolidityReadsResultPublishedAfterDeadline() throws Exception {
    assertLatePublicationIsRead(true, false, true);
  }

  @Test(timeout = 3_000)
  public void testEnergyAdjustmentOffReadsResultPublishedAfterDeadline() throws Exception {
    assertLatePublicationIsRead(false, false, false);
  }

  @Test(timeout = 3_000)
  public void testInterruptDuringGetCancelsAndRestoresStatus() throws Exception {
    withNode(true, false, false, () -> {
      try (LatePublication publication = new LatePublication()) {
        Thread reader = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
          try {
            Thread.sleep(30);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return;
          }
          reader.interrupt();
        });
        interrupter.start();
        try {
          collect(publication.latch, publication.futures, 5_000, ParallelTimeout.MATCH_CPU_CHECK);
          fail("get should have been interrupted");
        } catch (InterruptedException expected) {
          assertTrue(Thread.currentThread().isInterrupted());
          assertTrue(publication.future.isCancelled());
        } finally {
          Thread.interrupted();
          interrupter.join(1_000);
        }
      }
    });
  }

  @Test
  public void testWorkerFailurePropagatesAndCancelsRemainingTasks() throws Exception {
    IllegalStateException failure = new IllegalStateException("worker failed");
    FutureTask<Boolean> failed = new FutureTask<>(() -> {
      throw failure;
    });
    failed.run();
    FutureTask<Boolean> pending = new FutureTask<>(() -> Boolean.TRUE);
    List<Future<Boolean>> futures = new ArrayList<>();
    futures.add(failed);
    futures.add(pending);

    try {
      collect(new CountDownLatch(0), futures, 5_000, ParallelTimeout.MATCH_CPU_CHECK);
      fail("worker failure should propagate");
    } catch (ExecutionException expected) {
      assertSame(failure, expected.getCause());
      assertTrue(pending.isCancelled());
      assertFalse(failed.isCancelled());
    }
  }

  @Test
  public void testCancelledResultDropsAndCancelsRemainingTasks() throws Exception {
    FutureTask<Boolean> cancelled = new FutureTask<>(() -> Boolean.TRUE);
    cancelled.cancel(true);
    FutureTask<Boolean> pending = new FutureTask<>(() -> Boolean.TRUE);
    List<Future<Boolean>> futures = new ArrayList<>();
    futures.add(cancelled);
    futures.add(pending);

    assertNull(collect(new CountDownLatch(0), futures, 5_000, ParallelTimeout.MATCH_CPU_CHECK));
    assertTrue(pending.isCancelled());
  }

  private static void assertLatePublicationIsRead(boolean energy, boolean debug, boolean solidity)
      throws Exception {
    withNode(energy, debug, solidity, () -> {
      ExecutorService pool = Executors.newSingleThreadExecutor();
      try {
        CountDownLatch entered = new CountDownLatch(1);
        Future<Boolean> future = pool.submit(() -> {
          entered.countDown();
          Thread.sleep(200);
          return Boolean.TRUE;
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        List<Future<Boolean>> futures = new ArrayList<>();
        futures.add(future);
        List<Boolean> results = collect(new CountDownLatch(0), futures, 40,
            ParallelTimeout.MATCH_CPU_CHECK);
        assertEquals(Boolean.TRUE, results.get(0));
        assertFalse(future.isCancelled());
      } finally {
        pool.shutdownNow();
      }
    });
  }

  private static void assertTimedOutWaitIsKept(boolean debug, boolean solidity) throws Exception {
    withNode(true, debug, solidity, () -> {
      try (HeldWorkers workers = new HeldWorkers()) {
        workers.startOne();
        boolean collect = await(workers.idleLatch(), 40, ParallelTimeout.MATCH_CPU_CHECK);
        assertTrue(collect);
        assertFalse(workers.futures.get(0).isCancelled());
        workers.release();
        assertEquals(Boolean.TRUE, workers.futures.get(0).get(5, TimeUnit.SECONDS));
      }
    });
  }

  private static boolean await(CountDownLatch latch, long delayMs, ParallelTimeout timeout)
      throws InterruptedException {
    return contractAt(delayMs).awaitParallel(latch, timeout);
  }

  private static List<Boolean> collect(CountDownLatch latch, List<Future<Boolean>> futures,
      long delayMs, ParallelTimeout timeout) throws Exception {
    return contractAt(delayMs).collectParallel(latch, futures, timeout);
  }

  private static PrecompiledContract contractAt(long delayMs) {
    PrecompiledContract contract = new PrecompiledContracts.Identity();
    contract.setVmShouldEndInUs(System.nanoTime() / 1_000 + delayMs * 1_000);
    return contract;
  }

  private static void withNode(boolean energyAdjustment, boolean debug, boolean solidity,
      ThrowingRunnable body) throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    boolean previousDebug = parameter.isDebug();
    boolean previousSolidity = parameter.isSolidityNode();
    boolean previousEnergy = VMConfig.allowEnergyAdjustment();
    parameter.setDebug(debug);
    parameter.setSolidityNode(solidity);
    VMConfig.initAllowEnergyAdjustment(energyAdjustment ? 1 : 0);
    try {
      body.run();
    } finally {
      parameter.setDebug(previousDebug);
      parameter.setSolidityNode(previousSolidity);
      VMConfig.initAllowEnergyAdjustment(previousEnergy ? 1 : 0);
    }
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  /**
   * The task counts the latch down, then blocks before {@code call()} returns. That is the
   * window after {@code countDown()} and before {@code FutureTask} publishes the result.
   * A second task stays queued on the same single thread.
   */
  private static final class LatePublication implements AutoCloseable {
    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final CountDownLatch latch = new CountDownLatch(0);
    private final List<Future<Boolean>> futures = new ArrayList<>();
    private final Future<Boolean> future;

    private LatePublication() throws InterruptedException {
      CountDownLatch entered = new CountDownLatch(1);
      future = pool.submit(() -> {
        entered.countDown();
        while (release.getCount() > 0) {
          try {
            release.await(50, TimeUnit.MILLISECONDS);
          } catch (InterruptedException ignored) {
            // Hold this worker so a queued task cannot start during cancel.
          }
        }
        return Boolean.TRUE;
      });
      futures.add(future);
      assertTrue(entered.await(5, TimeUnit.SECONDS));
    }

    private void queueAnother(AtomicInteger started) {
      futures.add(pool.submit(() -> {
        started.incrementAndGet();
        return Boolean.TRUE;
      }));
    }

    @Override
    public void close() {
      release.countDown();
      pool.shutdownNow();
    }
  }

  /**
   * One worker holds the only pool thread until {@link #release()}. Further tasks stay queued,
   * so cancellation of those tasks is observable.
   */
  private static final class HeldWorkers implements AutoCloseable {
    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private final Object lock = new Object();
    private final AtomicInteger started = new AtomicInteger();
    private final List<Future<Boolean>> futures = new ArrayList<>();
    private boolean hold = true;

    private void startOne() throws InterruptedException {
      CountDownLatch entered = new CountDownLatch(1);
      futures.add(pool.submit(() -> holdUntilReleased(entered)));
      assertTrue(entered.await(5, TimeUnit.SECONDS));
    }

    private void queueTwo() {
      futures.add(pool.submit(() -> holdUntilReleased(null)));
      futures.add(pool.submit(() -> holdUntilReleased(null)));
    }

    private Boolean holdUntilReleased(CountDownLatch entered) {
      started.incrementAndGet();
      if (entered != null) {
        entered.countDown();
      }
      synchronized (lock) {
        while (hold) {
          try {
            lock.wait(50);
          } catch (InterruptedException ignored) {
            // Keep this worker so a queued task cannot start while cancel runs.
          }
        }
      }
      return Boolean.TRUE;
    }

    private CountDownLatch idleLatch() {
      return new CountDownLatch(futures.size() + 1);
    }

    private void release() {
      synchronized (lock) {
        hold = false;
        lock.notifyAll();
      }
    }

    @Override
    public void close() {
      release();
      pool.shutdownNow();
    }
  }
}
