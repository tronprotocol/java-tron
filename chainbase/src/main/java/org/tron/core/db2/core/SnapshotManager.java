package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.error.TronDBException;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StorageUtils;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.TronDatabase;
import org.tron.core.db2.ISession;
import org.tron.core.db2.archive.ArchivePersistenceException;
import org.tron.core.db2.archive.ArchiveRuntimeAttachment;
import org.tron.core.db2.archive.ArchiveStateBarrier.ArchiveStateAction;
import org.tron.core.db2.archive.ArchiveStoreScope;
import org.tron.core.db2.archive.ArchiveWalBinding;
import org.tron.core.db2.archive.ArchiveWalBindingCodec;
import org.tron.core.db2.archive.BlockChangeView;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockReverseDiffSink;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.DurableBlockReverseDiffSink;
import org.tron.core.db2.archive.DurableHistoryMarkerRangeEvidence;
import org.tron.core.db2.archive.HistoryCommitMarker;
import org.tron.core.db2.archive.OldValueCollector;
import org.tron.core.db2.stateroot.PathStateBlockTransition;
import org.tron.core.db2.stateroot.PathStateRuntimeAttachment;
import org.tron.core.db2.stateroot.PathStateSnapshotDelta;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.core.exception.TronError;
import org.tron.core.store.CheckPointV2Store;
import org.tron.core.store.CheckTmpStore;

@Slf4j(topic = "DB")
public class SnapshotManager implements RevokingDatabase {

  public static final int DEFAULT_MIN_FLUSH_COUNT = 1;
  private static final int DEFAULT_STACK_MAX_SIZE = 256;
  private static final long ONE_MINUTE_MILLS = 60*1000L;
  private static final String CHECKPOINT_V2_DIR = "checkpoint";
  @Getter
  private List<Chainbase> dbs = new ArrayList<>();
  @Getter
  private int size = 0;
  private AtomicInteger maxSize = new AtomicInteger(DEFAULT_STACK_MAX_SIZE);

  private boolean disabled = true;
  // for test
  @Getter
  private int activeSession = 0;
  // for test
  @Setter
  private boolean unChecked = true;

  private volatile int flushCount = 0;

  private volatile boolean  hitDown;

  private Map<String, ListeningExecutorService> flushServices = new HashMap<>();

  private ScheduledExecutorService pruneCheckpointThread = null;
  private final String pruneName = "checkpoint-prune";

  @Autowired
  @Setter
  @Getter
  private CheckTmpStore checkTmpStore;

  @Setter
  private volatile int maxFlushCount = DEFAULT_MIN_FLUSH_COUNT;

  private int checkpointVersion = 1;   // default v1

  private OldValueCollector oldValueCollector;
  private ArchiveRuntimeAttachment archiveRuntimeAttachment;
  private PathStateRuntimeAttachment pathStateRuntimeAttachment;
  private CommonCheckpointRuntimeAttachment commonCheckpointRuntimeAttachment;
  private Long submittedArchiveHistoryEpoch;
  private BlockReverseDiffSink blockReverseDiffSink;
  @Getter
  private volatile long archiveReadableEpoch = -1;
  private volatile ArchiveWalBinding latestArchiveWalBinding;
  private volatile ArchiveWalBinding recoveredArchiveWalBinding;

  public SnapshotManager(String checkpointPath) {
  }

  @PostConstruct
  public void init() {
    checkpointVersion = CommonParameter.getInstance().getStorage().getCheckpointVersion();
    // prune checkpoint
    if (isV2Open()) {
      pruneCheckpointThread = ExecutorServiceManager.newSingleThreadScheduledExecutor(pruneName);
      pruneCheckpointThread.scheduleWithFixedDelay(() -> {
        try {
          if (!unChecked) {
            pruneCheckpoint();
          }
        } catch (Throwable t) {
          logger.error("Exception in prune checkpoint", t);
        }
      }, 10000, 3600, TimeUnit.MILLISECONDS);
    }
  }

