package org.tron.core.jsonrpc;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.capsule.LogsFilterCapsule;
import org.tron.common.logsfilter.queue.FilterCapsuleQueue;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.LogFilterAndResult;
import org.tron.protos.Protocol.TransactionInfo;

/**
 * Shutdown semantics of the decoupled filter pipeline, on a directly constructed instance.
 */
public class FilterPipelineShutdownTest {

  private final FilterCapsuleQueue filterCapsuleQueue = new FilterCapsuleQueue();
  private TronJsonRpcImpl tronJsonRpc;

  @Before
  public void setUp() {
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(true);
    tronJsonRpc = new TronJsonRpcImpl(null, null, null);
    ReflectUtils.setFieldValue(tronJsonRpc, "filterCapsuleQueue", filterCapsuleQueue);
    ReflectUtils.invokeMethod(tronJsonRpc, "start");
  }

  @After
  public void tearDown() {
    tronJsonRpc.close();
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(false);
  }

  private static TransactionInfo buildTxInfoWithLog() {
    LogInfo logInfo = new LogInfo(new byte[20],
        Collections.singletonList(new DataWord(new byte[32])), new byte[0]);
    return TransactionInfo.newBuilder().addLog(LogInfo.buildLog(logInfo)).build();
  }

  /**
   * close() while the consumer is mid-capsule on the parallel (logsFilterPool) path:
   * the in-flight capsule must complete instead of dying on a RejectedExecutionException,
   * which is exactly the filterEs-before-logsFilterPool ordering constraint. A capsule
   * queued behind it is not drained.
   */
  @Test
  public void closeDuringParallelProcessingLosesNoEvent() throws Exception {
    tronJsonRpc.setFilterParallelThreshold(0);
    LogFilterAndResult filter = new LogFilterAndResult(new FilterRequest(), 100L, null);
    tronJsonRpc.getEventFilter2ResultFull().put("shutdown-race", filter);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    LogsFilterCapsule capsule = new LogsFilterCapsule(150L, "0xrace", null,
        Collections.singletonList(buildTxInfoWithLog()), false, false) {
      @Override
      public boolean isSolidified() {
        // first call is the head of handleLogsFilter, on the consumer thread
        entered.countDown();
        try {
          release.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return false;
      }
    };
    filterCapsuleQueue.offer(capsule);
    Assert.assertTrue("consumer did not pick up the capsule",
        entered.await(10, TimeUnit.SECONDS));
    LogsFilterCapsule queued = new LogsFilterCapsule(151L, "0xqueued", null,
        Collections.singletonList(buildTxInfoWithLog()), false, false);
    filterCapsuleQueue.offer(queued);

    ExecutorService filterEs = ReflectUtils.getFieldValue(tronJsonRpc, "filterEs");
    Thread closer = new Thread(tronJsonRpc::close, "test-closer");
    closer.start();
    try {
      // close() must be parked awaiting the consumer, with logsFilterPool still open
      long deadline = System.currentTimeMillis() + 10_000;
      while (!filterEs.isShutdown() && System.currentTimeMillis() < deadline) {
        Thread.sleep(10);
      }
      Assert.assertTrue("close() did not start stopping the consumer", filterEs.isShutdown());
      Assert.assertTrue("close() finished while a capsule was in flight", closer.isAlive());
    } finally {
      release.countDown();
    }

    closer.join(150_000);
    Assert.assertFalse("close() did not finish", closer.isAlive());
    Assert.assertTrue("consumer still running after close()", filterEs.isTerminated());
    Assert.assertEquals("in-flight capsule was lost during close()",
        1, filter.getResult().size());
    Assert.assertTrue("queued capsule was drained during close()",
        filterCapsuleQueue.stream().anyMatch(c -> c == queued));
  }
}
