package org.tron.core.db2.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import org.tron.core.db2.archive.StateArchiveCheckpointMaterializer;
import org.tron.core.db2.archive.StateArchiveCheckpointPlanner;
import org.tron.core.db2.archive.StateArchiveCheckpointReadSnapshot;
import org.tron.core.db2.archive.StateArchiveHotCheckpointMaterializer;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Isolated composition boundary for the next-format common-checkpoint runtime. */
public final class CommonCheckpointRuntime implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger("DB");

  private final CommonCheckpointRuntimeOwner owner;
  private final List<Chainbase> databases;
  private final Path archiveDirectory;
  private final byte[] formatIdentity;
  private final Engine engine;
  private final StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory;
  private final CommonCheckpointMemoryRebaser memoryRebaser;
  private final CommonCheckpointHotRecovery hotRecovery;
  private final StateArchiveCheckpointPlanner archivePlanner;
  private final CommonCheckpointMaterializedStore materializedStore;
  private final LongSupplier nanoTime;
  private final TimingSink timingSink;
  private final CommonCheckpointPayloadFactory payloadFactory = new CommonCheckpointPayloadFactory();
  private final CommonCheckpointSnapshotRebaser rebaser = new CommonCheckpointSnapshotRebaser();
  private CommonCheckpointTarget publishedTarget;

  public CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser) {
    this(owner, databases, archiveDirectory, formatIdentity, engine, latestFactory,
        memoryRebaser, null, null, null, System::nanoTime,
        CommonCheckpointRuntime::logTiming);
  }

  /** Constructs the default-off Hot DB v2 path selected by the dual-gated Manager branch. */
  public CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser,
      StateArchiveHotCheckpointMaterializer hotMaterializer,
      CommonCheckpointHotRecovery hotRecovery) {
    this(owner, databases, archiveDirectory, formatIdentity, engine, latestFactory,
        memoryRebaser, null, Objects.requireNonNull(hotMaterializer, "hotMaterializer"),
        Objects.requireNonNull(hotRecovery, "hotRecovery"), System::nanoTime,
        CommonCheckpointRuntime::logTiming);
  }

  /** Constructs the default-off append-file v3 participant without Hot DB recovery. */
  public CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser,
      StateArchiveCheckpointPlanner archivePlanner) {
    this(owner, databases, archiveDirectory, formatIdentity, engine, latestFactory,
        memoryRebaser, null, Objects.requireNonNull(archivePlanner, "archivePlanner"),
        null, System::nanoTime, CommonCheckpointRuntime::logTiming);
  }

  public CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser,
      CommonCheckpointMaterializedStore materializedStore) {
    this(owner, databases, archiveDirectory, formatIdentity, engine, latestFactory,
        memoryRebaser, materializedStore, null, null, System::nanoTime,
        CommonCheckpointRuntime::logTiming);
  }

  CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser, LongSupplier nanoTime,
      TimingSink timingSink) {
    this(owner, databases, archiveDirectory, formatIdentity, engine, latestFactory,
        memoryRebaser, null, null, null, nanoTime, timingSink);
  }

  CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser,
      StateArchiveHotCheckpointMaterializer hotMaterializer,
      CommonCheckpointHotRecovery hotRecovery, LongSupplier nanoTime, TimingSink timingSink) {
    this(owner, databases, archiveDirectory, formatIdentity, engine, latestFactory,
        memoryRebaser, null, hotMaterializer, hotRecovery, nanoTime, timingSink);
  }

  CommonCheckpointRuntime(CommonCheckpointRuntimeOwner owner, List<Chainbase> databases,
      Path archiveDirectory, byte[] formatIdentity, Engine engine,
      StateArchiveCheckpointReadSnapshot.PinnedLatestStateFactory latestFactory,
      CommonCheckpointMemoryRebaser memoryRebaser,
      CommonCheckpointMaterializedStore materializedStore,
      StateArchiveCheckpointPlanner archivePlanner,
      CommonCheckpointHotRecovery hotRecovery, LongSupplier nanoTime, TimingSink timingSink) {
    this.owner = Objects.requireNonNull(owner, "owner");
    this.databases = new ArrayList<>(Objects.requireNonNull(databases, "databases"));
    if (this.databases.isEmpty() || this.databases.contains(null)) {
      throw new IllegalArgumentException("common checkpoint runtime requires registered Stores");
    }
    this.archiveDirectory = Objects.requireNonNull(archiveDirectory, "archiveDirectory");
    this.formatIdentity = requireDigest(formatIdentity);
    this.engine = Objects.requireNonNull(engine, "engine");
    this.latestFactory = Objects.requireNonNull(latestFactory, "latestFactory");
    this.memoryRebaser = Objects.requireNonNull(memoryRebaser, "memoryRebaser");
    this.materializedStore = materializedStore;
    this.hotRecovery = hotRecovery;
    this.archivePlanner = archivePlanner;
    if (archivePlanner instanceof StateArchiveHotCheckpointMaterializer
        != (hotRecovery != null)) {
      throw new IllegalArgumentException(
          "Hot common checkpoint runtime requires materializer and recovery together");
    }
    if (archivePlanner != null) {
      owner.requireMaterializer(archivePlanner);
    }
    this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    this.timingSink = Objects.requireNonNull(timingSink, "timingSink");
  }

  /** Completes durable redo before this runtime admits checkpoint reads or new flushes. */
  public synchronized CommonCheckpointRedoCoordinator.RecoveryAction recoverBeforeServing()
      throws IOException {
    try {
      if (hotRecovery != null) {
        hotRecovery.reconcileBeforeCommonRedo();
      }
      CommonCheckpointRedoCoordinator.RecoveryAction action = owner.recoverBeforeServing();
      publishedTarget = archivePlanner == null
          ? StateArchiveCheckpointMaterializer.loadPublishedTargetIfPresent(
              archiveDirectory, formatIdentity, engine, materializedStore).orElse(null) : null;
      if (publishedTarget != null) {
        owner.requirePublishedBeforeServing(publishedTarget);
      }
      return action;
    } catch (IOException | RuntimeException failure) {
      owner.fail(failure);
      throw failure;
    }
  }

  /**
   * Captures and applies the immutable Snapshot prefix, then rebases it without a second Store
   * write. The caller must hold the SnapshotManager monitor for the whole call.
   */
  public synchronized CommonCheckpointTarget checkpointAndRebase(int flushCount)
      throws IOException {
    try {
      long totalStart = nanoTime.getAsLong();
      Timing timing = new Timing(flushCount);
      long captureStart = nanoTime.getAsLong();
      CommonCheckpointCapture capture = archivePlanner == null ? null
          : payloadFactory.captureV2(formatIdentity, databases, flushCount, archivePlanner);
      CommonCheckpointPayload payload = capture == null
          ? payloadFactory.capture(formatIdentity, databases, flushCount) : capture.getPayload();
      timing.payloadCaptureUs = elapsedUs(captureStart);
      CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
      timing.head = target.getLastBlock().getBlockNumber();
      if (capture != null) {
        CommonCheckpointHotRecovery.requireWalDynamicIdentity(payload, target.getLastBlock());
        long hotPrepareStart = nanoTime.getAsLong();
        CommonCheckpointTarget prepared = archivePlanner.prepare(capture);
        timing.hotPrepareUs = elapsedUs(hotPrepareStart);
        if (!target.equals(prepared)) {
          throw new IOException("Hot Archive prepared checkpoint target differs");
        }
      }
      long ownerApplyStart = nanoTime.getAsLong();
      owner.apply(payload, () -> {
        long chainbasePrepareStart = nanoTime.getAsLong();
        CommonCheckpointSnapshotRebaser.Plan chainbasePlan =
            rebaser.prepare(databases, target, flushCount);
        timing.chainbaseRebasePrepareUs = elapsedUs(chainbasePrepareStart);
        long pathStatePrepareStart = nanoTime.getAsLong();
        CommonCheckpointMemoryRebaser.RebasePlan pathStatePlan = memoryRebaser.prepare(target);
        timing.pathStateRebasePrepareUs = elapsedUs(pathStatePrepareStart);
        long chainbaseApplyStart = nanoTime.getAsLong();
        chainbasePlan.apply();
        timing.chainbaseRebaseApplyUs = elapsedUs(chainbaseApplyStart);
        long pathStateApplyStart = nanoTime.getAsLong();
        pathStatePlan.apply();
        timing.pathStateRebaseApplyUs = elapsedUs(pathStateApplyStart);
      });
      timing.ownerApplyUs = elapsedUs(ownerApplyStart);
      publishedTarget = target;
      timing.totalUs = elapsedUs(totalStart);
      emitTiming(timing);
      return target;
    } catch (IOException | RuntimeException failure) {
      owner.fail(failure);
      throw failure;
    }
  }

  /** Pins one point-only historical request under the same publication gate. */
  public synchronized StateArchiveCheckpointReadSnapshot pinPoint(long targetBlock)
      throws IOException {
    if (archivePlanner != null) {
      throw new IOException("Hot Archive runtime point reads are not integrated");
    }
    CommonCheckpointTarget target = publishedTarget;
    if (target == null) {
      throw new IOException("State Archive has no published common-checkpoint target");
    }
    return StateArchiveCheckpointReadSnapshot.pin(targetBlock, owner, archiveDirectory,
        target, engine, latestFactory);
  }

  public CommonCheckpointRuntimeOwner.State getState() {
    return owner.getState();
  }

  @Override
  public void close() {
    owner.close();
  }

  private static byte[] requireDigest(byte[] value) {
    byte[] admitted = Arrays.copyOf(Objects.requireNonNull(value, "formatIdentity"),
        value.length);
    if (admitted.length != 32) {
      throw new IllegalArgumentException("formatIdentity must contain exactly 32 bytes");
    }
    return admitted;
  }

  private long elapsedUs(long start) {
    return Math.max(0L, (nanoTime.getAsLong() - start) / 1_000L);
  }

  private void emitTiming(Timing timing) {
    try {
      timingSink.accept(timing);
    } catch (RuntimeException ignored) {
      // Diagnostics must not turn an already completed checkpoint into a caller-visible failure.
    }
  }

  private static void logTiming(Timing timing) {
    logger.info("Common checkpoint runtime stages: head={}, blocks={}, payloadCaptureUs={}, "
            + "hotPrepareUs={}, ownerApplyUs={}, chainbaseRebasePrepareUs={}, "
            + "pathStateRebasePrepareUs={}, "
            + "chainbaseRebaseApplyUs={}, pathStateRebaseApplyUs={}, totalUs={}",
        timing.head, timing.blocks, timing.payloadCaptureUs, timing.hotPrepareUs,
        timing.ownerApplyUs,
        timing.chainbaseRebasePrepareUs, timing.pathStateRebasePrepareUs,
        timing.chainbaseRebaseApplyUs, timing.pathStateRebaseApplyUs, timing.totalUs);
  }

  static final class Timing {

    private long head;
    private final int blocks;
    private long payloadCaptureUs;
    private long hotPrepareUs;
    private long ownerApplyUs;
    private long chainbaseRebasePrepareUs;
    private long pathStateRebasePrepareUs;
    private long chainbaseRebaseApplyUs;
    private long pathStateRebaseApplyUs;
    private long totalUs;

    private Timing(int blocks) {
      this.blocks = blocks;
    }

    long getHead() {
      return head;
    }

    int getBlocks() {
      return blocks;
    }

    long getPayloadCaptureUs() {
      return payloadCaptureUs;
    }

    long getHotPrepareUs() {
      return hotPrepareUs;
    }

    long getOwnerApplyUs() {
      return ownerApplyUs;
    }

    long getChainbaseRebasePrepareUs() {
      return chainbaseRebasePrepareUs;
    }

    long getPathStateRebasePrepareUs() {
      return pathStateRebasePrepareUs;
    }

    long getChainbaseRebaseApplyUs() {
      return chainbaseRebaseApplyUs;
    }

    long getPathStateRebaseApplyUs() {
      return pathStateRebaseApplyUs;
    }

    long getTotalUs() {
      return totalUs;
    }
  }

  @FunctionalInterface
  interface TimingSink {
    void accept(Timing timing);
  }
}