  public static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
    return new String(value);
  }

  public ISession buildSession() {
    return buildSession(false);
  }

  public synchronized ISession buildSession(boolean forceEnable) {
    if (disabled && !forceEnable) {
      return new Session(this);
    }

    boolean disableOnExit = disabled && forceEnable;
    if (forceEnable) {
      disabled = false;
    }

    if (size > maxSize.get() && !hitDown) {
      flushCount = flushCount + (size - maxSize.get());
      updateSolidity(size - maxSize.get());
      size = maxSize.get();
      flush();
    }

    advance();
    ++activeSession;
    return new Session(this, disableOnExit);
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor) {
    dbs.forEach(db -> db.setCursor(cursor));
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor, long offset) {
    dbs.forEach(db -> db.setCursor(cursor, offset));
  }

  @Override
  public void add(IRevokingDB db) {
    Chainbase revokingDB = (Chainbase) db;
    dbs.add(revokingDB);
    flushServices.put(revokingDB.getDbName(),
        MoreExecutors.listeningDecorator(ExecutorServiceManager.newSingleThreadExecutor(
            "flush-service-" + revokingDB.getDbName())));
  }

  private void advance() {
    dbs.forEach(db -> db.setHead(db.getHead().advance()));
    ++size;
  }

  private void retreat() {
    dbs.forEach(db -> db.setHead(db.getHead().retreat()));
    --size;
  }

  public synchronized void merge() {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException(activeSession);
    }

    if (size < 2) {
      return;
    }

    dbs.forEach(db -> db.getHead().getPrevious().merge(db.getHead()));
    retreat();
    --activeSession;
  }

  public synchronized void revoke() {
    if (disabled) {
      return;
    }

    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException(activeSession);
    }

    if (size <= 0) {
      return;
    }

    disabled = true;

    try {
      retreat();
    } finally {
      disabled = false;
    }
    --activeSession;
  }

  public synchronized void commit() {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException(activeSession);
    }

    --activeSession;

    dbs.forEach(db -> {
      if (db.getHead().isOptimized()) {
        db.getHead().reloadToMem();
      }
    });
  }

  /**
   * Commits a block session and materializes its reverse diff using the configured collector.
   * Plain transaction/pending sessions must continue to use {@link #commit()} or merge/revoke.
   */
  public synchronized void commit(BlockSnapshotMeta meta) {
    Objects.requireNonNull(meta, "meta");
    long startedNanos = System.nanoTime();
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException(activeSession);
    }

    validateBlockMeta(meta);
    for (Chainbase db : dbs) {
      Snapshot head = db.getHead();
      if (!Snapshot.isImpl(head)) {
        throw new IllegalStateException(
            "Cannot bind block metadata to non-SnapshotImpl head: " + db.getDbName());
      }
    }

    BlockChangeView changeView = null;
    if (oldValueCollector != null || pathStateRuntimeAttachment != null) {
      changeView = BlockChangeView.capture(meta, dbs);
    }
    long frozenNanos = System.nanoTime();
    BlockReverseDiff reverseDiff = null;
    if (oldValueCollector != null) {
      reverseDiff = Objects.requireNonNull(
          oldValueCollector.collect(changeView),
          "archive collector returned null");
    }
    long archiveNanos = System.nanoTime();
    PathStateBlockTransition pathStateTransition = pathStateRuntimeAttachment == null ? null
        : pathStateRuntimeAttachment.capture(changeView);
    long pathNanos = System.nanoTime();
    PathStateSnapshotDelta pathStateDelta = pathStateTransition == null ? null
        : pathStateRuntimeAttachment.preparedSnapshotDelta(pathStateTransition);

    dbs.forEach(db -> {
      if (db.getHead().isOptimized()) {
        db.getHead().reloadToMem();
      }
    });

    for (Chainbase db : dbs) {
      boolean stateDatabase = ArchiveStoreScope.isStateDatabase(db.getDbName());
      ((SnapshotImpl) db.getHead()).attachBlockArtifacts(meta,
          stateDatabase ? reverseDiff : null, stateDatabase ? pathStateDelta : null);
    }
    long attachedNanos = System.nanoTime();
    --activeSession;
    if (pathStateRuntimeAttachment != null) {
      pathStateRuntimeAttachment.publish(pathStateTransition);
    }
    long completedNanos = System.nanoTime();
    if (changeView != null) {
      logger.info("Block-final artifact stages: head={}, freezeMs={}, archiveMs={}, "
              + "pathCaptureMs={}, attachMs={}, publishMs={}, totalMs={}",
          meta.getBlockNumber(), elapsedMillis(startedNanos, frozenNanos),
          elapsedMillis(frozenNanos, archiveNanos), elapsedMillis(archiveNanos, pathNanos),
          elapsedMillis(pathNanos, attachedNanos), elapsedMillis(attachedNanos, completedNanos),
          elapsedMillis(startedNanos, completedNanos));
    }
  }

  private static long elapsedMillis(long startedNanos, long completedNanos) {
    return TimeUnit.NANOSECONDS.toMillis(completedNanos - startedNanos);
  }

  /** Previews optional producer metadata from the active block session without publishing it. */
  public synchronized byte[] previewPathStateRoot(BlockSnapshotMeta meta) {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException(activeSession);
    }
    if (pathStateRuntimeAttachment == null) {
      return null;
    }
    return pathStateRuntimeAttachment.preview(BlockChangeView.capture(
        Objects.requireNonNull(meta, "meta"), dbs));
  }

  private void validateBlockMeta(BlockSnapshotMeta meta) {
    BlockSnapshotMeta previousMeta = null;
    for (Chainbase db : dbs) {
      if (!ArchiveStoreScope.isStateDatabase(db.getDbName())) {
        continue;
      }
      Snapshot head = db.getHead();
      if (!Snapshot.isImpl(head)) {
        continue;
      }
      Snapshot previous = head.getPrevious();
      BlockSnapshotMeta candidate = Snapshot.isImpl(previous)
          ? ((SnapshotImpl) previous).getBlockSnapshotMeta() : null;
      if (candidate != null && previousMeta != null && !previousMeta.equals(candidate)) {
        throw new IllegalStateException("Previous block metadata differs across state databases");
      }
      if (candidate != null) {
        previousMeta = candidate;
      }
    }
    if (previousMeta == null) {
      return;
    }
    if (meta.getEpoch() != previousMeta.getEpoch() + 1
        || meta.getBlockNumber() != previousMeta.getBlockNumber() + 1
        || !Arrays.equals(meta.getParentHash(), previousMeta.getBlockHash())) {
      throw new IllegalStateException(
          "Non-contiguous block snapshot metadata: previous=" + previousMeta + ", current=" + meta);
    }
  }

  /** Enables archive collection after all Chainbase stores have registered. */
  public synchronized void installArchiveCollector(OldValueCollector collector,
      BlockReverseDiffSink sink) {
    ArchiveStoreScope.validate(dbs);
    if (archiveRuntimeAttachment != null) {
      throw new IllegalStateException("Borrowed archive runtime is already attached");
    }
    if (commonCheckpointRuntimeAttachment != null) {
      throw new IllegalStateException("Common checkpoint runtime is already attached");
    }
    oldValueCollector = Objects.requireNonNull(collector, "collector");
    blockReverseDiffSink = Objects.requireNonNull(sink, "sink");
  }

  /** Installs Archive artifact capture without any legacy per-block or flush-time sink. */
  public synchronized void installCommonCheckpointArchiveCollector(OldValueCollector collector) {
    ArchiveStoreScope.validate(dbs);
    if (oldValueCollector != null || blockReverseDiffSink != null
        || archiveRuntimeAttachment != null || commonCheckpointRuntimeAttachment != null) {
      throw new IllegalStateException("Archive collaborators are already installed");
    }
    oldValueCollector = Objects.requireNonNull(collector, "collector");
  }

  /** Clears a partially installed common-checkpoint collector during startup rollback. */
  public synchronized void clearCommonCheckpointArchiveCollector() {
    if (commonCheckpointRuntimeAttachment != null || archiveRuntimeAttachment != null
        || blockReverseDiffSink != null) {
      throw new IllegalStateException("Cannot clear an active Archive persistence runtime");
    }
    oldValueCollector = null;
  }

  /** Atomically installs one borrowed archive runtime bundle after store registration. */
  public synchronized void attachArchiveRuntime(ArchiveRuntimeAttachment attachment) {
    ArchiveStoreScope.validate(dbs);
    ArchiveRuntimeAttachment candidate = Objects.requireNonNull(attachment, "attachment");
    if (archiveRuntimeAttachment != null) {
      throw new IllegalStateException("Archive runtime is already attached");
    }
    if (commonCheckpointRuntimeAttachment != null) {
      throw new IllegalStateException("Common checkpoint runtime is already attached");
    }
    if (oldValueCollector != null || blockReverseDiffSink != null) {
      throw new IllegalStateException("Legacy archive collaborators are already installed");
    }
    oldValueCollector = candidate.getCollector();
    blockReverseDiffSink = candidate.getSink();
    archiveRuntimeAttachment = candidate;
  }

  /** Detaches the exact borrowed bundle without closing resources owned by its runtime. */
  public synchronized ArchiveRuntimeAttachment detachArchiveRuntime(
      ArchiveRuntimeAttachment expected) {
    ArchiveRuntimeAttachment candidate = Objects.requireNonNull(expected, "expected");
    if (archiveRuntimeAttachment == null) {
      throw new IllegalStateException("Archive runtime is not attached");
    }
    if (archiveRuntimeAttachment != candidate) {
      throw new IllegalStateException("Cannot detach a foreign archive runtime");
    }
    archiveRuntimeAttachment = null;
    oldValueCollector = null;
    blockReverseDiffSink = null;
    submittedArchiveHistoryEpoch = null;
    archiveReadableEpoch = -1;
    return candidate;
  }

  /** Installs an independent non-consensus path-state block-final runtime. */
  public synchronized void attachPathStateRuntime(PathStateRuntimeAttachment attachment) {
    ArchiveStoreScope.validate(dbs);
    PathStateRuntimeAttachment candidate = Objects.requireNonNull(attachment, "attachment");
    if (pathStateRuntimeAttachment != null) {
      throw new IllegalStateException("Path-state runtime is already attached");
    }
    pathStateRuntimeAttachment = candidate;
  }

  /**
   * Installs the exclusive three-authority persistence owner. Archive collection and PathState
   * in-memory advancement remain attached separately, but their legacy durable callbacks are
   * never used while this attachment is present.
   */
  public synchronized void attachCommonCheckpointRuntime(
      CommonCheckpointRuntimeAttachment attachment) {
    ArchiveStoreScope.validate(dbs);
    CommonCheckpointRuntimeAttachment candidate = Objects.requireNonNull(attachment,
        "attachment");
    if (!candidate.isEnabled()) {
      throw new IllegalArgumentException("Common checkpoint runtime must be enabled");
    }
    if (commonCheckpointRuntimeAttachment != null) {
      throw new IllegalStateException("Common checkpoint runtime is already attached");
    }
    if (archiveRuntimeAttachment != null || blockReverseDiffSink != null) {
      throw new IllegalStateException(
          "Common checkpoint runtime is mutually exclusive with legacy Archive persistence");
    }
    if (oldValueCollector == null || pathStateRuntimeAttachment == null
        || !pathStateRuntimeAttachment.isCommonCheckpointOnly()) {
      throw new IllegalStateException(
          "Common checkpoint requires Archive capture and a checkpoint-only PathState runtime");
    }
    commonCheckpointRuntimeAttachment = candidate;
  }

  /** Detaches the exact Manager-owned common-checkpoint runtime without closing it. */
  public synchronized CommonCheckpointRuntimeAttachment detachCommonCheckpointRuntime(
      CommonCheckpointRuntimeAttachment expected) {
    CommonCheckpointRuntimeAttachment candidate = Objects.requireNonNull(expected, "expected");
    if (commonCheckpointRuntimeAttachment != candidate) {
      throw new IllegalStateException("Cannot detach a missing or foreign common runtime");
    }
    commonCheckpointRuntimeAttachment = null;
    oldValueCollector = null;
    return candidate;
  }

  /** Detaches the exact borrowed path-state runtime without closing its Manager-owned state. */
  public synchronized PathStateRuntimeAttachment detachPathStateRuntime(
      PathStateRuntimeAttachment expected) {
    PathStateRuntimeAttachment candidate = Objects.requireNonNull(expected, "expected");
    if (pathStateRuntimeAttachment == null) {
      throw new IllegalStateException("Path-state runtime is not attached");
    }
    if (pathStateRuntimeAttachment != candidate) {
      throw new IllegalStateException("Cannot detach a foreign path-state runtime");
    }
    pathStateRuntimeAttachment = null;
    return candidate;
  }

  /** Runs latest-state snapshot acquisition inside the canonical apply/flush monitor. */
  public synchronized void withArchiveStateBarrier(ArchiveStateAction action) throws IOException {
    Objects.requireNonNull(action, "action").run();
  }

  public void markArchiveReadableThrough(long epoch) {
    archiveReadableEpoch = epoch;
  }

  public synchronized void pop() {
    if (activeSession != 0) {
      throw new RevokingStoreIllegalStateException(
          String.format("activeSession has to be equal 0, current %d", activeSession));
    }

    if (size <= 0) {
      throw new RevokingStoreIllegalStateException(
          String.format("there is not snapshot to be popped, current: %d", size));
    }

    disabled = true;

    try {
      retreat();
    } finally {
      disabled = false;
    }
  }

  @Override
  public synchronized void fastPop() {
    if (activeSession != 0) {
      throw new RevokingStoreIllegalStateException(
          String.format("activeSession has to be equal 0, current %d", activeSession));
    }
    if (size <= 0) {
      throw new RevokingStoreIllegalStateException(
          String.format("there is not snapshot to be popped, current: %d", size));
    }
    if (submittedArchiveHistoryEpoch != null) {
      throw new IllegalStateException("Cannot pop while archive history flush is pending");
    }
    pop();
  }

  public synchronized void enable() {
    disabled = false;
  }

  @Override
  public int size() {
    return size;
  }

  public int getMaxSize() {
    return maxSize.get();
  }

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize.set(maxSize);
  }

  public synchronized void disable() {
    disabled = true;
  }

  @Override
  public void shutdown() {
    Closeable legacyArchiveSink = prepareArchiveShutdown();
    ExecutorServiceManager.shutdownAndAwaitTermination(pruneCheckpointThread, pruneName);
    flushServices.forEach((key, value) -> ExecutorServiceManager.shutdownAndAwaitTermination(value,
        "flush-service-" + key));
    if (legacyArchiveSink != null) {
      try {
        legacyArchiveSink.close();
      } catch (IOException e) {
        logger.error("Failed to close archive history sink.", e);
      }
    }
  }

  private synchronized Closeable prepareArchiveShutdown() {
    submittedArchiveHistoryEpoch = null;
    if (archiveRuntimeAttachment != null) {
      archiveRuntimeAttachment = null;
      oldValueCollector = null;
      blockReverseDiffSink = null;
      archiveReadableEpoch = -1;
      return null;
    }
    if (commonCheckpointRuntimeAttachment != null) {
      commonCheckpointRuntimeAttachment = null;
      oldValueCollector = null;
      blockReverseDiffSink = null;
      archiveReadableEpoch = -1;
      return null;
    }
    return blockReverseDiffSink instanceof Closeable ? (Closeable) blockReverseDiffSink : null;
  }

  public void updateSolidity(int hops) {
    for (int i = 0; i < hops; i++) {
      for (Chainbase db : dbs) {
        db.getHead().updateSolidity();
      }
    }
  }

  public boolean shouldBeRefreshed() {
    return flushCount >= maxFlushCount;
  }

  private void refresh() {
    refresh(flushCount);
  }

  private void refresh(int count) {
    List<ListenableFuture<?>> futures = new ArrayList<>(dbs.size());
    Chainbase properties = null;
    if (oldValueCollector != null) {
      properties = dbs.stream()
          .filter(db -> "properties".equals(db.getDbName()))
          .findFirst()
          .orElse(null);
      if (properties != null) {
        // Account root projection reads the durable optimization flag. Make that dependency
        // deterministic when archive mode projects account-asset changes at block boundaries.
        refreshOne(properties, count);
      }
    }
    for (Chainbase db : dbs) {
      if (db == properties) {
        continue;
      }
      futures.add(flushServices.get(db.getDbName()).submit(() -> refreshOne(db, count)));
    }
    Future<?> future = Futures.allAsList(futures);
    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TronDBException(e);
    } catch (ExecutionException e) {
      throw new TronDBException(e);
    }
  }

  private void refreshOne(Chainbase db, int count) {
    if (Snapshot.isRoot(db.getHead())) {
      return;
    }

    List<Snapshot> snapshots = new ArrayList<>();

    SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
    Snapshot next = root;
    for (int i = 0; i < count; ++i) {
      next = next.getNext();
      snapshots.add(next);
    }

    root.merge(snapshots);

    root.resetSolidity();
    if (db.getHead() == next) {
      db.setHead(root);
    } else {
      next.getNext().setPrevious(root);
      root.setNext(next.getNext());
    }
  }

  public void flush() {
    flush(false);
  }

  @Override
  public void flushPending() {
    flush(true);
  }

  private synchronized void flush(boolean force) {
    if (unChecked || (force && flushCount == 0)) {
      return;
    }

    if (force || shouldBeRefreshed()) {
      try {
        long start = System.currentTimeMillis();
        if (commonCheckpointRuntimeAttachment != null) {
          if (flushCount <= 0) {
            return;
          }
          try {
            commonCheckpointRuntimeAttachment.checkpointAndRebase(flushCount);
          } catch (IOException | RuntimeException failure) {
            throw new TronDBException("Common checkpoint publication failed", failure);
          }
          flushCount = 0;
          logger.info("Common checkpoint flush cost: {} ms.",
              System.currentTimeMillis() - start);
          return;
        }
        BlockSnapshotMeta pathStateFlushTarget = pathStateFlushTarget();
        ArchiveWalBinding archiveBinding = publishArchiveHistoryForFlush();
        if (!isV2Open()) {
          deleteCheckpoint();
        }
        createCheckpoint(archiveBinding);

        long checkPointEnd = System.currentTimeMillis();
        if (archiveBinding != null && archiveRuntimeAttachment != null) {
          try {
            archiveRuntimeAttachment.publishCommittedPrefix(archiveBinding.getLast());
          } catch (IOException | RuntimeException failure) {
            throw new TronDBException("Archive committed-prefix publication failed", failure);
          }
        }
        refresh();
        if (pathStateRuntimeAttachment != null && pathStateFlushTarget != null) {
          pathStateRuntimeAttachment.flushBaseThrough(pathStateFlushTarget.getBlockNumber(),
              pathStateFlushTarget.getBlockHash());
        }
        if (archiveBinding != null && archiveRuntimeAttachment != null) {
          try {
            archiveRuntimeAttachment.publishReadableState(archiveBinding.getLast());
          } catch (IOException | RuntimeException failure) {
            throw new TronDBException("Archive readable-state publication failed", failure);
          }
        }
        if (archiveBinding != null) {
          ((DurableBlockReverseDiffSink) blockReverseDiffSink)
              .releaseThrough(archiveBinding.getLast().getEpoch());
          submittedArchiveHistoryEpoch = null;
        }
        flushCount = 0;
        logger.info("Flush cost: {} ms, create checkpoint cost: {} ms, refresh cost: {} ms.",
            System.currentTimeMillis() - start,
            checkPointEnd - start,
            System.currentTimeMillis() - checkPointEnd
        );
      } catch (TronDBException e) {
        logger.error(" Find fatal error, program will be exited soon.", e);
        hitDown = true;
        throw new TronError(e, TronError.ErrCode.DB_FLUSH);
      }
    }
  }

  private BlockSnapshotMeta pathStateFlushTarget() {
    if (pathStateRuntimeAttachment == null || flushCount == 0 || dbs.isEmpty()) {
      return null;
    }
    try {
      Snapshot next = dbs.get(0).getHead().getRoot();
      BlockSnapshotMeta target = null;
      for (int index = 0; index < flushCount; index++) {
        next = next.getNext();
        if (!Snapshot.isImpl(next)) {
          throw new IllegalStateException("Path-state flush range is missing a snapshot layer");
        }
        target = ((SnapshotImpl) next).getBlockSnapshotMeta();
        if (target == null) {
          throw new IllegalStateException("Path-state flush layer has no block metadata");
        }
      }
      return target;
    } catch (RuntimeException failure) {
      pathStateRuntimeAttachment.fail(failure);
      return null;
    }
  }

  private ArchiveWalBinding publishArchiveHistoryForFlush() {
    if (oldValueCollector == null) {
      return null;
    }
    if (!(blockReverseDiffSink instanceof DurableBlockReverseDiffSink)) {
      throw new TronDBException("Archive sink cannot prove durable history before checkpoint");
    }
    Chainbase stateDatabase = dbs.stream()
        .filter(db -> ArchiveStoreScope.isStateDatabase(db.getDbName()))
        .findFirst()
        .orElseThrow(() -> new TronDBException("Archive mode has no registered state database"));
    Snapshot next = stateDatabase.getHead().getRoot();
    List<BlockReverseDiff> prepared = new ArrayList<>(flushCount);
    BlockSnapshotMeta last = null;
    for (int i = 0; i < flushCount; i++) {
      next = next.getNext();
      if (!Snapshot.isImpl(next)) {
        throw new TronDBException("Archive flush range is missing a snapshot layer");
      }
      BlockSnapshotMeta meta = ((SnapshotImpl) next).getBlockSnapshotMeta();
      if (meta == null) {
        throw new TronDBException("Archive flush range contains a layer without block metadata");
      }
      if (last != null && meta.getEpoch() != last.getEpoch() + 1) {
        throw new TronDBException("Archive flush range is not epoch-contiguous");
      }
      BlockReverseDiff reverseDiff = ((SnapshotImpl) next).getPreparedArchiveBlock();
      if (reverseDiff == null || !meta.equals(reverseDiff.getMeta())) {
        throw new TronDBException(
            "Archive flush range contains a layer without its matching prepared payload");
      }
      prepared.add(reverseDiff);
      last = meta;
    }
    if (last == null) {
      return null;
    }
    try {
      DurableBlockReverseDiffSink durableSink =
          (DurableBlockReverseDiffSink) blockReverseDiffSink;
      if (submittedArchiveHistoryEpoch == null) {
        durableSink.acceptAll(prepared);
        submittedArchiveHistoryEpoch = last.getEpoch();
      } else if (submittedArchiveHistoryEpoch.longValue() != last.getEpoch()) {
        throw new ArchivePersistenceException(
            "Submitted archive history target does not match the pending flush range");
      }
      durableSink.awaitCommitted(last.getEpoch());
      List<BlockSnapshotMeta> expectedMetas = prepared.stream()
          .map(BlockReverseDiff::getMeta).collect(Collectors.toList());
      DurableHistoryMarkerRangeEvidence evidence =
          durableSink.createMarkerRangeEvidence(prepared.size());
      return ArchiveWalBinding.fromMarkers(evidence.read(expectedMetas));
    } catch (RuntimeException e) {
      throw new TronDBException("Archive history durability gate failed", e);
    }
  }

  public void createCheckpoint() {
    createCheckpoint(null);
  }

  private void createCheckpoint(ArchiveWalBinding archiveBinding) {
    TronDatabase<byte[]> checkPointStore = null;
    try {
      Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
      for (Chainbase db : dbs) {
        Snapshot head = db.getHead();
        if (Snapshot.isRoot(head)) {
          return;
        }

        String dbName = db.getDbName();

        if (Objects.equals(dbName, "trans-cache")) {
          // trans-cache is deprecated
          continue;
        }

        Snapshot next = head.getRoot();
        for (int i = 0; i < flushCount; ++i) {
          next = next.getNext();
          SnapshotImpl snapshot = (SnapshotImpl) next;
          DB<Key, Value> keyValueDB = snapshot.getDb();
          for (Map.Entry<Key, Value> e : keyValueDB) {
            Key k = e.getKey();
            Value v = e.getValue();
            batch.put(WrappedByteArray.of(Bytes.concat(simpleEncode(dbName), k.getBytes())),
                WrappedByteArray.of(v.encode()));
          }
        }
      }
      if (archiveBinding != null) {
        batch.put(WrappedByteArray.of(ArchiveWalBinding.getCheckpointKey()),
            WrappedByteArray.of(new ArchiveWalBindingCodec().encode(archiveBinding)));
      }
      if (isV2Open()) {
        String dbName = String.valueOf(System.currentTimeMillis());
        checkPointStore = getCheckpointDB(dbName);
      } else {
        checkPointStore = checkTmpStore;
      }

      checkPointStore.updateByBatch(batch.entrySet().stream()
              .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
              .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll));
      latestArchiveWalBinding = archiveBinding;

    } catch (Exception e) {
      throw new TronDBException(e);
    } finally {
      if (isV2Open() && checkPointStore != null) {
        checkPointStore.close();
      }
    }
  }

  private TronDatabase<byte[]> getCheckpointDB(String dbName) {
    return new CheckPointV2Store(CHECKPOINT_V2_DIR+"/"+dbName);
  }

  public List<String> getCheckpointList() {
    String dbPath = Paths.get(StorageUtils.getOutputDirectoryByDbName(CHECKPOINT_V2_DIR),
        CommonParameter.getInstance().getStorage().getDbDirectory()).toString();
    File file = new File(Paths.get(dbPath, CHECKPOINT_V2_DIR).toString());
    if (file.exists() && file.isDirectory()) {
      String[] subDirs = file.list();
      if (subDirs != null) {
        return Arrays.stream(subDirs).sorted().collect(Collectors.toList());
      }
    }
    return null;
  }

  private void deleteCheckpoint() {
    if(checkTmpStore == null) {
      // only occurs in mock test. TODO fix test
      return;
    }
    try {
      Map<byte[], byte[]> hmap = new HashMap<>();
      for (Map.Entry<byte[], byte[]> e : checkTmpStore.getDbSource()) {
        hmap.put(e.getKey(), null);
      }
      if (hmap.size() != 0) {
        checkTmpStore.getDbSource().updateByBatch(hmap);
      }
    } catch (Exception e) {
      throw new TronDBException(e);
    }
  }

  private void pruneCheckpoint() {
    if (unChecked) {
      return;
    }
    List<String> cpList = getCheckpointList();
    if (cpList == null) {
      return;
    }
    if (cpList.size() < 3) {
      return;
    }
    long latestTimestamp = Long.parseLong(cpList.get(cpList.size()-1));
    for (String cp: cpList.subList(0, cpList.size()-3)) {
      long timestamp = Long.parseLong(cp);
      if (latestTimestamp - timestamp <= ONE_MINUTE_MILLS*2) {
        break;
      }
      String checkpointPath = Paths.get(StorageUtils.getOutputDirectoryByDbName(CHECKPOINT_V2_DIR),
          CommonParameter.getInstance().getStorage().getDbDirectory(), CHECKPOINT_V2_DIR).toString();
      if (!FileUtil.recursiveDelete(Paths.get(checkpointPath, cp).toString())) {
        logger.error("checkpoint prune failed, timestamp: {}", timestamp);
        return;
      }
      logger.debug("checkpoint prune success, timestamp: {}", timestamp);
    }
  }

  // ensure run this method first after process start.
  @Override
  public void check() {
    recoveredArchiveWalBinding = null;
    if (hasDurableCommonCheckpointAuthority()) {
      logger.info("Common checkpoint authority established, skip legacy checkpoint recovery");
      unChecked = false;
      return;
    }
    if (!isV2Open()) {
      List<String> cpList = getCheckpointList();
      if (cpList != null && cpList.size() != 0) {
        String msg = "checkpoint check failed, the checkpoint version of database not match your "
            + "config file, please set storage.checkpoint.version = 2 in your config file "
            + "and restart the node.";
        throw new TronError(msg, TronError.ErrCode.CHECKPOINT_VERSION);
      }
      checkV1();
    } else {
      checkV2();
    }
  }

  private boolean hasDurableCommonCheckpointAuthority() {
    org.tron.core.config.args.Storage storage =
        CommonParameter.getInstance().getStorage();
    if (!storage.isCommonCheckpointEnabled()) {
      return false;
    }
    Path directory = Paths.get(CommonParameter.getInstance().getOutputDirectory(),
        storage.getCommonCheckpointDirectory()).normalize();
    return hasDurableCommonCheckpointAuthority(true, directory);
  }

  static boolean hasDurableCommonCheckpointAuthority(boolean enabled, Path directory) {
    if (!enabled) {
      return false;
    }
    Path admitted = Objects.requireNonNull(directory, "directory");
    return Files.isRegularFile(admitted.resolve(ChainbaseCheckpointMaterializer.CURRENT_FILE),
        LinkOption.NOFOLLOW_LINKS)
        || Files.isRegularFile(admitted.resolve(CommonCheckpointFile.FILE_NAME),
        LinkOption.NOFOLLOW_LINKS);
  }

  private void checkV1() {
    for (Chainbase db: dbs) {
      if (!Snapshot.isRoot(db.getHead())) {
        throw new IllegalStateException("First check.");
      }
    }
    recover(checkTmpStore);
    logger.info("checkpoint v1 recover success");
    unChecked = false;
  }

  private void checkV2() {
    logger.info("checkpoint version: {}", CommonParameter.getInstance().getStorage().getCheckpointVersion());
    logger.info("checkpoint sync: {}", CommonParameter.getInstance().getStorage().isCheckpointSync());
    List<String> cpList = getCheckpointList();
    if (cpList == null || cpList.size() == 0) {
      logger.info("checkpoint size is 0, using v1 recover");
      checkV1();
      deleteCheckpoint();
      return;
    }

    long latestTimestamp = Long.parseLong(cpList.get(cpList.size()-1));
    for (String cp: cpList) {
      long timestamp = Long.parseLong(cp);
      if (latestTimestamp - timestamp > ONE_MINUTE_MILLS*2) {
        continue;
      }
      TronDatabase<byte[]> checkPointV2Store = getCheckpointDB(cp);
      try {
        recover(checkPointV2Store);
      } finally {
        checkPointV2Store.close();
      }
    }
    logger.info("checkpoint v2 recover success");
    unChecked = false;
  }

  private void recover(TronDatabase<byte[]> tronDatabase) {
    List<Map.Entry<byte[], byte[]>> entries = new ArrayList<>();
    ArchiveWalBinding recoveredBinding = null;
    for (Map.Entry<byte[], byte[]> entry : tronDatabase.getDbSource()) {
      byte[] key = Arrays.copyOf(entry.getKey(), entry.getKey().length);
      byte[] value = Arrays.copyOf(entry.getValue(), entry.getValue().length);
      if (ArchiveWalBinding.isCheckpointKey(key)) {
        if (recoveredBinding != null) {
          throw new ArchivePersistenceException(
              "Checkpoint contains duplicate Archive WAL bindings");
        }
        try {
          recoveredBinding = new ArchiveWalBindingCodec().decode(value);
        } catch (IllegalArgumentException invalid) {
          throw new ArchivePersistenceException(
              "Checkpoint Archive WAL binding is corrupt", invalid);
        }
      }
      entries.add(Maps.immutableEntry(key, value));
    }
    Map<String, Chainbase> dbMap = dbs.stream()
        .map(db -> Maps.immutableEntry(db.getDbName(), db))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    advance();
    for (Map.Entry<byte[], byte[]> e : entries) {
      byte[] key = e.getKey();
      byte[] value = e.getValue();
      if (ArchiveWalBinding.isCheckpointKey(key)) {
        continue;
      }
      String db = simpleDecode(key);
      if (dbMap.get(db) == null) {
        continue;
      }
      byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);
      byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
      if (realValue != null) {
        dbMap.get(db).getHead().put(realKey, realValue);
      } else {
        byte op = value[0];
        if (Value.Operator.DELETE.getValue() == op) {
          dbMap.get(db).getHead().remove(realKey);
        } else {
          dbMap.get(db).getHead().put(realKey, new byte[0]);
        }
      }
    }

    dbs.forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    retreat();
    if (recoveredBinding != null) {
      recoveredArchiveWalBinding = recoveredBinding;
    }
  }

  public ArchiveWalBinding getLatestArchiveWalBinding() {
    return latestArchiveWalBinding;
  }

  public ArchiveWalBinding getRecoveredArchiveWalBinding() {
    return recoveredArchiveWalBinding;
  }

  private boolean isV2Open() {
    return checkpointVersion == 2;
  }

  private byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  @Slf4j(topic = "DB")
  @Getter // only for unit test
  public static class Session implements ISession {

    private SnapshotManager snapshotManager;
    private boolean applySnapshot = true;
    private boolean disableOnExit = false;

    public Session(SnapshotManager snapshotManager) {
      this(snapshotManager, false);
    }

    public Session(SnapshotManager snapshotManager, boolean disableOnExit) {
      this.snapshotManager = snapshotManager;
      this.disableOnExit = disableOnExit;
    }

    @Override
    public void commit() {
      snapshotManager.commit();
      applySnapshot = false;
    }

    @Override
    public void commit(BlockSnapshotMeta meta) {
      snapshotManager.commit(meta);
      applySnapshot = false;
    }

    @Override
    public void revoke() {
      if (applySnapshot) {
        snapshotManager.revoke();
      }

      applySnapshot = false;
    }

    @Override
    public void merge() {
      if (applySnapshot) {
        snapshotManager.merge();
      }

      applySnapshot = false;
    }

    @Override
    public void destroy() {
      try {
        if (applySnapshot) {
          snapshotManager.revoke();
        }
      } catch (Exception e) {
        logger.error("Revoke database error.", e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }

    @Override
    public void close() {
      try {
        if (applySnapshot) {
          snapshotManager.revoke();
        }
      } catch (Exception e) {
        logger.error("Revoke database error.", e);
        throw new RevokingStoreIllegalStateException(e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }
  }

}
