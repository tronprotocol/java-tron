package org.tron.core.db2.stateroot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.db2.archive.BlockChangeView;
import org.tron.core.db2.archive.BlockSnapshotMeta;

/** Independent non-consensus runtime installed at the metadata-aware block commit boundary. */
@Slf4j(topic = "DB")
public final class PathStateRuntimeAttachment {

  private final PathStateTransitionCollector collector;
  private final TransitionSink sink;
  private final BaseFlushSink baseFlushSink;
  private final TransitionPreviewer previewer;
  private final SnapshotDeltaPreparer snapshotDeltaPreparer;
  private final boolean deferredCapture;
  private final boolean commonCheckpointOnly;
  private final BlockingQueue<BlockChangeView> deferredQueue;
  private final Thread deferredWorker;
  private Throwable failure;
  private FailureStage failureStage;
  private long readyBlockNumber = -1;
  private byte[] readyBlockHash;
  private long observedBlockNumber = -1;
  private byte[] observedBlockHash;
  private PathStateBlockTransition pending;
  private PathStateSnapshotDelta pendingSnapshotDelta;
  private BlockChangeView pendingView;
  private volatile boolean closed;
  private HeaderDiagnostic headerDiagnostic = HeaderDiagnostic.NONE;
  private long headerDiagnosticBlockNumber = -1;
  private byte[] headerDiagnosticBlockHash;

  public PathStateRuntimeAttachment(PathStateTransitionCollector collector, TransitionSink sink) {
    this(collector, sink, (blockNumber, blockHash) -> { }, null);
  }

  public PathStateRuntimeAttachment(PathStateTransitionCollector collector, TransitionSink sink,
      BaseFlushSink baseFlushSink) {
    this(collector, sink, baseFlushSink, null);
  }

  public PathStateRuntimeAttachment(PathStateTransitionCollector collector, TransitionSink sink,
      BaseFlushSink baseFlushSink, TransitionPreviewer previewer) {
    this(collector, sink, baseFlushSink, previewer, null);
  }

  public PathStateRuntimeAttachment(PathStateTransitionCollector collector, TransitionSink sink,
      BaseFlushSink baseFlushSink, TransitionPreviewer previewer,
      SnapshotDeltaPreparer snapshotDeltaPreparer) {
    this(collector, sink, baseFlushSink, previewer, snapshotDeltaPreparer, false);
  }

  /** Creates a benchmark runtime that defers collect, delta preparation, and head advance. */
  public static PathStateRuntimeAttachment deferred(PathStateTransitionCollector collector,
      TransitionSink sink, BaseFlushSink baseFlushSink, TransitionPreviewer previewer,
      SnapshotDeltaPreparer snapshotDeltaPreparer) {
    return new PathStateRuntimeAttachment(collector, sink, baseFlushSink, previewer,
        snapshotDeltaPreparer, true, false);
  }

  /** Creates the production in-memory head used exclusively by the common checkpoint owner. */
  public static PathStateRuntimeAttachment commonCheckpoint(PathStateTransitionCollector collector,
      TransitionSink sink, TransitionPreviewer previewer,
      SnapshotDeltaPreparer snapshotDeltaPreparer) {
    return new PathStateRuntimeAttachment(collector, sink, (blockNumber, blockHash) -> { },
        previewer, snapshotDeltaPreparer, false, true);
  }

  private PathStateRuntimeAttachment(PathStateTransitionCollector collector, TransitionSink sink,
      BaseFlushSink baseFlushSink, TransitionPreviewer previewer,
      SnapshotDeltaPreparer snapshotDeltaPreparer, boolean deferredCapture) {
    this(collector, sink, baseFlushSink, previewer, snapshotDeltaPreparer, deferredCapture, false);
  }

