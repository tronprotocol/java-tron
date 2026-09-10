package org.tron.core.jsonrpc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.logsfilter.capsule.BlockFilterCapsule;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.BlockFilterAndResult;

public class ConcurrentHashMapTest {
  private static final String EXECUTOR_NAME = "jsonrpc-concurrent-map-test";

  @Test
  public void testHandleBlockHash() throws Exception {
    int count = 20_000;
    int filterCount = 5;
    CountDownLatch ready = new CountDownLatch(4);
    CountDownLatch producerDone = new CountDownLatch(1);
    ExecutorService executor = ExecutorServiceManager.newFixedThreadPool(EXECUTOR_NAME, 4);
    try (TronJsonRpcImpl jsonRpc = new TronJsonRpcImpl(null, null)) {
      try {
        Map<String, BlockFilterAndResult> filters = jsonRpc.getBlockFilter2ResultFull();
        for (int i = 0; i < filterCount; i++) {
          filters.put(String.valueOf(i), new BlockFilterAndResult());
        }
        List<Future<List<List<String>>>> consumers = new ArrayList<>();
        for (int consumer = 0; consumer < 3; consumer++) {
          consumers.add(executor.submit(() -> {
            List<List<String>> results = new ArrayList<>();
            for (int i = 0; i < filterCount; i++) {
              results.add(new ArrayList<>());
            }
            ready.countDown();
            Assert.assertTrue("Workers did not start", ready.await(10, TimeUnit.SECONDS));
            // Completion, rather than a fixed number of polls, determines when to stop.
            while (!producerDone.await(1, TimeUnit.MILLISECONDS)) {
              drainFilters(jsonRpc, filters, results);
            }
            drainFilters(jsonRpc, filters, results);
            return results;
          }));
        }
        Future<?> producer = executor.submit(() -> {
          try {
            ready.countDown();
            Assert.assertTrue("Workers did not start", ready.await(10, TimeUnit.SECONDS));
            for (int i = 1; i <= count; i++) {
              if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Producer cancelled");
              }
              jsonRpc.handleBLockFilter(new BlockFilterCapsule(String.valueOf(i), false));
            }
            return null;
          } finally {
            producerDone.countDown();
          }
        });
        producer.get(30, TimeUnit.SECONDS);
        List<List<List<String>>> results = new ArrayList<>();
        for (Future<List<List<String>>> consumer : consumers) {
          results.add(consumer.get(30, TimeUnit.SECONDS));
        }
        Set<String> expected = new HashSet<>();
        for (int i = 1; i <= count; i++) {
          expected.add(ByteArray.toJsonHex(String.valueOf(i)));
        }
        for (int filter = 0; filter < filterCount; filter++) {
          List<String> received = new ArrayList<>();
          for (List<List<String>> result : results) {
            received.addAll(result.get(filter));
          }
          Assert.assertEquals("Unexpected event count for filter " + filter,
              count, received.size());
          Assert.assertEquals("Missing or duplicate events for filter " + filter,
              expected, new HashSet<>(received));
          Assert.assertTrue(filters.get(String.valueOf(filter)).getResult().isEmpty());
        }
      } finally {
        executor.shutdownNow();
        Assert.assertTrue("Filter workers did not stop",
            executor.awaitTermination(5, TimeUnit.SECONDS));
      }
    }
  }

  private void drainFilters(TronJsonRpcImpl jsonRpc, Map<String, BlockFilterAndResult> filters,
      List<List<String>> results) throws ItemNotFoundException {
    for (int filter = 0; filter < results.size(); filter++) {
      Object[] batch = jsonRpc.getFilterResult(String.valueOf(filter), filters,
          jsonRpc.getEventFilter2ResultFull());
      for (Object hash : batch) {
        results.get(filter).add((String) hash);
      }
    }
  }
}
