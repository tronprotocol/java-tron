package org.tron.core.jsonrpc;

import static org.tron.common.math.Maths.random;
import static org.tron.common.math.Maths.round;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.logsfilter.capsule.BlockFilterCapsule;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.BlockFilterAndResult;

@Slf4j
public class ConcurrentHashMapTest {
  private static final String EXECUTOR_NAME = "jsonrpc-concurrent-map-test";
  private final TronJsonRpcImpl jsonRpc = new TronJsonRpcImpl(null, null);

  private static int randomInt(int minInt, int maxInt) {
    return (int) round(random(true) * (maxInt - minInt) + minInt, true);
  }

  /**
   * test producer and consumer model in getFilterChanges after newBlockFilter.
   * Firstly, sum of all consumers' number of messages is same as producer generates.
   * Secondly, message of every consumer is continuous, not interject with another
   * when consumes parallel.
   */
  @Test
  public void testHandleBlockHash() {
    int times = 100;
    int eachCount = 200;

    Map<String, BlockFilterAndResult> conMap = jsonRpc.getBlockFilter2ResultFull();
    Map<String, List<String>> resultMap1 = new ConcurrentHashMap<>(); // used to check result
    Map<String, List<String>> resultMap2 = new ConcurrentHashMap<>(); // used to check result
    Map<String, List<String>> resultMap3 = new ConcurrentHashMap<>(); // used to check result

    for (int i = 0; i < 5; i++) {
      BlockFilterAndResult filterAndResult = new BlockFilterAndResult();
      String filterID = String.valueOf(i);

      conMap.put(filterID, filterAndResult);
      resultMap1.put(filterID, new ArrayList<>());
      resultMap2.put(filterID, new ArrayList<>());
      resultMap3.put(filterID, new ArrayList<>());
    }

    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      Assert.fail("Interrupted during test setup: " + e.getMessage());
    }

    ExecutorService executor = ExecutorServiceManager.newFixedThreadPool(EXECUTOR_NAME, 4, true);

    try {
      Future<?> putTask = executor.submit(() -> {
        for (int i = 1; i <= times; i++) {
          logger.info("put time {}, from {} to {}", i, (1 + (i - 1) * eachCount), i * eachCount);

          for (int j = 1 + (i - 1) * eachCount; j <= i * eachCount; j++) {
            BlockFilterCapsule blockFilterCapsule =
                new BlockFilterCapsule(String.valueOf(j), false);
            jsonRpc.handleBLockFilter(blockFilterCapsule);
          }
          try {
            Thread.sleep(randomInt(50, 100));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("putThread interrupted", e);
          }
        }
      });

      Future<?> getTask1 = executor.submit(() -> {
        for (int t = 1; t <= times * 2; t++) {

          try {
            Thread.sleep(randomInt(50, 100));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("getThread1 interrupted", e);
          }

          logger.info("Thread1 get time {}", t);

          for (int k = 0; k < 5; k++) {
            try {
              Object[] blockHashList = jsonRpc.getFilterResult(String.valueOf(k), conMap,
                  jsonRpc.getEventFilter2ResultFull());

              for (Object str : blockHashList) {
                resultMap1.get(String.valueOf(k)).add(str.toString());
              }

            } catch (ItemNotFoundException e) {
              Assert.fail("Filter ID should always exist: " + e.getMessage());
            }
          }
        }
      });

      Future<?> getTask2 = executor.submit(() -> {
        for (int t = 1; t <= times * 2; t++) {

          try {
            Thread.sleep(randomInt(50, 100));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("getThread2 interrupted", e);
          }

          logger.info("Thread2 get time {}", t);

          for (int k = 0; k < 5; k++) {
            try {
              Object[] blockHashList = jsonRpc.getFilterResult(String.valueOf(k), conMap,
                  jsonRpc.getEventFilter2ResultFull());

              // if (blockHashList.length == 0) {
              //   continue;
              // }

              for (Object str : blockHashList) {
                resultMap2.get(String.valueOf(k)).add(str.toString());
              }

            } catch (ItemNotFoundException e) {
              Assert.fail("Filter ID should always exist: " + e.getMessage());
            }
          }
        }
      });

      Future<?> getTask3 = executor.submit(() -> {
        for (int t = 1; t <= times * 2; t++) {

          try {
            Thread.sleep(randomInt(50, 100));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("getThread3 interrupted", e);
          }

          logger.info("Thread3 get time {}", t);

          for (int k = 0; k < 5; k++) {
            try {
              Object[] blockHashList = jsonRpc.getFilterResult(String.valueOf(k), conMap,
                  jsonRpc.getEventFilter2ResultFull());

              for (Object str : blockHashList) {
                try {
                  resultMap3.get(String.valueOf(k)).add(str.toString());
                } catch (Exception e) {
                  throw new AssertionError("resultMap3 get " + k + " exception", e);
                }
              }

            } catch (ItemNotFoundException e) {
              Assert.fail("Filter ID should always exist: " + e.getMessage());
            }
          }
        }
      });

      for (Future<?> future : new Future<?>[] {putTask, getTask1, getTask2, getTask3}) {
        try {
          future.get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          Assert.fail("Main thread interrupted while waiting for worker threads: "
              + e.getMessage());
        } catch (ExecutionException e) {
          Assert.fail("Worker thread failed: " + e.getCause());
        }
      }
    } finally {
      ExecutorServiceManager.shutdownAndAwaitTermination(executor, EXECUTOR_NAME);
    }

    logger.info("-----------------------------------------------------------------------");

    for (int i = 0; i < 5; i++) {
      List<String> pResult = resultMap1.get(String.valueOf(i));
      pResult.addAll(resultMap2.get(String.valueOf(i)));
      pResult.addAll(resultMap3.get(String.valueOf(i)));

      for (int j = 1; j <= times * eachCount; j++) {
        // if (!pResult.contains(ByteArray.toJsonHex(String.valueOf(j)))) {
        //   logger.info("key {} not contains {}", i, j);
        // }
        Assert.assertTrue(pResult.contains(ByteArray.toJsonHex(String.valueOf(j))));
      }

      Assert.assertEquals(times * eachCount, pResult.size());
    }
  }

}