  private PathStateRuntimeAttachment(PathStateTransitionCollector collector, TransitionSink sink,
      BaseFlushSink baseFlushSink, TransitionPreviewer previewer,
      SnapshotDeltaPreparer snapshotDeltaPreparer, boolean deferredCapture,
      boolean commonCheckpointOnly) {
    this.collector = Objects.requireNonNull(collector, "collector");
    this.sink = Objects.requireNonNull(sink, "sink");
    this.baseFlushSink = Objects.requireNonNull(baseFlushSink, "baseFlushSink");
    this.previewer = previewer;
    this.snapshotDeltaPreparer = snapshotDeltaPreparer;
    this.deferredCapture = deferredCapture;
    this.commonCheckpointOnly = commonCheckpointOnly;
    deferredQueue = deferredCapture ? new ArrayBlockingQueue<>(64) : null;
    deferredWorker = deferredCapture
        ? new Thread(this::runDeferred, "path-state-deferred-capture") : null;
    if (deferredWorker != null) {
      deferredWorker.setDaemon(true);
      deferredWorker.start();
    }
  }

  public boolean isCommonCheckpointOnly() {
    return commonCheckpointOnly;
  }

  /** Computes producer metadata without observing, publishing, or failing this runtime. */
  public synchronized byte[] preview(BlockChangeView view) {
    if (failure != null || previewer == null || status().getState() != State.READY) {
      return null;
    }
    try {
      PathStateBlockTransition transition = collectAndValidate(view);
      byte[] candidate = previewer.prepare(transition);
      if (candidate == null || candidate.length != 32) {
        throw new IOException("path-state preview root must be exactly 32 bytes");
      }
      return copy(candidate);
    } catch (IOException | RuntimeException previewFailure) {
      logger.warn("Path-state producer preview unavailable; state_root remains absent",
          previewFailure);
      return null;
    }
  }

  /** Capture failures fail only this shadow runtime and never reject the canonical block. */
  public synchronized PathStateBlockTransition capture(BlockChangeView view) {
    BlockChangeView admitted = Objects.requireNonNull(view, "view");
    observe(admitted);
    if (failure != null) {
      return null;
    }
    if (deferredCapture) {
      pendingView = admitted;
      return null;
    }
    try {
      PathStateBlockTransition transition = collectAndValidate(admitted);
      PathStateSnapshotDelta snapshotDelta = snapshotDeltaPreparer == null ? null
          : snapshotDeltaPreparer.prepare(admitted.getMeta(), transition);
      validateSnapshotDelta(admitted.getMeta(), transition, snapshotDelta);
      pending = transition;
      pendingSnapshotDelta = snapshotDelta;
      return transition;
    } catch (IOException | RuntimeException currentFailure) {
      fail(FailureStage.CAPTURE, currentFailure);
      return null;
    }
  }

  /** Durable publication failures are retained as observable fail-stop state. */
  public void publish(PathStateBlockTransition transition) {
    if (deferredCapture) {
      publishDeferred(transition);
      return;
    }
    publishNow(transition);
  }

  private synchronized void publishNow(PathStateBlockTransition transition) {
    if (failure != null || transition == null) {
      return;
    }
    try {
      if (pending != transition) {
        throw new IOException("path-state publication differs from captured transition");
      }
      sink.accept(transition);
      readyBlockNumber = transition.getBlockNumber();
      readyBlockHash = transition.getBlockHash();
      pending = null;
      pendingSnapshotDelta = null;
    } catch (IOException | RuntimeException currentFailure) {
      fail(FailureStage.PUBLISH, currentFailure);
    }
  }

