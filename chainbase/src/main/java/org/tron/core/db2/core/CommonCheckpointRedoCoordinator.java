package org.tron.core.db2.core;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Two-barrier, idempotent redo coordinator for one durable common checkpoint. */
public final class CommonCheckpointRedoCoordinator implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger("DB");
  private static final Authority[] ORDER = {
      Authority.CHAINBASE, Authority.PATH_STATE, Authority.STATE_ARCHIVE};

  private final CommonCheckpointFile checkpointFile;
  private final Map<Authority, CommonCheckpointMaterializer> materializers;
  private final FaultHook faultHook;
  private final LongSupplier nanoTime;
  private final TimingSink timingSink;
  private boolean closed;

  public CommonCheckpointRedoCoordinator(CommonCheckpointFile checkpointFile,
      CommonCheckpointMaterializer chainbase, CommonCheckpointMaterializer pathState,
      CommonCheckpointMaterializer stateArchive) {
    this(checkpointFile, chainbase, pathState, stateArchive, stage -> { });
  }

  CommonCheckpointRedoCoordinator(CommonCheckpointFile checkpointFile,
      CommonCheckpointMaterializer chainbase, CommonCheckpointMaterializer pathState,
      CommonCheckpointMaterializer stateArchive, FaultHook faultHook) {
    this(checkpointFile, chainbase, pathState, stateArchive, faultHook, System::nanoTime,
        CommonCheckpointRedoCoordinator::logTiming);
  }

  CommonCheckpointRedoCoordinator(CommonCheckpointFile checkpointFile,
      CommonCheckpointMaterializer chainbase, CommonCheckpointMaterializer pathState,
      CommonCheckpointMaterializer stateArchive, FaultHook faultHook, LongSupplier nanoTime,
      TimingSink timingSink) {
    this.checkpointFile = Objects.requireNonNull(checkpointFile, "checkpointFile");
    this.materializers = new EnumMap<>(Authority.class);
    admit(Authority.CHAINBASE, chainbase);
    admit(Authority.PATH_STATE, pathState);
    admit(Authority.STATE_ARCHIVE, stateArchive);
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
    this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    this.timingSink = Objects.requireNonNull(timingSink, "timingSink");
  }

  /** Durably publishes the redo payload before applying it to any authority. */
  public synchronized RecoveryAction apply(CommonCheckpointPayload payload) throws IOException {
    requireOpen();
    CommonCheckpointPayload admitted = Objects.requireNonNull(payload, "payload");
    Timing timing = new Timing("apply", CommonCheckpointTarget.from(admitted),
        admitted.getBlocks().size());
    long totalStart = nanoTime.getAsLong();
    timing.walPublishUs = timed(() -> checkpointFile.publish(admitted));
    Holder<CommonCheckpointPayload> loaded = new Holder<>();
    timing.walLoadUs = timed(() -> loaded.value = checkpointFile.loadRequired());
    RecoveryAction action = redo(loaded.value, timing);
    timing.totalUs = elapsedUs(totalStart);
    emitTiming(timing);
    return action;
  }

  /** Resumes the only durable checkpoint, or performs no work when none exists. */
  public synchronized RecoveryAction recover() throws IOException {
    requireOpen();
    long totalStart = nanoTime.getAsLong();
    Holder<CommonCheckpointPayload> loaded = new Holder<>();
    long loadUs = timed(() -> loaded.value = checkpointFile.loadIfPresent());
    if (loaded.value == null) {
      return RecoveryAction.NO_CHECKPOINT;
    }
    Timing timing = new Timing("recover", CommonCheckpointTarget.from(loaded.value),
        loaded.value.getBlocks().size());
    timing.walLoadUs = loadUs;
    RecoveryAction action = redo(loaded.value, timing);
    timing.totalUs = elapsedUs(totalStart);
    emitTiming(timing);
    return action;
  }

  synchronized void requireMaterializer(Authority authority,
      CommonCheckpointMaterializer expected) {
    if (materializers.get(Objects.requireNonNull(authority, "authority"))
        != Objects.requireNonNull(expected, "expected")) {
      throw new IllegalArgumentException(
          "common checkpoint runtime materializer identity differs: " + authority);
    }
  }

  /** Closes runtime-owned authority resources in reverse publication order. */
  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    Throwable failure = null;
    for (int index = ORDER.length - 1; index >= 0; index--) {
      CommonCheckpointMaterializer materializer = materializers.get(ORDER[index]);
      try {
        materializer.close();
      } catch (IOException | RuntimeException closing) {
        if (failure == null) {
          failure = closing;
        } else {
          failure.addSuppressed(closing);
        }
      }
    }
    if (failure instanceof IOException) {
      throw (IOException) failure;
    }
    if (failure != null) {
      throw (RuntimeException) failure;
    }
  }

  private RecoveryAction redo(CommonCheckpointPayload payload, Timing timing) throws IOException {
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    try (CheckpointScope ignored = new CheckpointScope(target, timing)) {
      return redo(payload, target, timing);
    }
  }

  private RecoveryAction redo(CommonCheckpointPayload payload, CommonCheckpointTarget target,
      Timing timing) throws IOException {
    Map<Authority, Status> initial = inspectAll(target, timing);
    if (initial.containsValue(Status.PUBLISHED)
        && initial.containsValue(Status.NEEDS_MATERIALIZATION)) {
      throw new IOException("common checkpoint has published authority before materialization "
          + "barrier");
    }

    for (Authority authority : ORDER) {
      CommonCheckpointMaterializer materializer = materializers.get(authority);
      if (initial.get(authority) == Status.NEEDS_MATERIALIZATION) {
        timing.materializeUs[authority.ordinal()] += timed(
            () -> materializer.materialize(payload, target));
        timing.materializeCount[authority.ordinal()]++;
        requireStatus(authority, Status.MATERIALIZED, inspect(authority, target, timing),
            "materialization");
        faultHook.after(materializeStage(authority));
      }
    }

    Map<Authority, Status> materialized = inspectAll(target, timing);
    if (materialized.containsValue(Status.NEEDS_MATERIALIZATION)) {
      throw new IOException("common checkpoint materialization barrier is incomplete");
    }
    for (Authority authority : ORDER) {
      CommonCheckpointMaterializer materializer = materializers.get(authority);
      if (materialized.get(authority) == Status.MATERIALIZED) {
        timing.publishUs[authority.ordinal()] += timed(() -> materializer.publish(target));
        timing.publishCount[authority.ordinal()]++;
        requireStatus(authority, Status.PUBLISHED, inspect(authority, target, timing),
            "publication");
        faultHook.after(publishStage(authority));
      }
    }

    Map<Authority, Status> published = inspectAll(target, timing);
    for (Authority authority : ORDER) {
      requireStatus(authority, Status.PUBLISHED, published.get(authority), "retirement");
    }
    faultHook.after(Stage.BEFORE_CHECKPOINT_RETIRE);
    timing.walRetireUs = timed(checkpointFile::retire);
    faultHook.after(Stage.AFTER_CHECKPOINT_RETIRE);
    return RecoveryAction.COMPLETED_REDO;
  }

  private final class CheckpointScope implements AutoCloseable {

    private final CommonCheckpointTarget target;
    private final Timing timing;
    private int opened;

    private CheckpointScope(CommonCheckpointTarget target, Timing timing) throws IOException {
      this.target = target;
      this.timing = timing;
      try {
        for (Authority authority : ORDER) {
          timing.beginUs[authority.ordinal()] += timed(
              () -> materializers.get(authority).beginCheckpoint(target));
          opened++;
        }
      } catch (IOException | RuntimeException failure) {
        try {
          close();
        } catch (IOException closing) {
          failure.addSuppressed(closing);
        }
        throw failure;
      }
    }

    @Override
    public void close() throws IOException {
      IOException failure = null;
      while (opened > 0) {
        Authority authority = ORDER[--opened];
        CommonCheckpointMaterializer materializer = materializers.get(authority);
        try {
          timing.endUs[authority.ordinal()] += timed(() -> materializer.endCheckpoint(target));
        } catch (IOException closing) {
          if (failure == null) {
            failure = closing;
          } else {
            failure.addSuppressed(closing);
          }
        }
      }
      if (failure != null) {
        throw failure;
      }
    }
  }

  private Map<Authority, Status> inspectAll(CommonCheckpointTarget target, Timing timing)
      throws IOException {
    Map<Authority, Status> statuses = new EnumMap<>(Authority.class);
    for (Authority authority : ORDER) {
      Status status = inspect(authority, target, timing);
      if (status == null) {
        throw new IOException("common checkpoint " + authority + " returned null status");
      }
      statuses.put(authority, status);
    }
    return statuses;
  }

  private Status inspect(Authority authority, CommonCheckpointTarget target, Timing timing)
      throws IOException {
    Holder<Status> status = new Holder<>();
    timing.inspectUs[authority.ordinal()] += timed(
        () -> status.value = materializers.get(authority).inspect(target));
    timing.inspectCount[authority.ordinal()]++;
    return status.value;
  }

  private long timed(IoAction action) throws IOException {
    long start = nanoTime.getAsLong();
    action.run();
    return elapsedUs(start);
  }

  private long elapsedUs(long start) {
    return Math.max(0L, (nanoTime.getAsLong() - start) / 1_000L);
  }

  private void emitTiming(Timing timing) {
    try {
      timingSink.accept(timing);
    } catch (RuntimeException ignored) {
      // Diagnostics must not turn an already durable checkpoint into a caller-visible failure.
    }
  }

  private void requireOpen() throws IOException {
    if (closed) {
      throw new IOException("common checkpoint coordinator is closed");
    }
  }

  private static void logTiming(Timing timing) {
    logger.info("Common checkpoint redo stages: mode={}, head={}, blocks={}, walPublishUs={}, "
            + "walLoadUs={}, chainbaseBeginUs={}, pathStateBeginUs={}, archiveBeginUs={}, "
            + "chainbaseInspectUs={}/{}, pathStateInspectUs={}/{}, archiveInspectUs={}/{}, "
            + "chainbaseMaterializeUs={}/{}, pathStateMaterializeUs={}/{}, "
            + "archiveMaterializeUs={}/{}, chainbasePublishUs={}/{}, "
            + "pathStatePublishUs={}/{}, archivePublishUs={}/{}, walRetireUs={}, "
            + "chainbaseEndUs={}, pathStateEndUs={}, archiveEndUs={}, totalUs={}",
        timing.mode, timing.head, timing.blocks, timing.walPublishUs, timing.walLoadUs,
        timing.begin(Authority.CHAINBASE), timing.begin(Authority.PATH_STATE),
        timing.begin(Authority.STATE_ARCHIVE), timing.inspect(Authority.CHAINBASE),
        timing.inspectCount(Authority.CHAINBASE), timing.inspect(Authority.PATH_STATE),
        timing.inspectCount(Authority.PATH_STATE), timing.inspect(Authority.STATE_ARCHIVE),
        timing.inspectCount(Authority.STATE_ARCHIVE), timing.materialize(Authority.CHAINBASE),
        timing.materializeCount(Authority.CHAINBASE), timing.materialize(Authority.PATH_STATE),
        timing.materializeCount(Authority.PATH_STATE), timing.materialize(Authority.STATE_ARCHIVE),
        timing.materializeCount(Authority.STATE_ARCHIVE), timing.publish(Authority.CHAINBASE),
        timing.publishCount(Authority.CHAINBASE), timing.publish(Authority.PATH_STATE),
        timing.publishCount(Authority.PATH_STATE), timing.publish(Authority.STATE_ARCHIVE),
        timing.publishCount(Authority.STATE_ARCHIVE), timing.walRetireUs,
        timing.end(Authority.CHAINBASE), timing.end(Authority.PATH_STATE),
        timing.end(Authority.STATE_ARCHIVE), timing.totalUs);
  }

  private void admit(Authority expected, CommonCheckpointMaterializer materializer) {
    CommonCheckpointMaterializer admitted = Objects.requireNonNull(materializer,
        expected + " materializer");
    if (admitted.authority() != expected || materializers.put(expected, admitted) != null) {
      throw new IllegalArgumentException("common checkpoint materializer authority differs: "
          + expected);
    }
  }

  private static void requireStatus(Authority authority, Status expected, Status actual,
      String operation) throws IOException {
    if (actual != expected) {
      throw new IOException("common checkpoint " + authority + " " + operation
          + " returned " + actual + " instead of " + expected);
    }
  }

  static final class Timing {

    private final String mode;
    private final long head;
    private final int blocks;
    private final long[] beginUs = new long[Authority.values().length];
    private final long[] inspectUs = new long[Authority.values().length];
    private final int[] inspectCount = new int[Authority.values().length];
    private final long[] materializeUs = new long[Authority.values().length];
    private final int[] materializeCount = new int[Authority.values().length];
    private final long[] publishUs = new long[Authority.values().length];
    private final int[] publishCount = new int[Authority.values().length];
    private final long[] endUs = new long[Authority.values().length];
    private long walPublishUs;
    private long walLoadUs;
    private long walRetireUs;
    private long totalUs;

    private Timing(String mode, CommonCheckpointTarget target, int blocks) {
      this.mode = mode;
      this.head = target.getLastBlock().getBlockNumber();
      this.blocks = blocks;
    }

    String getMode() {
      return mode;
    }

    long getHead() {
      return head;
    }

    int getBlocks() {
      return blocks;
    }

    long getWalPublishUs() {
      return walPublishUs;
    }

    long getWalLoadUs() {
      return walLoadUs;
    }

    long getWalRetireUs() {
      return walRetireUs;
    }

    long getTotalUs() {
      return totalUs;
    }

    long begin(Authority authority) {
      return beginUs[authority.ordinal()];
    }

    long inspect(Authority authority) {
      return inspectUs[authority.ordinal()];
    }

    int inspectCount(Authority authority) {
      return inspectCount[authority.ordinal()];
    }

    long materialize(Authority authority) {
      return materializeUs[authority.ordinal()];
    }

    int materializeCount(Authority authority) {
      return materializeCount[authority.ordinal()];
    }

    long publish(Authority authority) {
      return publishUs[authority.ordinal()];
    }

    int publishCount(Authority authority) {
      return publishCount[authority.ordinal()];
    }

    long end(Authority authority) {
      return endUs[authority.ordinal()];
    }
  }

  @FunctionalInterface
  interface TimingSink {
    void accept(Timing timing);
  }

  @FunctionalInterface
  private interface IoAction {
    void run() throws IOException;
  }

  private static final class Holder<T> {
    private T value;
  }

  private static Stage materializeStage(Authority authority) {
    switch (authority) {
      case CHAINBASE:
        return Stage.AFTER_CHAINBASE_MATERIALIZE;
      case PATH_STATE:
        return Stage.AFTER_PATH_STATE_MATERIALIZE;
      case STATE_ARCHIVE:
        return Stage.AFTER_ARCHIVE_MATERIALIZE;
      default:
        throw new IllegalArgumentException("unsupported checkpoint authority " + authority);
    }
  }

  private static Stage publishStage(Authority authority) {
    switch (authority) {
      case CHAINBASE:
        return Stage.AFTER_CHAINBASE_PUBLISH;
      case PATH_STATE:
        return Stage.AFTER_PATH_STATE_PUBLISH;
      case STATE_ARCHIVE:
        return Stage.AFTER_ARCHIVE_PUBLISH;
      default:
        throw new IllegalArgumentException("unsupported checkpoint authority " + authority);
    }
  }

  public enum RecoveryAction {
    NO_CHECKPOINT,
    COMPLETED_REDO
  }

  enum Stage {
    AFTER_CHAINBASE_MATERIALIZE,
    AFTER_PATH_STATE_MATERIALIZE,
    AFTER_ARCHIVE_MATERIALIZE,
    AFTER_CHAINBASE_PUBLISH,
    AFTER_PATH_STATE_PUBLISH,
    AFTER_ARCHIVE_PUBLISH,
    BEFORE_CHECKPOINT_RETIRE,
    AFTER_CHECKPOINT_RETIRE
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage) throws IOException;
  }
}
