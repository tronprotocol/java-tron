package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.ForwardingCache;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.tron.common.TestConstants;
import org.tron.common.cache.TronCache;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;

/**
 * Deterministic concurrency tests for the SnapshotRoot read-miss refill vs flush publish race.
 * A stale value a reader read before a flush must never end up (or be reintroduced) in the shared
 * root cache, because block-processing reads consume that same cache.
 */
public class SnapshotRootCacheRaceTest {

  private static final byte[] K = "key".getBytes();
  private static final byte[] OLD = "OLD".getBytes();
  private static final byte[] NEW = "NEW".getBytes();
  private static final String CACHED_DB = "storage-row";   // in CACHE_DBS → root cache on
  private static final String UNCACHED_DB = "block";        // not cached
  private static final String ACCOUNT_DB = "account";       // cached, asset optimization path
  private static final String EXECUTOR_NAME = "snapshot-root-cache-race";

  private static ExecutorService exec;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    exec = ExecutorServiceManager.newFixedThreadPool(EXECUTOR_NAME, 4, true);
  }

  @AfterClass
  public static void destroy() throws InterruptedException {
    try {
      if (exec != null) {
        exec.shutdownNow();
        assertTrue("race test workers did not terminate",
            exec.awaitTermination(5, TimeUnit.SECONDS));
      }
    } finally {
      Args.clearParam();
    }
  }

  /** A reader that read OLD then paused must not overwrite the NEW a concurrent write published. */
  @Test
  public void staleReadDoesNotOverwriteNewPublish() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      // reader parked inside db.get holding its observed gen; a timeout means the interleaving
      // never formed and the assertions below would be vacuous, so fail loudly instead.
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      root.put(K, NEW);                      // authoritative write: db=NEW, cache=NEW, gen++
    } finally {
      release.countDown();                   // always release the parked reader
    }

    // reader's own return may lag (benign; solidity view is read-only)
    assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(NEW, cached.getBytes());                 // cache holds NEW, not stale OLD
    assertArrayEquals(NEW, root.get(K));
  }

  /** Even if NEW is evicted during the reader's pause, the late reader must not reinstate OLD. */
  @Test
  public void evictionDoesNotReintroduceStale() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      root.put(K, NEW);        // cache=NEW, gen++
      evict(root, K);          // simulate capacity eviction of the NEW entry
    } finally {
      release.countDown();
    }

    reader.get(5, TimeUnit.SECONDS);
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    assertNull("stale read must not reinstate a value", peek(root, K));  // reader skipped refill
    assertArrayEquals(NEW, root.get(K));    // re-reads DB=NEW (no concurrent writer → refills)
    WrappedByteArray refilled = peek(root, K);
    assertNotNull("a miss after the write completed must refill again", refilled);
    assertArrayEquals(NEW, refilled.getBytes());
  }

  /**
   * The generation must only move forward: two writes while one read is parked must not bring it
   * back to the value the reader captured.
   */
  @Test
  public void staleReadAcrossTwoWritesIsSkipped() throws Exception {
    byte[] newer = "NEWER".getBytes();
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      root.put(K, NEW);
      root.put(K, newer);
    } finally {
      release.countDown();
    }

    assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(newer, cached.getBytes());
    assertArrayEquals(newer, root.get(K));
  }

  /** New-account (OLD=null) negative value and delete go through the generation protocol. */
  @Test
  public void negativeValueAndDelete() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);
    assertArrayEquals(OLD, root.get(K));           // warms cache

    root.remove(K);                                 // db delete + negative cache publish
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);                          // negative entry present
    assertNull(cached.getBytes());
    assertNull(root.get(K));
  }

  /** An uncached store must keep working (read-through) and never NPE through the protocol. */
  @Test
  public void uncachedStoreReadsThrough() {
    FakeFlushDB db = new FakeFlushDB(UNCACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);
    assertArrayEquals(OLD, root.get(K));
    root.put(K, NEW);
    assertArrayEquals(NEW, root.get(K));
    root.remove(K);
    assertNull(root.get(K));
  }

  /**
   * merge(Snapshot) is reached on the root only from SnapshotManager.recover() at startup. It
   * must still publish NEW and bump the generation just like put, so a parked stale reader
   * cannot overwrite it.
   */
  @Test
  public void mergeFlushBeatsStaleRead() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      SnapshotImpl layer = new SnapshotImpl(root);
      layer.put(K, NEW);
      root.merge((Snapshot) layer);        // flush db=NEW, publish NEW, gen++
    } finally {
      release.countDown();
    }

    reader.get(5, TimeUnit.SECONDS);
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(NEW, cached.getBytes());
    assertArrayEquals(NEW, root.get(K));
  }

  /**
   * The batch flush path merge(List) is what SnapshotManager.refreshOne() actually drives on the
   * flush-service thread — the one that genuinely races reads — and it is a separate method body
   * from merge(Snapshot), so it needs its own interleaving test: it too must publish NEW and bump
   * the generation, otherwise a parked stale reader would overwrite the flushed value.
   */
  @Test
  public void mergeListFlushBeatsStaleRead() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      SnapshotImpl layer = new SnapshotImpl(root);
      layer.put(K, NEW);
      root.merge(Collections.singletonList((Snapshot) layer));   // refreshOne() path
    } finally {
      release.countDown();
    }

    reader.get(5, TimeUnit.SECONDS);
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(NEW, cached.getBytes());
    assertArrayEquals(NEW, root.get(K));
  }

  /**
   * The batch publish runs outside cacheLock, so a stale reader can resume while the writer is
   * still publishing; the generation must already be bumped by then, or the reader overwrites NEW.
   */
  @Test
  public void staleReadDuringBatchPublishIsSkipped() throws Exception {
    assertStaleReadDuringPublishIsSkipped(NEW, SnapshotRootCacheRaceTest::mergeNewLayer);
  }

  /** Same as above for the single-key put(): the generation must advance before NEW is cached. */
  @Test
  public void staleReadDuringPutPublishIsSkipped() throws Exception {
    assertStaleReadDuringPublishIsSkipped(NEW, root -> root.put(K, NEW));
  }

  /** Same for remove(): a stale reader must not overwrite the negative entry being published. */
  @Test
  public void staleReadDuringRemovePublishIsSkipped() throws Exception {
    assertStaleReadDuringPublishIsSkipped(null, root -> root.remove(K));
  }

  /**
   * A stale reader that passed the lock-free check and then waits for the read lock must recheck
   * the generation after acquiring it: a write may advance it while the reader is waiting.
   */
  @Test
  public void staleReadWaitingOnLockIsSkipped() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);
    ReentrantReadWriteLock lock = cacheLock(root);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    AtomicReference<Thread> readerThread = new AtomicReference<>();
    Future<byte[]> reader = exec.submit(() -> {
      readerThread.set(Thread.currentThread());
      return root.get(K);
    });
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      lock.writeLock().lock();
      try {
        release.countDown();                 // reader passes the lock-free check, then waits
        assertTrue("reader did not queue on cacheLock", awaitQueued(lock, readerThread.get()));
        root.put(K, NEW);                    // reentrant: advances the generation, caches NEW
      } finally {
        lock.writeLock().unlock();
      }
    } finally {
      release.countDown();
    }

    assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(NEW, cached.getBytes());
    assertUnlocked(lock);
    assertArrayEquals(NEW, root.get(K));
  }

  /** Refills only share the read lock, so one in flight must not hold up or drop another. */
  @Test
  public void concurrentRefillsDoNotBlockEachOther() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);
    ReentrantReadWriteLock lock = cacheLock(root);

    lock.readLock().lock();                  // stands in for another refill in flight
    try {
      Future<byte[]> reader = exec.submit(() -> root.get(K));
      assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));
      WrappedByteArray cached = peek(root, K);
      assertNotNull("a concurrent refill must still cache its value", cached);
      assertArrayEquals(OLD, cached.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    assertUnlocked(lock);
  }

  /**
   * A reader whose DB read starts while the write is still in flight reads OLD; if its refill lands
   * after the write finishes, it must be skipped. That holds only if the writer completes the DB
   * write before advancing the generation, and advances it before publishing.
   */
  @Test
  public void putWritesDbBeforePublish() throws Exception {
    assertDbWriteCompletesBeforePublish(NEW, root -> root.put(K, NEW));
  }

  /** Same for remove(). */
  @Test
  public void removeWritesDbBeforePublish() throws Exception {
    assertDbWriteCompletesBeforePublish(null, root -> root.remove(K));
  }

  /** Same for the batch flush path merge(List). */
  @Test
  public void mergeListWritesDbBeforePublish() throws Exception {
    assertDbWriteCompletesBeforePublish(NEW, SnapshotRootCacheRaceTest::mergeNewLayer);
  }

  /**
   * Same for the account store with asset optimization on, where merge goes through
   * processAccount(): it must also flush before publishing.
   */
  @Test
  public void accountMergeWritesDbBeforePublish() throws Throwable {
    byte[] oldAccount = account(1L);
    byte[] newAccount = account(2L);
    withAssetOptimization(() -> assertDbWriteCompletesBeforePublish(ACCOUNT_DB, oldAccount,
        newAccount, root -> mergeLayer(root, newAccount)));
  }

  /** Same for merge(Snapshot), used by recover() at startup. */
  @Test
  public void mergeSnapshotWritesDbBeforePublish() throws Exception {
    assertDbWriteCompletesBeforePublish(NEW, root -> {
      SnapshotImpl layer = new SnapshotImpl(root);
      layer.put(K, NEW);
      root.merge((Snapshot) layer);
    });
  }

  /**
   * A writer must not advance the generation while a refill that already passed the recheck is
   * caching its value: the bump has to wait for cacheLock, so the publish lands after the refill.
   */
  @Test
  public void putWaitsForInFlightRefill() throws Exception {
    assertPublishWaitsForInFlightRefill(root -> root.put(K, NEW));
  }

  /** Same for the batch flush path merge(List). */
  @Test
  public void mergeListWaitsForInFlightRefill() throws Exception {
    assertPublishWaitsForInFlightRefill(SnapshotRootCacheRaceTest::mergeNewLayer);
  }

  private void assertDbWriteCompletesBeforePublish(byte[] value, Consumer<SnapshotRoot> write)
      throws Exception {
    assertDbWriteCompletesBeforePublish(CACHED_DB, OLD, value, write);
  }

  private void assertDbWriteCompletesBeforePublish(String dbName, byte[] old, byte[] value,
      Consumer<SnapshotRoot> write) throws Exception {
    FakeFlushDB db = new FakeFlushDB(dbName);
    db.seed(K, old);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch writeEntered = new CountDownLatch(1);
    CountDownLatch writeRelease = new CountDownLatch(1);
    db.armWrite(writeEntered, writeRelease);
    CountDownLatch readEntered = new CountDownLatch(1);
    CountDownLatch readRelease = new CountDownLatch(1);

    Future<?> writer = exec.submit(() -> write.accept(root));
    try {
      assertTrue("writer did not reach the DB write gate",
          writeEntered.await(30, TimeUnit.SECONDS));
      evict(root, K);                        // drop anything cached ahead of the DB write
      db.arm(K, readEntered, readRelease);
      Future<byte[]> reader = exec.submit(() -> root.get(K));  // misses, reads old, parks
      assertTrue("reader did not reach the DB gate", readEntered.await(30, TimeUnit.SECONDS));
      writeRelease.countDown();
      writer.get(5, TimeUnit.SECONDS);       // DB write, generation advance and publish all done
      readRelease.countDown();               // the reader refills only now
      assertArrayEquals(old, reader.get(5, TimeUnit.SECONDS));
    } finally {
      writeRelease.countDown();
      readRelease.countDown();
    }

    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(value, cached.getBytes());
    assertArrayEquals(value, root.get(K));
  }

  private void assertPublishWaitsForInFlightRefill(Consumer<SnapshotRoot> write)
      throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);
    ReentrantReadWriteLock lock = cacheLock(root);

    CountDownLatch refilling = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    pauseOnCachePut(root, OLD, true, refilling, resume);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    AtomicReference<Thread> writerThread = new AtomicReference<>();
    Future<?> writer;
    try {
      assertTrue("reader did not start its refill", refilling.await(30, TimeUnit.SECONDS));
      writer = exec.submit(() -> {
        writerThread.set(Thread.currentThread());
        write.accept(root);
      });
      assertTrue("writer neither queued nor finished",
          awaitQueuedOrDone(lock, writerThread, writer));
    } finally {
      resume.countDown();
    }

    assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));
    writer.get(5, TimeUnit.SECONDS);
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(NEW, cached.getBytes());
    assertUnlocked(lock);
    assertArrayEquals(NEW, root.get(K));
  }

  private void assertStaleReadDuringPublishIsSkipped(byte[] value, Consumer<SnapshotRoot> write)
      throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch published = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);
    pauseOnCachePut(root, value, false, published, resume);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    Future<?> writer;
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      writer = exec.submit(() -> write.accept(root));
      assertTrue("writer did not publish", published.await(30, TimeUnit.SECONDS));
      release.countDown();                   // stale reader resumes mid-publish
      assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));
    } finally {
      release.countDown();
      resume.countDown();
    }

    writer.get(5, TimeUnit.SECONDS);
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(value, cached.getBytes());
    assertArrayEquals(value, root.get(K));
  }

  /**
   * Concurrent delete: a reader that read OLD then paused must not resurrect the key by refilling
   * OLD over the negative (deleted) entry a concurrent remove published.
   */
  @Test
  public void staleReadDoesNotOverwriteDelete() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      root.remove(K);                      // db delete + negative publish + gen++
    } finally {
      release.countDown();
    }

    assertArrayEquals(OLD, reader.get(5, TimeUnit.SECONDS));   // reader's own stale return (benign)
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull("delete must leave a negative cache entry", cached);
    assertNull("stale read must not resurrect a deleted key", cached.getBytes());
    assertNull(root.get(K));
  }

  /**
   * Reverse of the delete case: a reader that found the key absent must not cache that absence
   * over the value a concurrent put created.
   */
  @Test
  public void staleAbsentReadDoesNotOverwriteCreate() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    SnapshotRoot root = new SnapshotRoot(db);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    db.arm(K, entered, release);

    Future<byte[]> reader = exec.submit(() -> root.get(K));
    try {
      assertTrue("reader did not reach the DB gate", entered.await(30, TimeUnit.SECONDS));
      root.put(K, NEW);                      // creates the key: db=NEW, cache=NEW, gen++
    } finally {
      release.countDown();
    }

    assertNull(reader.get(5, TimeUnit.SECONDS));
    assertFalse("gate release timed out — interleaving not achieved", db.gateTimedOut());
    WrappedByteArray cached = peek(root, K);
    assertNotNull(cached);
    assertArrayEquals(NEW, cached.getBytes());
    assertArrayEquals(NEW, root.get(K));
  }

  /** No-regression: a cache miss with no concurrent write MUST still refill the cache. */
  @Test
  public void missWithoutConcurrentWriteRefills() throws Exception {
    FakeFlushDB db = new FakeFlushDB(CACHED_DB);
    db.seed(K, OLD);
    SnapshotRoot root = new SnapshotRoot(db);

    assertArrayEquals(OLD, root.get(K));
    WrappedByteArray cached = peek(root, K);
    assertNotNull("plain miss must refill", cached);
    assertArrayEquals(OLD, cached.getBytes());
    assertUnlocked(cacheLock(root));
  }

  // ---- helpers ----

  @SuppressWarnings("unchecked")
  private static TronCache<WrappedByteArray, WrappedByteArray> tronCache(SnapshotRoot root)
      throws Exception {
    Field f = SnapshotRoot.class.getDeclaredField("cache");
    f.setAccessible(true);
    return (TronCache<WrappedByteArray, WrappedByteArray>) f.get(root);
  }

  private static WrappedByteArray peek(SnapshotRoot root, byte[] key) throws Exception {
    return tronCache(root).getIfPresent(WrappedByteArray.of(key));
  }

  @SuppressWarnings("unchecked")
  private static Cache<WrappedByteArray, WrappedByteArray> guava(SnapshotRoot root)
      throws Exception {
    return (Cache<WrappedByteArray, WrappedByteArray>) guavaField().get(tronCache(root));
  }

  private static ReentrantReadWriteLock cacheLock(SnapshotRoot root) throws Exception {
    Field f = SnapshotRoot.class.getDeclaredField("cacheLock");
    f.setAccessible(true);
    return (ReentrantReadWriteLock) f.get(root);
  }

  private static void assertUnlocked(ReentrantReadWriteLock lock) {
    assertFalse("cacheLock write lock still held", lock.isWriteLocked());
    assertEquals("cacheLock read lock still held", 0, lock.getReadLockCount());
  }

  private static boolean awaitQueued(ReentrantReadWriteLock lock, Thread thread)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (!lock.hasQueuedThread(thread)) {
      if (System.nanoTime() > deadline) {
        return false;
      }
      Thread.sleep(1);
    }
    return true;
  }

  private static boolean awaitQueuedOrDone(ReentrantReadWriteLock lock,
      AtomicReference<Thread> thread, Future<?> task) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (!task.isDone()) {
      Thread t = thread.get();
      if (t != null && lock.hasQueuedThread(t)) {
        return true;
      }
      if (System.nanoTime() > deadline) {
        return false;
      }
      Thread.sleep(1);
    }
    return true;
  }

  private static void mergeNewLayer(SnapshotRoot root) {
    mergeLayer(root, NEW);
  }

  private static void mergeLayer(SnapshotRoot root, byte[] value) {
    SnapshotImpl layer = new SnapshotImpl(root);
    layer.put(K, value);
    root.merge(Collections.singletonList((Snapshot) layer));
  }

  private static byte[] account(long balance) {
    AccountCapsule account = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(K), AccountType.Normal, balance);
    account.setAssetOptimized(true);
    return account.getData();
  }

  /** Runs {@code body} with a stub ChainBaseManager whose properties report asset optimization. */
  private static void withAssetOptimization(ThrowingRunnable body) throws Throwable {
    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    when(properties.getAllowAccountAssetOptimizationFromRoot()).thenReturn(1L);
    ChainBaseManager manager = mock(ChainBaseManager.class);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    when(manager.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));
    Field f = ChainBaseManager.class.getDeclaredField("chainBaseManager");
    f.setAccessible(true);
    Object previous = f.get(null);
    f.set(null, manager);
    try {
      body.run();
    } finally {
      f.set(null, previous);
    }
  }

  private static Field guavaField() throws Exception {
    Field f = TronCache.class.getDeclaredField("cache");
    f.setAccessible(true);
    return f;
  }

  private static void evict(SnapshotRoot root, byte[] key) throws Exception {
    guava(root).invalidate(WrappedByteArray.of(key));
  }

  /**
   * Parks, once, the thread that puts {@code value} into the root cache: just before the put when
   * {@code beforePut} is set, otherwise right after it lands.
   */
  private static void pauseOnCachePut(SnapshotRoot root, byte[] value, boolean beforePut,
      CountDownLatch reached, CountDownLatch resume) throws Exception {
    Cache<WrappedByteArray, WrappedByteArray> delegate = guava(root);
    AtomicBoolean armed = new AtomicBoolean(true);
    guavaField().set(tronCache(root),
        new ForwardingCache.SimpleForwardingCache<WrappedByteArray, WrappedByteArray>(delegate) {
          @Override
          public void put(WrappedByteArray k, WrappedByteArray v) {
            boolean pause = Arrays.equals(value, v.getBytes()) && armed.compareAndSet(true, false);
            if (pause && beforePut) {
              park(reached, resume);
            }
            super.put(k, v);
            if (pause && !beforePut) {
              park(reached, resume);
            }
          }
        });
  }

  private static void park(CountDownLatch reached, CountDownLatch resume) {
    reached.countDown();
    try {
      if (!resume.await(30, TimeUnit.SECONDS)) {
        throw new AssertionError("paused thread was never resumed");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * In-memory DB + Flusher with a one-shot latch gate on a chosen key's get(), and a one-shot gate
   * that parks the next put / remove / flush before it applies the write.
   */
  private static final class FakeFlushDB implements DB<byte[], byte[]>, Flusher {

    private final Map<WrappedByteArray, byte[]> map = new ConcurrentHashMap<>();
    private final String name;
    private volatile WrappedByteArray gateKey;
    private volatile CountDownLatch entered;
    private volatile CountDownLatch release;
    private volatile CountDownLatch writeEntered;
    private volatile CountDownLatch writeRelease;
    private volatile boolean gateTimedOut;

    private FakeFlushDB(String name) {
      this.name = name;
    }

    private void seed(byte[] k, byte[] v) {
      map.put(WrappedByteArray.of(k), v);
    }

    private void arm(byte[] k, CountDownLatch entered, CountDownLatch release) {
      this.gateKey = WrappedByteArray.of(k);
      this.entered = entered;
      this.release = release;
    }

    private void armWrite(CountDownLatch entered, CountDownLatch release) {
      this.writeEntered = entered;
      this.writeRelease = release;
    }

    private boolean gateTimedOut() {
      return gateTimedOut;
    }

    private void writeGate() {
      CountDownLatch e = writeEntered;
      if (e == null) {
        return;
      }
      writeEntered = null;              // one-shot: later writes are not gated
      e.countDown();
      awaitRelease(writeRelease);
    }

    // A timeout here means the test never released the gate, so the intended read-vs-write
    // interleaving did not form; record it so the test fails loudly instead of passing vacuously.
    private void awaitRelease(CountDownLatch r) {
      try {
        if (!r.await(30, TimeUnit.SECONDS)) {
          gateTimedOut = true;
        }
      } catch (InterruptedException ex) {
        gateTimedOut = true;
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public byte[] get(byte[] k) {
      WrappedByteArray wk = WrappedByteArray.of(k);
      // Capture the value at call entry (models LevelDB/RocksDB fixing its read sequence up front:
      // a flush that commits during the pause below is invisible to this read), THEN park.
      byte[] v = map.get(wk);
      if (wk.equals(gateKey)) {
        gateKey = null;                 // one-shot: later reads are not gated
        CountDownLatch e = entered;
        CountDownLatch r = release;
        if (e != null) {
          e.countDown();
        }
        if (r != null) {
          awaitRelease(r);
        }
      }
      return v;
    }

    @Override
    public void put(byte[] k, byte[] v) {
      writeGate();
      map.put(WrappedByteArray.of(k), v);
    }

    @Override
    public void remove(byte[] k) {
      writeGate();
      map.remove(WrappedByteArray.of(k));
    }

    @Override
    public long size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public String getDbName() {
      return name;
    }

    @Override
    public Iterator<Map.Entry<byte[], byte[]>> iterator() {
      List<Map.Entry<byte[], byte[]>> entries = new ArrayList<>();
      for (Map.Entry<WrappedByteArray, byte[]> e : map.entrySet()) {
        entries.add(new java.util.AbstractMap.SimpleImmutableEntry<>(
            e.getKey().getBytes(), e.getValue()));
      }
      return entries.iterator();
    }

    @Override
    public void close() {
      map.clear();
    }

    @Override
    public void stat() {
    }

    @Override
    public FakeFlushDB newInstance() {
      return new FakeFlushDB(name);
    }

    @Override
    public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
      writeGate();
      batch.forEach((k, v) -> {
        if (v == null || v.getBytes() == null) {
          map.remove(k);
        } else {
          map.put(k, v.getBytes());
        }
      });
    }

    @Override
    public void reset() {
      map.clear();
    }
  }
}
