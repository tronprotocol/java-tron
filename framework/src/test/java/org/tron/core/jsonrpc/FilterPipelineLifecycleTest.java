package org.tron.core.jsonrpc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.TestConstants;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.logsfilter.capsule.BlockFilterCapsule;
import org.tron.common.logsfilter.capsule.LogsFilterCapsule;
import org.tron.common.logsfilter.queue.FilterCapsuleQueue;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;

/**
 * Lifecycle of the filter consumer owned by TronJsonRpcImpl. Only the context-destruction
 * case needs Spring; the rest drive a directly constructed instance.
 */
public class FilterPipelineLifecycleTest {

  private static final String FILTER_THREAD = "filter";
  private static final String LOOP_METHOD = "filterProcessLoop";
  // shutdownAndAwaitTermination() escalates to shutdownNow() after 60s; a stop well
  // below that bound can only come from the stop flag
  private static final long GRACEFUL_BOUND_MS = 10_000;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final FilterCapsuleQueue queue = new FilterCapsuleQueue();
  private TronJsonRpcImpl tronJsonRpc;
  private TronApplicationContext context;

  private void startStandalone(boolean filterEnabled) {
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(filterEnabled);
    CommonParameter.getInstance().setJsonRpcHttpSolidityNodeEnable(false);
    tronJsonRpc = new TronJsonRpcImpl(null, null, null);
    ReflectUtils.setFieldValue(tronJsonRpc, "filterCapsuleQueue", queue);
    ReflectUtils.invokeMethod(tronJsonRpc, "start");
  }

  @After
  public void tearDown() {
    if (tronJsonRpc != null) {
      tronJsonRpc.close();
    }
    if (context != null) {
      if (context.isActive()) {
        context.close();
      }
      Args.clearParam();
    }
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(false);
    CommonParameter.getInstance().setJsonRpcHttpSolidityNodeEnable(false);
  }

  private ExecutorService filterEs() {
    return ReflectUtils.getFieldValue(tronJsonRpc, "filterEs");
  }

  private static Optional<Thread> filterThread() {
    return Thread.getAllStackTraces().entrySet().stream()
        .filter(e -> FILTER_THREAD.equals(e.getKey().getName()) && Arrays.stream(e.getValue())
            .anyMatch(frame -> LOOP_METHOD.equals(frame.getMethodName())))
        .map(Map.Entry::getKey).findFirst();
  }

  private static boolean consumerRunning() {
    return filterThread().isPresent();
  }

  private static void await(BooleanSupplier condition, String message)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(50);
    }
    Assert.fail(message);
  }

  private static long timedClose(TronJsonRpcImpl rpc) {
    long t0 = System.nanoTime();
    rpc.close();
    return (System.nanoTime() - t0) / 1_000_000;
  }

  @Test
  public void disabledModeStartsNoConsumerAndCloseIsSafe() throws Exception {
    startStandalone(false);
    Assert.assertNull("consumer started while both filter APIs are disabled", filterEs());

    tronJsonRpc.close();
    assertPoolsTerminated();
    tronJsonRpc.close();
    assertPoolsTerminated();
    Assert.assertNull(filterEs());
  }

  private void assertPoolsTerminated() {
    Assert.assertTrue("logs-filter-pool not terminated", ReflectUtils.<ExecutorService>
        getFieldValue(tronJsonRpc, "logsFilterPool").isTerminated());
    Assert.assertTrue("query-section pool not terminated", ReflectUtils.<ExecutorService>
        getFieldValue(tronJsonRpc, "sectionExecutor").isTerminated());
  }

  /**
   * Built like FullNode (circular references disallowed); context.close() is the
   * production sequence: ApplicationImpl.shutdown() closes the bean explicitly, then bean
   * destruction invokes the inferred close() again. Spring logs rather than propagates a
   * failure of that second call, so the consumer state below - not the absence of an
   * exception - is what this pins down.
   */
  @Test
  public void explicitCloseThenContextDestruction() throws Exception {
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString()}, TestConstants.TEST_CONF);
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(true);
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    context = new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    TronJsonRpcImpl bean = context.getBean(TronJsonRpcImpl.class);
    ExecutorService beanFilterEs = ReflectUtils.getFieldValue(bean, "filterEs");
    Assert.assertNotNull("consumer did not start", beanFilterEs);

    context.close();
    Assert.assertTrue("consumer survived context close", beanFilterEs.isTerminated());
    long elapsedMs = timedClose(bean);
    Assert.assertTrue("close() after destruction should be harmless and quick, took "
        + elapsedMs + "ms", elapsedMs < 1_000);
  }

  @Test
  public void emptyQueueStopsWithoutForcedCancellation() throws Exception {
    startStandalone(true);
    await(FilterPipelineLifecycleTest::consumerRunning, "consumer did not start");

    long elapsedMs = timedClose(tronJsonRpc);
    Assert.assertTrue("graceful stop took " + elapsedMs + "ms", elapsedMs < GRACEFUL_BOUND_MS);
    Assert.assertTrue("consumer still alive after close()", filterEs().isTerminated());
  }

  /**
   * Interrupt without close(): the stop flag still says running, so only the
   * InterruptedException branch can end the loop. The executor worker outlives the task,
   * hence the stack-frame check instead of thread liveness.
   */
  @Test
  public void interruptDuringPollExitsLoop() throws Exception {
    startStandalone(true);
    await(FilterPipelineLifecycleTest::consumerRunning, "consumer did not start");

    filterThread().get().interrupt();
    await(() -> !consumerRunning(), "loop kept running after interrupt");

    queue.offer(new BlockFilterCapsule("0xafter-interrupt", false));
    Thread.sleep(500);
    Assert.assertEquals("capsule consumed after the loop exited", 1, queue.stream().count());
  }

  /**
   * The closing thread is interrupted while the consumer is mid-capsule: close() gives up
   * waiting, keeps the interrupt status and still completes; the consumer is cancelled.
   */
  @Test
  public void interruptedCloserReturnsAndPreservesInterruptStatus() throws Exception {
    startStandalone(true);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    queue.offer(new LogsFilterCapsule(150L, "0xblocked", null,
        Collections.emptyList(), false, false) {
      @Override
      public boolean isSolidified() {
        entered.countDown();
        try {
          release.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return false;
      }
    });
    Assert.assertTrue("consumer did not pick up the capsule",
        entered.await(10, TimeUnit.SECONDS));

    AtomicBoolean interruptKept = new AtomicBoolean(false);
    AtomicReference<Throwable> closeError = new AtomicReference<>();
    Thread closer = new Thread(() -> {
      try {
        tronJsonRpc.close();
      } catch (Throwable t) {
        closeError.set(t);
      } finally {
        interruptKept.set(Thread.currentThread().isInterrupted());
      }
    }, "test-closer");
    closer.start();
    try {
      await(filterEs()::isShutdown, "close() did not start stopping the consumer");
      Assert.assertTrue("close() finished while a capsule was in flight", closer.isAlive());

      closer.interrupt();
      closer.join(GRACEFUL_BOUND_MS);
      Assert.assertFalse("interrupted close() did not return", closer.isAlive());
      Assert.assertNull("close() failed", closeError.get());
      Assert.assertTrue("close() swallowed the interrupt status", interruptKept.get());

      // assert before releasing the latch: a released consumer would finish on its own,
      // so only a still-blocked one proves close() cancelled it
      Assert.assertTrue("consumer was not cancelled",
          filterEs().awaitTermination(10, TimeUnit.SECONDS));
    } finally {
      release.countDown();
    }
  }
}