  private void publishDeferred(PathStateBlockTransition transition) {
    BlockChangeView admitted;
    synchronized (this) {
      if (failure != null) {
        return;
      }
      if (transition != null || pendingView == null) {
        fail(FailureStage.PUBLISH,
            new IOException("deferred PathState publication has no captured view"));
        return;
      }
      admitted = pendingView;
      pendingView = null;
    }
    long startedNanos = System.nanoTime();
    try {
      while (true) {
        if (deferredWorkerUnavailable()) {
          return;
        }
        if (!deferredQueue.offer(admitted, 100L, TimeUnit.MILLISECONDS)) {
          continue;
        }
        if (deferredWorkerUnavailable()) {
          deferredQueue.remove(admitted);
          return;
        }
        logger.info("Path-state deferred view enqueued: head={}, queueDepth={}, enqueueMicros={}",
            admitted.getMeta().getBlockNumber(), deferredQueue.size(),
            TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startedNanos));
        return;
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      fail(FailureStage.PUBLISH, interrupted);
    }
  }

  private void runDeferred() {
    while (!closed || !deferredQueue.isEmpty()) {
      try {
        BlockChangeView view = deferredQueue.poll(100L, TimeUnit.MILLISECONDS);
        if (view == null) {
          continue;
        }
        long startedNanos = System.nanoTime();
        PathStateBlockTransition transition = collectAndValidate(view);
        PathStateSnapshotDelta delta = snapshotDeltaPreparer == null ? null
            : snapshotDeltaPreparer.prepare(view.getMeta(), transition);
        validateSnapshotDelta(view.getMeta(), transition, delta);
        sink.accept(transition);
        synchronized (this) {
          readyBlockNumber = transition.getBlockNumber();
          readyBlockHash = transition.getBlockHash();
        }
        logger.info("Path-state deferred view prepared: head={}, queueDepth={}, serviceMs={}",
            transition.getBlockNumber(), deferredQueue.size(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
      } catch (InterruptedException interrupted) {
        if (!closed) {
          Thread.currentThread().interrupt();
          fail(FailureStage.CAPTURE, interrupted);
        }
        return;
      } catch (Throwable currentFailure) {
        fail(FailureStage.CAPTURE, currentFailure);
        deferredQueue.clear();
        return;
      }
    }
  }

  private synchronized boolean deferredWorkerUnavailable() {
    if (failure != null) {
      return true;
    }
    if (closed) {
      fail(FailureStage.PUBLISH,
          new IOException("deferred PathState runtime closed while publishing"));
      return true;
    }
    if (!deferredWorker.isAlive()) {
      fail(FailureStage.CAPTURE,
          new IllegalStateException("deferred PathState worker terminated unexpectedly"));
      return true;
    }
    return false;
  }

  /** Drains the deferred benchmark worker before its Manager-owned head is closed. */
  public void close() throws IOException {
    if (deferredWorker == null) {
      return;
    }
    closed = true;
    try {
      deferredWorker.join(TimeUnit.SECONDS.toMillis(30));
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("deferred PathState close interrupted", interrupted);
    }
    if (deferredWorker.isAlive()) {
      deferredWorker.interrupt();
      throw new IOException("deferred PathState worker did not drain before close");
    }
    if (failure != null) {
      throw new IOException("deferred PathState worker failed", failure);
    }
  }

  /** Compacts only after Chainbase has durably refreshed the matching prefix. */
  public synchronized void flushBaseThrough(long blockNumber, byte[] blockHash) {
    if (failure != null) {
      return;
    }
    try {
      baseFlushSink.accept(blockNumber, blockHash);
    } catch (IOException | RuntimeException currentFailure) {
      fail(FailureStage.BASE_FLUSH, currentFailure);
    }
  }

  public synchronized boolean isFailed() {
    return failure != null;
  }

  /** Returns the exact optional delta bound to the currently captured transition. */
  public synchronized PathStateSnapshotDelta preparedSnapshotDelta(
      PathStateBlockTransition transition) {
    if (pending != Objects.requireNonNull(transition, "transition")) {
      throw new IllegalStateException("path-state Snapshot delta transition is not pending");
    }
    return pendingSnapshotDelta;
  }

  public synchronized Throwable getFailure() {
    return failure;
  }

  /** Returns a copy-only diagnostic snapshot; it never repairs or guesses a root. */
  public synchronized Status status() {
    State state = failure != null ? State.FAILED
        : pending == null && readyBlockNumber >= 0
        && readyBlockNumber == observedBlockNumber
        && Arrays.equals(readyBlockHash, observedBlockHash) ? State.READY : State.NOT_READY;
    return new Status(state, readyBlockNumber, readyBlockHash, observedBlockNumber,
        observedBlockHash, failureStage, classify(failure), failure, headerDiagnostic,
        headerDiagnosticBlockNumber, headerDiagnosticBlockHash);
  }

  /** True only when an owner root read cannot wait for a deferred predecessor transition. */
  public synchronized boolean isReadyForHeaderDiagnostic(long blockNumber, byte[] blockHash) {
    byte[] admittedHash = Objects.requireNonNull(blockHash, "blockHash");
    return failure == null && pending == null && pendingView == null
        && readyBlockNumber == observedBlockNumber
        && readyBlockNumber == blockNumber
        && Arrays.equals(readyBlockHash, observedBlockHash)
        && Arrays.equals(readyBlockHash, admittedHash);
  }

  /** Records a non-blocking comparison of carried header metadata against the local READY root. */
  public synchronized void diagnoseHeader(long blockNumber, byte[] blockHash, byte[] carriedRoot,
      byte[] localRoot) {
    headerDiagnosticBlockNumber = blockNumber;
    headerDiagnosticBlockHash = copy(blockHash);
    if (carriedRoot == null || carriedRoot.length == 0) {
      headerDiagnostic = HeaderDiagnostic.ABSENT;
      return;
    }
    if (carriedRoot.length != 32) {
      headerDiagnostic = HeaderDiagnostic.INVALID_LENGTH;
      logger.warn("Path-state header diagnostic: block={}, result={}, length={}", blockNumber,
          headerDiagnostic, carriedRoot.length);
      return;
    }
    if (failure != null || readyBlockNumber != blockNumber
        || !Arrays.equals(readyBlockHash, blockHash) || localRoot == null
        || localRoot.length != 32) {
      headerDiagnostic = HeaderDiagnostic.NOT_AVAILABLE;
      return;
    }
    headerDiagnostic = Arrays.equals(carriedRoot, localRoot)
        ? HeaderDiagnostic.MATCH : HeaderDiagnostic.MISMATCH;
    if (headerDiagnostic == HeaderDiagnostic.MISMATCH) {
      logger.warn("Path-state header diagnostic: block={}, result={}", blockNumber,
          headerDiagnostic);
    }
  }

  /** Seeds or rewinds the exact verified durable head; failed runtimes cannot be reset. */
  public synchronized void synchronizeReadyHead(PathStateRootMetadata metadata) {
    if (failure != null) {
      throw new IllegalStateException("failed path-state runtime cannot become ready");
    }
    if (pending != null) {
      throw new IllegalStateException("captured path-state transition is not published");
    }
    PathStateRootMetadata admitted = Objects.requireNonNull(metadata, "metadata");
    readyBlockNumber = admitted.getBlockNumber();
    readyBlockHash = admitted.getBlockHash();
    observedBlockNumber = readyBlockNumber;
    observedBlockHash = copy(readyBlockHash);
  }

  /** Marks an externally coordinated lifecycle operation as failed without replacing first cause. */
  public synchronized void fail(Throwable currentFailure) {
    fail(FailureStage.EXTERNAL, currentFailure);
  }

  public synchronized void fail(FailureStage stage, Throwable currentFailure) {
    if (failure == null) {
      failure = Objects.requireNonNull(currentFailure, "currentFailure");
      failureStage = Objects.requireNonNull(stage, "stage");
      logger.error(
          "Path-state runtime fail-stop: stage={}, kind={}, readyBlock={}, observedBlock={}, "
              + "rootLag={}",
          failureStage, classify(failure), readyBlockNumber, observedBlockNumber,
          lag(readyBlockNumber, observedBlockNumber),
          failure);
    }
  }

  /** Records the exact canonical target of a failed external lifecycle operation. */
  public synchronized void failAt(FailureStage stage, long blockNumber, byte[] blockHash,
      Throwable currentFailure) {
    observedBlockNumber = blockNumber;
    observedBlockHash = copy(Objects.requireNonNull(blockHash, "blockHash"));
    fail(stage, currentFailure);
  }

  private void observe(BlockChangeView view) {
    long number = view.getMeta().getBlockNumber();
    byte[] hash = view.getMeta().getBlockHash();
    byte[] parentHash = view.getMeta().getParentHash();
    if (failure == null) {
      if (pending != null || pendingView != null) {
        fail(FailureStage.CAPTURE_GAP,
            new IOException("path-state previous capture is not published"));
      }
      long expectedParentNumber = observedBlockNumber >= 0
          ? observedBlockNumber : readyBlockNumber;
      byte[] expectedParentHash = observedBlockHash != null
          ? observedBlockHash : readyBlockHash;
      if (expectedParentNumber >= 0 && (number != expectedParentNumber + 1
          || !Arrays.equals(parentHash, expectedParentHash))) {
        fail(FailureStage.CAPTURE_GAP,
            new IOException("path-state block-final capture is not continuous"));
      }
    }
    observedBlockNumber = number;
    observedBlockHash = copy(hash);
  }

  private PathStateBlockTransition collectAndValidate(BlockChangeView view) throws IOException {
    BlockChangeView admitted = Objects.requireNonNull(view, "view");
    PathStateBlockTransition transition = Objects.requireNonNull(collector.collect(admitted),
        "path-state collector returned null");
    if (transition.getBlockNumber() != admitted.getMeta().getBlockNumber()
        || !Arrays.equals(transition.getBlockHash(), admitted.getMeta().getBlockHash())
        || !Arrays.equals(transition.getParentHash(), admitted.getMeta().getParentHash())
        || transition.getTimestamp() != admitted.getMeta().getTimestamp()) {
      throw new IOException("path-state collector changed the captured block identity");
    }
    return transition;
  }

  private static void validateSnapshotDelta(BlockSnapshotMeta meta,
      PathStateBlockTransition transition, PathStateSnapshotDelta delta) throws IOException {
    if (delta == null) {
      return;
    }
    if (!meta.equals(delta.getMeta())
        || !Arrays.equals(transition.getPayloadDigest(), delta.getTransitionPayloadDigest())) {
      throw new IOException("path-state Snapshot delta identity mismatch");
    }
  }

  private static FailureKind classify(Throwable failure) {
    if (failure == null) {
      return FailureKind.NONE;
    }
    StringBuilder messages = new StringBuilder();
    Throwable current = failure;
    boolean io = false;
    while (current != null) {
      io |= current instanceof IOException;
      if (current.getMessage() != null) {
        messages.append(' ').append(current.getMessage().toLowerCase(Locale.ROOT));
      }
      current = current.getCause();
    }
    String text = messages.toString();
    if (text.contains("no space left") || text.contains("disk full")
        || text.contains("out of space")) {
      return FailureKind.STORAGE_FULL;
    }
    if (text.contains("corrupt") || text.contains("checksum") || text.contains("mismatch")
        || text.contains("orphan") || text.contains("native progress")) {
      return FailureKind.CORRUPTION;
    }
    return io ? FailureKind.IO : FailureKind.RUNTIME;
  }

  private static byte[] copy(byte[] value) {
    return value == null ? null : Arrays.copyOf(value, value.length);
  }

  private static long lag(long readyBlockNumber, long observedBlockNumber) {
    if (readyBlockNumber < 0 || observedBlockNumber < 0) {
      return -1;
    }
    return observedBlockNumber >= readyBlockNumber
        ? observedBlockNumber - readyBlockNumber : readyBlockNumber - observedBlockNumber;
  }

  public enum State {
    NOT_READY,
    READY,
    FAILED
  }

  public enum FailureStage {
    CAPTURE_GAP,
    CAPTURE,
    PUBLISH,
    BASE_FLUSH,
    REORG,
    EXTERNAL
  }

  public enum FailureKind {
    NONE,
    STORAGE_FULL,
    CORRUPTION,
    IO,
    RUNTIME
  }

  public enum HeaderDiagnostic {
    NONE,
    ABSENT,
    INVALID_LENGTH,
    NOT_AVAILABLE,
    MATCH,
    MISMATCH
  }

  public static final class Status {

    private final State state;
    private final long readyBlockNumber;
    private final byte[] readyBlockHash;
    private final long observedBlockNumber;
    private final byte[] observedBlockHash;
    private final FailureStage failureStage;
    private final FailureKind failureKind;
    private final String failureType;
    private final String failureMessage;
    private final HeaderDiagnostic headerDiagnostic;
    private final long headerDiagnosticBlockNumber;
    private final byte[] headerDiagnosticBlockHash;

    private Status(State state, long readyBlockNumber, byte[] readyBlockHash,
        long observedBlockNumber, byte[] observedBlockHash, FailureStage failureStage,
        FailureKind failureKind, Throwable failure, HeaderDiagnostic headerDiagnostic,
        long headerDiagnosticBlockNumber, byte[] headerDiagnosticBlockHash) {
      this.state = state;
      this.readyBlockNumber = readyBlockNumber;
      this.readyBlockHash = copy(readyBlockHash);
      this.observedBlockNumber = observedBlockNumber;
      this.observedBlockHash = copy(observedBlockHash);
      this.failureStage = failureStage;
      this.failureKind = failureKind;
      this.failureType = failure == null ? null : failure.getClass().getName();
      this.failureMessage = failure == null ? null : failure.getMessage();
      this.headerDiagnostic = headerDiagnostic;
      this.headerDiagnosticBlockNumber = headerDiagnosticBlockNumber;
      this.headerDiagnosticBlockHash = copy(headerDiagnosticBlockHash);
    }

    public State getState() {
      return state;
    }

    public long getReadyBlockNumber() {
      return readyBlockNumber;
    }

    public byte[] getReadyBlockHash() {
      return copy(readyBlockHash);
    }

    public long getObservedBlockNumber() {
      return observedBlockNumber;
    }

    public byte[] getObservedBlockHash() {
      return copy(observedBlockHash);
    }

    public long getRootLag() {
      return lag(readyBlockNumber, observedBlockNumber);
    }

    public FailureStage getFailureStage() {
      return failureStage;
    }

    public FailureKind getFailureKind() {
      return failureKind;
    }

    public String getFailureType() {
      return failureType;
    }

    public String getFailureMessage() {
      return failureMessage;
    }

    public HeaderDiagnostic getHeaderDiagnostic() {
      return headerDiagnostic;
    }

    public long getHeaderDiagnosticBlockNumber() {
      return headerDiagnosticBlockNumber;
    }

    public byte[] getHeaderDiagnosticBlockHash() {
      return copy(headerDiagnosticBlockHash);
    }
  }

  @FunctionalInterface
  public interface TransitionSink {

    void accept(PathStateBlockTransition transition) throws IOException;
  }

  @FunctionalInterface
  public interface BaseFlushSink {

    void accept(long blockNumber, byte[] blockHash) throws IOException;
  }

  @FunctionalInterface
  public interface TransitionPreviewer {

    byte[] prepare(PathStateBlockTransition transition) throws IOException;
  }

  @FunctionalInterface
  public interface SnapshotDeltaPreparer {

    PathStateSnapshotDelta prepare(BlockSnapshotMeta meta, PathStateBlockTransition transition)
        throws IOException;
  }
}
