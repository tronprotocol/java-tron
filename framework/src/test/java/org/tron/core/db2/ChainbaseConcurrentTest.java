package org.tron.core.db2;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.rocksdb.RocksDB;
import org.tron.common.BaseMethodTest;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.SnapshotRoot;

/**
 * Verifies that concurrent setHead/prefixQuery operations on Chainbase
 * do not throw exceptions after the volatile + local snapshot fix.
 */
public class ChainbaseConcurrentTest extends BaseMethodTest {

  @Rule
  public Timeout globalTimeout = Timeout.seconds(60);

  @Override
  protected void afterInit() {
    RocksDB.loadLibrary();
  }

  @Test
  public void testConcurrentSetHeadAndPrefixQuery() throws Exception {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "testConcurrentPrefixQuery");
    Chainbase chainbase = new Chainbase(
        new SnapshotRoot(new org.tron.core.db2.common.RocksDB(dataSource)));

    try {
      byte[] prefix = "test_prefix_".getBytes();

      // Put initial data in root
      chainbase.getHead().getRoot()
          .put("test_prefix_key1".getBytes(), "value1".getBytes());
      chainbase.getHead().getRoot()
          .put("test_prefix_key2".getBytes(), "value2".getBytes());

      AtomicReference<Throwable> error = new AtomicReference<>();
      int iterations = 500;
      CountDownLatch latch = new CountDownLatch(1);
      ExecutorService executor = Executors.newFixedThreadPool(4);

      // Reader threads: call prefixQuery concurrently
      for (int t = 0; t < 3; t++) {
        executor.submit(() -> {
          try {
            latch.await();
            for (int i = 0; i < iterations; i++) {
              Map<WrappedByteArray, byte[]> result =
                  chainbase.prefixQuery(prefix);
              Assert.assertNotNull(result);
            }
          } catch (Throwable e) {
            error.compareAndSet(null, e);
          }
        });
      }

      // Writer thread: advance head concurrently
      executor.submit(() -> {
        try {
          latch.await();
          for (int i = 0; i < iterations; i++) {
            chainbase.setHead(chainbase.getHead().advance());
            chainbase.getHead().put(
                ("test_prefix_key_" + i).getBytes(),
                ("value_" + i).getBytes());
          }
        } catch (Throwable e) {
          error.compareAndSet(null, e);
        }
      });

      latch.countDown();
      executor.shutdown();
      executor.awaitTermination(30, TimeUnit.SECONDS);

      Assert.assertNull(
          "Concurrent access caused an exception: " + error.get(),
          error.get());
    } finally {
      chainbase.reset();
      chainbase.close();
    }
  }
}
