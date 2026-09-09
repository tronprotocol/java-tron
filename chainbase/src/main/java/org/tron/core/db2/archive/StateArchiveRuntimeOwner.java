package org.tron.core.db2.archive;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.tron.core.db2.archive.ArchiveProgressEnvelope.Kind;
import org.tron.core.db2.core.SnapshotManager;

/** Sole owner for exact-27 State Archive resources from recovered startup through shutdown. */
public final class StateArchiveRuntimeOwner implements Closeable {

  static final String SERVING_INDEX_RUNTIME_DIRECTORY = ".serving-index-runtime";

  public enum ServingIndexStage {
    BEFORE_BUILD,
    GENERATION_INSTALLED,
    CURRENT_PUBLISHED
  }

  @FunctionalInterface
  public interface ServingIndexFaultHook {

    void afterStage(ServingIndexStage stage) throws IOException;
  }

  public enum ReadableStateStage {
    CANONICAL_REFRESHED,
    LATEST_PUBLISHED,
    READABLE_PUBLISHED
  }

  @FunctionalInterface
  public interface ReadableStateFaultHook {

    void afterStage(ReadableStateStage stage) throws IOException;
  }

  public enum State {
    RECOVERED,
    RUNNING,
    QUIESCING,
    CLOSED,
    FAILED_CLOSED
  }

  private final SnapshotManager snapshotManager;
  private final Path archiveDirectory;
  private final long maxSegmentSize;
  private final List<Closeable> participants;
  private final BlockSnapshotMeta recoveredHead;
  private final int startupRecoveryActionCount;
  private final ServingIndexFaultHook servingIndexFaultHook;
  private final ReadableStateFaultHook readableStateFaultHook;
  private ArchiveRuntimeAttachment attachment;
  private ArchiveRuntimeQueryGate queryGate;
  private Closeable latestCoordinator;
  private Closeable servingCatalog;
  private PersistentServingKeyIndexCatalog servingIndexCatalog;
  // Process-owned writer: checkpoint targets borrow it and only owner shutdown closes it.
  private PersistentServingKeyIndexGeneration.RuntimeBuilder servingIndexBuilder;
  private LatestStateGenerationCoordinator latestStateCoordinator;
  private BlockSnapshotMeta latestAuthorityHead;
  private volatile BlockSnapshotMeta readableHead;
  private Closeable sink;
  private ArchiveHistoryWriter historyWriter;
  private ServingIndexApplyStatistics lastServingApply;
  private State state;
  private boolean detached;
  private IOException terminalFailure;

  public StateArchiveRuntimeOwner(SnapshotManager snapshotManager,
      ArchiveRuntimeAttachment attachment, ArchiveRuntimeQueryGate queryGate,
      Closeable latestCoordinator, Closeable servingCatalog,
      List<? extends Closeable> participants) {
    this.snapshotManager = Objects.requireNonNull(snapshotManager, "snapshotManager");
    this.archiveDirectory = null;
    this.maxSegmentSize = 0;
    this.attachment = Objects.requireNonNull(attachment, "attachment");
    this.queryGate = Objects.requireNonNull(queryGate, "queryGate");
    this.latestCoordinator = Objects.requireNonNull(latestCoordinator, "latestCoordinator");
    this.servingCatalog = Objects.requireNonNull(servingCatalog, "servingCatalog");
    this.servingIndexCatalog = null;
    this.servingIndexBuilder = null;
    this.latestStateCoordinator = null;
    this.latestAuthorityHead = null;
    this.readableHead = null;
    this.lastServingApply = null;
    if (!(attachment.getSink() instanceof Closeable)) {
      throw new IllegalArgumentException("Attached archive sink must be Closeable");
    }
    this.sink = (Closeable) attachment.getSink();
    this.participants = immutableParticipants(participants);
    this.recoveredHead = null;
    this.startupRecoveryActionCount = 0;
    this.servingIndexFaultHook = stage -> { };
    this.readableStateFaultHook = stage -> { };
    this.state = State.RUNNING;
    validateUniqueOwnership();
  }

  private StateArchiveRuntimeOwner(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize, BlockSnapshotMeta recoveredHead,
      int startupRecoveryActionCount, ServingIndexFaultHook servingIndexFaultHook,
      ReadableStateFaultHook readableStateFaultHook) {
    this.snapshotManager = Objects.requireNonNull(snapshotManager, "snapshotManager");
    this.archiveDirectory = Objects.requireNonNull(archiveDirectory, "archiveDirectory");
    this.maxSegmentSize = maxSegmentSize;
    this.attachment = null;
    this.queryGate = null;
    this.latestCoordinator = null;
    this.servingCatalog = null;
    this.servingIndexCatalog = null;
    this.servingIndexBuilder = null;
    this.latestStateCoordinator = null;
    this.latestAuthorityHead = null;
    this.lastServingApply = null;
    this.readableHead = null;
    this.participants = Collections.emptyList();
    this.sink = null;
    this.recoveredHead = Objects.requireNonNull(recoveredHead, "recoveredHead");
    this.startupRecoveryActionCount = startupRecoveryActionCount;
    this.servingIndexFaultHook = Objects.requireNonNull(servingIndexFaultHook,
        "servingIndexFaultHook");
    this.readableStateFaultHook = Objects.requireNonNull(readableStateFaultHook,
        "readableStateFaultHook");
    this.state = State.RECOVERED;
  }

  /** Opens and validates the committed history authority before normal writes are attached. */
  public static StateArchiveRuntimeOwner recover(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize) throws IOException {
    return recover(snapshotManager, archiveDirectory, maxSegmentSize, stage -> { });
  }

  public static StateArchiveRuntimeOwner recover(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize, ServingIndexFaultHook servingIndexFaultHook)
      throws IOException {
    return recover(snapshotManager, archiveDirectory, maxSegmentSize, servingIndexFaultHook,
        stage -> { });
  }

  public static StateArchiveRuntimeOwner recover(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize, ServingIndexFaultHook servingIndexFaultHook,
      ReadableStateFaultHook readableStateFaultHook) throws IOException {
    Objects.requireNonNull(snapshotManager, "snapshotManager");
    Path root = Objects.requireNonNull(archiveDirectory, "archiveDirectory");
    HistoryCommitMarker head;
    try (ArchiveHistoryWriter history = new ArchiveHistoryWriter(root, maxSegmentSize,
        ArchiveStoreScope.getStateDatabases())) {
      head = history.committedHead();
    }
    if (head == null) {
      throw new ArchivePersistenceException("State Archive recovered H head is missing");
    }
    return new StateArchiveRuntimeOwner(snapshotManager, root, maxSegmentSize, head.getMeta(), 0,
        servingIndexFaultHook, readableStateFaultHook);
  }

  /**
   * Atomically establishes one empty-diff H baseline at the persisted Chainbase head, then
   * reopens it through the ordinary startup path. No legacy participant/progress path is created.
   */
  public static StateArchiveRuntimeOwner bootstrapAndRecover(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize, BlockSnapshotMeta baseHead) throws IOException {
    return bootstrapAndRecover(snapshotManager, archiveDirectory, maxSegmentSize, baseHead,
        stage -> { });
  }

  public static StateArchiveRuntimeOwner bootstrapAndRecover(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize, BlockSnapshotMeta baseHead,
      ServingIndexFaultHook servingIndexFaultHook) throws IOException {
    return bootstrapAndRecover(snapshotManager, archiveDirectory, maxSegmentSize, baseHead,
        servingIndexFaultHook, stage -> { });
  }

  public static StateArchiveRuntimeOwner bootstrapAndRecover(SnapshotManager snapshotManager,
      Path archiveDirectory, long maxSegmentSize, BlockSnapshotMeta baseHead,
      ServingIndexFaultHook servingIndexFaultHook,
      ReadableStateFaultHook readableStateFaultHook) throws IOException {
    Objects.requireNonNull(snapshotManager, "snapshotManager");
    Path root = Objects.requireNonNull(archiveDirectory, "archiveDirectory");
    BlockSnapshotMeta head = Objects.requireNonNull(baseHead, "baseHead");
    Path parent = Objects.requireNonNull(root.getParent(), "archive parent directory");
    requireEmptyBootstrapTarget(root);
    Files.createDirectories(parent);
    Path staging = parent.resolve("." + root.getFileName() + ".bootstrap-" + UUID.randomUUID());
    List<String> names = storeNames();
    try {
      HistoryCommitMarker marker;
      try (ArchiveHistoryWriter writer = new ArchiveHistoryWriter(staging, maxSegmentSize,
          new java.util.LinkedHashSet<>(names))) {
        writer.accept(new BlockReverseDiff(head, Collections.emptyList()));
        marker = Objects.requireNonNull(writer.committedHead(), "bootstrap history head");
      }

      ArchiveBootstrapAnchor.store(staging, marker, names);

      try (StateArchiveRuntimeOwner verified = recover(snapshotManager, staging,
          maxSegmentSize)) {
        if (!head.equals(verified.getRecoveredHead())
            || verified.getStartupRecoveryActionCount() != 0) {
          throw new ArchivePersistenceException(
              "Fresh State Archive baseline did not recover at a zero-action fixed point");
        }
      }
      if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
        Files.delete(root);
      }
      try {
        Files.move(staging, root, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException failure) {
        throw new ArchivePersistenceException(
            "State Archive bootstrap requires atomic directory publication", failure);
      }
      HistorySegmentStore.syncDirectory(parent);
      return recover(snapshotManager, root, maxSegmentSize, servingIndexFaultHook,
          readableStateFaultHook);
    } catch (IOException | RuntimeException failure) {
      throw failure;
    }
  }

  public synchronized State getState() {
    return state;
  }

  public BlockSnapshotMeta getRecoveredHead() {
    if (recoveredHead == null) {
      throw new IllegalStateException("State Archive runtime has no startup recovery head");
    }
    return recoveredHead;
  }

  public int getStartupRecoveryActionCount() {
    return startupRecoveryActionCount;
  }

  /** Continues this recovered owner into one atomically attached normal-write runtime. */
  public synchronized ArchiveHistoryWriter attachNormalWriter(OldValueCollector collector,
      int queueCapacity, BlockSnapshotMeta canonicalHead) throws IOException {
    return attachNormalWriter(collector, queueCapacity, canonicalHead, Collections.emptyMap());
  }

  public synchronized ArchiveHistoryWriter attachNormalWriter(OldValueCollector collector,
      int queueCapacity, BlockSnapshotMeta canonicalHead,
      Map<String, LatestStateGenerationAdapter.SnapshotCapableStore> supplementalStores)
      throws IOException {
    if (state != State.RECOVERED) {
      throw new IllegalStateException("State Archive owner is not recovered");
    }
    ArchiveHistoryWriter writer = null;
    AsyncArchiveHistorySink asyncSink = null;
    PersistentServingKeyIndexCatalog catalog = null;
    PersistentServingKeyIndexGeneration.RuntimeBuilder builder = null;
    LatestStateGenerationCoordinator latest = null;
    ArchiveRuntimeAttachment candidate = null;
    boolean attached = false;
    try {
      writer = new ArchiveHistoryWriter(archiveDirectory, maxSegmentSize,
          ArchiveStoreScope.getStateDatabases());
      if (!recoveredHead.equals(writer.committedHeadMeta())) {
        throw new ArchivePersistenceException(
            "Recovered archive head changed before normal writer attachment");
      }
      ArchiveWalStartupValidator.requireFixedPoint(writer, canonicalHead,
          snapshotManager.getRecoveredArchiveWalBinding(),
          storeNames());
      catalog = openOrCreateServingCatalog(writer);
      validateServingIndex(writer, catalog, canonicalHead);
      try (PersistentServingKeyIndexGeneration current = catalog.pin()) {
        builder = current.openRuntimeBuilder(
            archiveDirectory.resolve(SERVING_INDEX_RUNTIME_DIRECTORY));
      }
      latest = LatestStateGenerationCoordinatorFactory.create(snapshotManager,
          supplementalStores, this::readLatestAuthority);
      restoreLatestState(writer, catalog, builder, latest, canonicalHead);
      if (lastServingApply == null) {
        lastServingApply = ServingIndexApplyStatistics.zeroAction(canonicalHead);
      }
      asyncSink = new AsyncArchiveHistorySink(writer, queueCapacity);
      ArchiveHistoryWriter attachedWriter = writer;
      PersistentServingKeyIndexCatalog attachedCatalog = catalog;
      PersistentServingKeyIndexGeneration.RuntimeBuilder attachedBuilder = builder;
      LatestStateGenerationCoordinator attachedLatest = latest;
      candidate = new ArchiveRuntimeAttachment(collector, asyncSink,
          target -> publishServingIndex(attachedWriter, attachedCatalog, target),
          target -> publishReadableState(attachedWriter, attachedCatalog, attachedBuilder,
              attachedLatest, target));
      snapshotManager.attachArchiveRuntime(candidate);
      attached = true;
      snapshotManager.markArchiveReadableThrough(canonicalHead.getEpoch());
      validateReadableState(catalog, latest, canonicalHead);
      attachment = candidate;
      sink = asyncSink;
      historyWriter = writer;
      servingIndexCatalog = catalog;
      servingIndexBuilder = builder;
      latestStateCoordinator = latest;
      latestCoordinator = latest;
      servingCatalog = catalog;
      readableHead = canonicalHead;
      queryGate = new ArchiveRuntimeQueryGate(new ArchiveGenerationCapsule(catalog,
          archiveDirectory, maxSegmentSize, latest, this::readReadableAuthority));
      state = State.RUNNING;
      return writer;
    } catch (IOException | RuntimeException failure) {
      if (attached) {
        snapshotManager.detachArchiveRuntime(candidate);
      }
      if (asyncSink != null) {
        try {
          asyncSink.close();
        } catch (IOException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      } else if (writer != null) {
        try {
          writer.close();
        } catch (IOException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      }
      if (builder != null) {
        try {
          builder.close();
        } catch (IOException | RuntimeException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      }
      if (catalog != null) {
        try {
          catalog.close();
        } catch (IOException | RuntimeException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      }
      if (latest != null) {
        try {
          latest.close();
        } catch (IOException | RuntimeException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      }
      throw failure;
    }
  }

  public synchronized ArchiveHistoryWriter getHistoryWriter() {
    if (state != State.RUNNING || historyWriter == null) {
      throw new IllegalStateException("State Archive normal writer is not attached");
    }
    return historyWriter;
  }

  /** Acquires one request-owned historical snapshot from the currently published fixed point. */
  public synchronized ArchiveRuntimeQueryGate.Lease pinHistoricalState(long targetBlock)
      throws IOException {
    if (state != State.RUNNING || queryGate == null) {
      throw new IllegalStateException("State Archive historical query runtime is not running");
    }
    return queryGate.pin(targetBlock);
  }

  /** Machine-checks the latest committed H against the last Chainbase WAL binding. */
  public synchronized BlockSnapshotMeta verifyNormalWriteFixedPoint() throws IOException {
    ArchiveHistoryWriter writer = getHistoryWriter();
    HistoryCommitMarker head = Objects.requireNonNull(writer.committedHead(),
        "archive history head");
    ArchiveWalBinding binding = snapshotManager.getLatestArchiveWalBinding();
    if (binding == null) {
      binding = snapshotManager.getRecoveredArchiveWalBinding();
    }
    ArchiveWalStartupValidator.requireFixedPoint(writer, head.getMeta(),
        binding, storeNames());
    validateServingIndex(writer, requireServingIndexCatalog(), head.getMeta());
    validateReadableState(requireServingIndexCatalog(), head.getMeta());
    return head.getMeta();
  }

  /** Returns an explicit measured snapshot of the current reader-visible v5 serving index. */
  public synchronized ServingIndexInspection inspectServingIndex() throws IOException {
    BlockSnapshotMeta head = verifyNormalWriteFixedPoint();
    PersistentServingKeyIndexCatalog catalog = requireServingIndexCatalog();
    try (PersistentServingKeyIndexGeneration serving = catalog.pin()) {
      PersistentServingKeyIndexGeneration.GenerationStatistics generation =
          serving.inspectStatistics();
      ServingIndexApplyStatistics apply = Objects.requireNonNull(lastServingApply,
          "last serving apply statistics");
      if (apply.getIndexedThrough() != head.getEpoch()) {
        throw new ArchivePersistenceException(
            "Serving inspection apply boundary differs from readable fixed point");
      }
      long backlog = head.getEpoch() - generation.getIndexedThrough();
      if (backlog != 0) {
        throw new ArchivePersistenceException(
            "Serving inspection requires zero history-to-index backlog");
      }
      return new ServingIndexInspection(head, generation, apply, backlog);
    }
  }

  private ArchiveProgressEnvelope readLatestAuthority() {
    BlockSnapshotMeta target = latestAuthorityHead;
    if (target == null) {
      throw new ArchivePersistenceException("Latest-state authority is not being published");
    }
    return new ArchiveProgressEnvelope(Kind.READER_VISIBLE, null, target.getEpoch(),
        target.getBlockHash(), new byte[16], new byte[32], storeNames());
  }

  private synchronized void restoreLatestState(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexCatalog catalog,
      PersistentServingKeyIndexGeneration.RuntimeBuilder builder,
      LatestStateGenerationCoordinator latest, BlockSnapshotMeta target) throws IOException {
    try (PersistentServingKeyIndexGeneration serving = catalog.pin()) {
      validateServingGeneration(writer, serving, target);
      if (serving.isLatestSourceIdentityBound()) {
        publishExistingLatest(latest, serving, target);
        return;
      }
    }
    bindAndPublishLatest(writer, catalog, builder, latest, target);
  }

  private synchronized void publishReadableState(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexCatalog catalog,
      PersistentServingKeyIndexGeneration.RuntimeBuilder builder,
      LatestStateGenerationCoordinator latest, BlockSnapshotMeta target) throws IOException {
    if (!target.equals(writer.committedHeadMeta())) {
      throw new ArchivePersistenceException(
          "Readable-state target differs from committed history head");
    }
    readableStateFaultHook.afterStage(ReadableStateStage.CANONICAL_REFRESHED);
    bindAndPublishLatest(writer, catalog, builder, latest, target);
    readableStateFaultHook.afterStage(ReadableStateStage.LATEST_PUBLISHED);
    long previousReadable = snapshotManager.getArchiveReadableEpoch();
    BlockSnapshotMeta previousReadableHead = readableHead;
    snapshotManager.markArchiveReadableThrough(target.getEpoch());
    try {
      validateReadableState(catalog, target);
      readableHead = target;
      readableStateFaultHook.afterStage(ReadableStateStage.READABLE_PUBLISHED);
    } catch (IOException | RuntimeException failure) {
      snapshotManager.markArchiveReadableThrough(previousReadable);
      readableHead = previousReadableHead;
      throw failure;
    }
  }

  private void publishExistingLatest(LatestStateGenerationCoordinator latest,
      PersistentServingKeyIndexGeneration serving, BlockSnapshotMeta target) throws IOException {
    latestAuthorityHead = target;
    try (LatestStateGenerationCoordinator.Candidate candidate =
        latest.acquire(serving.getGenerationId())) {
      if (!latest.publish(null, candidate, serving)) {
        throw new ArchivePersistenceException("Latest-state startup publication changed");
      }
    } finally {
      latestAuthorityHead = null;
    }
  }

  private void bindAndPublishLatest(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexCatalog catalog,
      PersistentServingKeyIndexGeneration.RuntimeBuilder builder,
      LatestStateGenerationCoordinator latest, BlockSnapshotMeta target) throws IOException {
    String expectedLatest = latest.getCurrentGenerationId();
    long started = System.nanoTime();
    latestAuthorityHead = target;
    try (PersistentServingKeyIndexGeneration current = catalog.pin()) {
      ServingIndexIncrementalPlan plan = writer.planServingIncrement(current.getIndexedThrough(),
          current.getHeadHash());
      boolean targetAlreadyIndexed = plan.getIndexedFrom() == plan.getIndexedThrough();
      if (targetAlreadyIndexed && current.isLatestSourceIdentityBound()) {
        try (LatestStateGenerationCoordinator.Candidate candidate =
            latest.acquire(current.getGenerationId())) {
          if (Arrays.equals(current.getLatestSourceIdentityDigest(),
              candidate.getSourceIdentityDigest())) {
            if (!latest.publish(expectedLatest, candidate, current)) {
              throw new ArchivePersistenceException(
                  "Latest-state generation changed during zero-action publish");
            }
            lastServingApply = ServingIndexApplyStatistics.from(plan, false,
                System.nanoTime() - started);
            return;
          }
        }
      }
      try (LatestStateGenerationCoordinator.Candidate candidate =
          latest.acquire(generationId(target))) {
        publishServingAndLatest(catalog, builder, latest, current, plan, candidate,
            expectedLatest, target);
        lastServingApply = ServingIndexApplyStatistics.from(plan, true,
            System.nanoTime() - started);
      }
    } finally {
      latestAuthorityHead = null;
    }
  }

  private void publishServingAndLatest(PersistentServingKeyIndexCatalog catalog,
      PersistentServingKeyIndexGeneration.RuntimeBuilder builder,
      LatestStateGenerationCoordinator latest, PersistentServingKeyIndexGeneration current,
      ServingIndexIncrementalPlan plan, LatestStateGenerationCoordinator.Candidate candidate,
      String expectedLatest, BlockSnapshotMeta target) throws IOException {
    String generationId = candidate.getGenerationId();
    Path shadow = archiveDirectory.resolve(".serving-index-build-" + UUID.randomUUID());
    try (PersistentServingKeyIndexGeneration built = builder.extendExact(shadow,
        generationId, plan, candidate.getSourceIdentityDigest())) {
      validateIncrementCandidate(current, plan, built, target);
    }
    String expectedServing = catalog.getCurrentGenerationId();
    if (!catalog.publish(expectedServing, shadow)) {
      throw new ArchivePersistenceException(
          "Serving index catalog changed during latest-state publication");
    }
    try (PersistentServingKeyIndexGeneration serving = catalog.pin()) {
      if (!latest.publish(expectedLatest, candidate, serving)) {
        throw new ArchivePersistenceException("Latest-state generation changed during publish");
      }
    }
  }

  private static void validateIncrementCandidate(PersistentServingKeyIndexGeneration current,
      ServingIndexIncrementalPlan plan, PersistentServingKeyIndexGeneration built,
      BlockSnapshotMeta target) throws IOException {
    if (!built.isExactOnlyFormat()
        || built.getIndexedFrom() != current.getIndexedFrom()
        || built.getIndexedThrough() != target.getEpoch()
        || !Arrays.equals(built.getHeadHash(), target.getBlockHash())
        || !built.getParticipatingDatabases().equals(storeNames())) {
      throw new ArchivePersistenceException("Incremental serving candidate identity mismatch");
    }
    for (String store : storeNames()) {
      PersistentServingKeyIndexGeneration.PersistentStoreCoverage coverage =
          built.getPersistentStoreCoverage(store);
      if (coverage.getIndexedThrough() != plan.getIndexedThrough()) {
        throw new ArchivePersistenceException(
            "Incremental serving candidate Store coverage mismatch: " + store);
      }
    }
  }

  private void validateReadableState(PersistentServingKeyIndexCatalog catalog,
      BlockSnapshotMeta target) throws IOException {
    LatestStateGenerationCoordinator latest = latestStateCoordinator;
    if (latest == null) {
      throw new ArchivePersistenceException("Latest-state coordinator is not attached");
    }
    validateReadableState(catalog, latest, target);
  }

  private void validateReadableState(PersistentServingKeyIndexCatalog catalog,
      LatestStateGenerationCoordinator latest, BlockSnapshotMeta target) throws IOException {
    try (PersistentServingKeyIndexGeneration serving = catalog.pin()) {
      if (!serving.isLatestSourceIdentityBound()
          || !serving.getGenerationId().equals(latest.getCurrentGenerationId())
          || serving.getIndexedThrough() != target.getEpoch()
          || !Arrays.equals(serving.getHeadHash(), target.getBlockHash())
          || snapshotManager.getArchiveReadableEpoch() != target.getEpoch()) {
        throw new ArchivePersistenceException("Archive P/H/I/latest/R fixed point mismatch");
      }
    }
  }

  private ArchiveProgressEnvelope readReadableAuthority() throws IOException {
    BlockSnapshotMeta head = readableHead;
    PersistentServingKeyIndexCatalog catalog = servingIndexCatalog;
    LatestStateGenerationCoordinator latest = latestStateCoordinator;
    // H and the WAL binding may advance before the next reader generation is published. Queries
    // continue to pin the previous immutable serving/latest generation under this owner's lock;
    // write/recovery callers retain the stricter P/H/I/latest/R check above.
    if (head == null || catalog == null || latest == null
        || snapshotManager.getArchiveReadableEpoch() != head.getEpoch()) {
      throw new ArchivePersistenceException(
          "Archive historical query has no published reader generation");
    }
    try (PersistentServingKeyIndexGeneration serving = catalog.pin()) {
      if (!serving.isLatestSourceIdentityBound()
          || !serving.getGenerationId().equals(latest.getCurrentGenerationId())
          || serving.getIndexedThrough() != head.getEpoch()
          || !Arrays.equals(serving.getHeadHash(), head.getBlockHash())) {
        throw new ArchivePersistenceException(
            "Archive historical query generation is outside readable R");
      }
    }
    return new ArchiveProgressEnvelope(Kind.READER_VISIBLE, null, head.getEpoch(),
        head.getBlockHash(), new byte[16], new byte[32], storeNames());
  }

  private PersistentServingKeyIndexCatalog openOrCreateServingCatalog(
      ArchiveHistoryWriter writer) throws IOException {
    Path root = archiveDirectory.resolve("serving-index");
    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      PersistentServingKeyIndexCatalog catalog =
          PersistentServingKeyIndexCatalog.open(root,
              StateArchiveCheckpointServingIndex.configuredEngine(), this::afterCatalogStage);
      try {
        upgradeServingExactIndex(writer, catalog);
        return catalog;
      } catch (IOException | RuntimeException failure) {
        try {
          catalog.close();
        } catch (IOException | RuntimeException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
        throw failure;
      }
    }
    String generationId = generationId(writer.committedHeadMeta());
    Path shadow = archiveDirectory.resolve(".serving-index-build-" + UUID.randomUUID());
    ServingIndexIncrementalPlan plan = writer.planServingRebuild();
    long started = System.nanoTime();
    try (PersistentServingKeyIndexGeneration ignored =
        PersistentServingKeyIndexGeneration.buildExact(shadow, generationId, plan,
            new byte[32])) {
      // The catalog reopens and validates the immutable generation before publishing it.
    }
    PersistentServingKeyIndexCatalog catalog = PersistentServingKeyIndexCatalog.create(root,
        shadow, this::afterCatalogStage);
    lastServingApply = ServingIndexApplyStatistics.from(plan, true,
        System.nanoTime() - started);
    return catalog;
  }

  private void upgradeServingExactIndex(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexCatalog catalog) throws IOException {
    String expected = catalog.getCurrentGenerationId();
    byte[] latestSourceIdentityDigest;
    BlockSnapshotMeta target = writer.committedHeadMeta();
    try (PersistentServingKeyIndexGeneration current = catalog.pin()) {
      if (current.isExactOnlyFormat()
          && current.getIndexedThrough() == target.getEpoch()
          && Arrays.equals(current.getHeadHash(), target.getBlockHash())) {
        validateServingGeneration(writer, current, target);
        return;
      }
      latestSourceIdentityDigest = current.getLatestSourceIdentityDigest();
      if (current.isExactOnlyFormat()) {
        ServingIndexIncrementalPlan plan = writer.planServingIncrement(
            current.getIndexedThrough(), current.getHeadHash());
        long started = System.nanoTime();
        Path shadow = archiveDirectory.resolve(".serving-index-build-" + UUID.randomUUID());
        try (PersistentServingKeyIndexGeneration candidate = current.extendExact(shadow,
            generationId(target), plan, latestSourceIdentityDigest)) {
          validateIncrementCandidate(current, plan, candidate, target);
        }
        if (!catalog.publish(expected, shadow)) {
          throw new ArchivePersistenceException(
              "Serving index catalog changed during startup catch-up");
        }
        lastServingApply = ServingIndexApplyStatistics.from(plan, true,
            System.nanoTime() - started);
        return;
      }
      validateServingGeneration(writer, current, target);
    }
    Path shadow = archiveDirectory.resolve(".serving-index-build-" + UUID.randomUUID());
    ServingIndexIncrementalPlan plan = writer.planServingRebuild();
    long started = System.nanoTime();
    try (PersistentServingKeyIndexGeneration candidate =
        PersistentServingKeyIndexGeneration.buildExact(shadow, generationId(target),
            plan, latestSourceIdentityDigest)) {
      validateServingGeneration(writer, candidate, target);
    }
    if (!catalog.publish(expected, shadow)) {
      throw new ArchivePersistenceException(
          "Serving index catalog changed during exact-index upgrade");
    }
    lastServingApply = ServingIndexApplyStatistics.from(plan, true,
        System.nanoTime() - started);
  }

  private synchronized void publishServingIndex(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexCatalog catalog, BlockSnapshotMeta target) throws IOException {
    BlockSnapshotMeta historyHead = writer.committedHeadMeta();
    if (!target.equals(historyHead)) {
      throw new ArchivePersistenceException(
          "Serving index target differs from committed history head");
    }
    servingIndexFaultHook.afterStage(ServingIndexStage.BEFORE_BUILD);
    try (PersistentServingKeyIndexGeneration current = catalog.pin()) {
      if (!current.isExactOnlyFormat() || current.getIndexedThrough() > target.getEpoch()) {
        throw new ArchivePersistenceException(
            "Serving index cannot increment from current durable I");
      }
      writer.planServingIncrement(current.getIndexedThrough(), current.getHeadHash());
    }
  }

  private void afterCatalogStage(PersistentServingKeyIndexCatalog.PublicationStage stage)
      throws IOException {
    servingIndexFaultHook.afterStage(stage
        == PersistentServingKeyIndexCatalog.PublicationStage.GENERATION_INSTALLED
        ? ServingIndexStage.GENERATION_INSTALLED : ServingIndexStage.CURRENT_PUBLISHED);
  }

  private static String generationId(BlockSnapshotMeta target) {
    return "h-" + target.getEpoch() + "-" + UUID.randomUUID();
  }

  private PersistentServingKeyIndexCatalog requireServingIndexCatalog() {
    if (servingIndexCatalog == null) {
      throw new IllegalStateException("State Archive serving index is not attached");
    }
    return servingIndexCatalog;
  }

  private static void validateServingIndex(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexCatalog catalog, BlockSnapshotMeta target) throws IOException {
    try (PersistentServingKeyIndexGeneration pinned = catalog.pin()) {
      validateServingGeneration(writer, pinned, target);
    }
  }

  private static void validateServingGeneration(ArchiveHistoryWriter writer,
      PersistentServingKeyIndexGeneration generation, BlockSnapshotMeta target)
      throws IOException {
    if (generation.isExactOnlyFormat()) {
      ServingIndexIncrementalPlan expected = writer.planServingRebuild();
      if (generation.getIndexedFrom() != expected.getIndexedFrom()
          || generation.getIndexedThrough() != target.getEpoch()
          || expected.getIndexedThrough() != target.getEpoch()
          || !Arrays.equals(generation.getHeadHash(), target.getBlockHash())
          || !Arrays.equals(expected.getHeadHash(), target.getBlockHash())
          || !Arrays.equals(generation.getAuthoritativePrefixDigest(),
          PersistentServingKeyIndexGeneration.sourceDigestForRebuild(expected))
          || !generation.getParticipatingDatabases().equals(storeNames())) {
        throw new ArchivePersistenceException(
            "Exact serving index differs from committed history authority");
      }
      for (String store : storeNames()) {
        generation.getPersistentStoreCoverage(store);
      }
      return;
    }
    ServingKeyIndexGeneration expected = writer.buildServingIdentity("expected");
    List<String> stores = storeNames();
    if (generation.getIndexedFrom() != expected.getIndexedFrom()
        || generation.getIndexedThrough() != target.getEpoch()
        || expected.getIndexedThrough() != target.getEpoch()
        || !Arrays.equals(generation.getHeadHash(), target.getBlockHash())
        || !Arrays.equals(expected.getHeadHash(), target.getBlockHash())
        || !Arrays.equals(generation.getAuthoritativePrefixDigest(),
        expected.getAuthoritativePrefixDigest())
        || !generation.getParticipatingDatabases().equals(stores)) {
      throw new ArchivePersistenceException(
          "Serving index generation differs from committed history authority");
    }
    for (String store : stores) {
      if (!expected.getStoreCoverage(store).isPresent()) {
        throw new ArchivePersistenceException(
            "Serving index identity is missing Store coverage: " + store);
      }
    }
  }

  /** One fixed-point inspection result; collection performs no publication or mutation. */
  public static final class ServingIndexInspection {
    private final BlockSnapshotMeta readableHead;
    private final PersistentServingKeyIndexGeneration.GenerationStatistics generation;
    private final ServingIndexApplyStatistics lastApply;
    private final long historyToServingBacklog;

    private ServingIndexInspection(BlockSnapshotMeta readableHead,
        PersistentServingKeyIndexGeneration.GenerationStatistics generation,
        ServingIndexApplyStatistics lastApply, long historyToServingBacklog) {
      this.readableHead = readableHead;
      this.generation = generation;
      this.lastApply = lastApply;
      this.historyToServingBacklog = historyToServingBacklog;
    }

    public BlockSnapshotMeta getReadableHead() {
      return readableHead;
    }

    public PersistentServingKeyIndexGeneration.GenerationStatistics getGeneration() {
      return generation;
    }

    public ServingIndexApplyStatistics getLastApply() {
      return lastApply;
    }

    public long getHistoryToServingBacklog() {
      return historyToServingBacklog;
    }
  }

  /** In-memory counters for the last successful serving publication at readable R. */
  public static final class ServingIndexApplyStatistics {
    private final long indexedFrom;
    private final long indexedThrough;
    private final int validatedCommitCount;
    private final Map<String, Long> changedEntriesByStore;
    private final boolean generationCreated;
    private final long elapsedNanos;

    private ServingIndexApplyStatistics(long indexedFrom, long indexedThrough,
        int validatedCommitCount, Map<String, Long> changedEntriesByStore,
        boolean generationCreated, long elapsedNanos) {
      this.indexedFrom = indexedFrom;
      this.indexedThrough = indexedThrough;
      this.validatedCommitCount = validatedCommitCount;
      this.changedEntriesByStore = Collections.unmodifiableMap(
          new LinkedHashMap<>(changedEntriesByStore));
      this.generationCreated = generationCreated;
      this.elapsedNanos = elapsedNanos;
    }

    private static ServingIndexApplyStatistics from(ServingIndexIncrementalPlan plan,
        boolean generationCreated, long elapsedNanos) {
      Map<String, Long> changed = new LinkedHashMap<>();
      plan.getChangesByDatabase().forEach((store, entries) ->
          changed.put(store, (long) entries.size()));
      return new ServingIndexApplyStatistics(plan.getIndexedFrom(), plan.getIndexedThrough(),
          plan.getSourceStepDigests().size(), changed, generationCreated,
          Math.max(0, elapsedNanos));
    }

    private static ServingIndexApplyStatistics zeroAction(BlockSnapshotMeta head) {
      Map<String, Long> changed = new LinkedHashMap<>();
      storeNames().forEach(store -> changed.put(store, 0L));
      return new ServingIndexApplyStatistics(head.getEpoch(), head.getEpoch(), 0, changed,
          false, 0);
    }

    public long getIndexedFrom() {
      return indexedFrom;
    }

    public long getIndexedThrough() {
      return indexedThrough;
    }

    public int getValidatedCommitCount() {
      return validatedCommitCount;
    }

    public Map<String, Long> getChangedEntriesByStore() {
      return changedEntriesByStore;
    }

    public boolean isGenerationCreated() {
      return generationCreated;
    }

    public long getElapsedNanos() {
      return elapsedNanos;
    }

    public long getChangedEntryCount() {
      return changedEntriesByStore.values().stream().mapToLong(Long::longValue).sum();
    }

    public boolean isThroughputAvailable() {
      return elapsedNanos > 0 && validatedCommitCount > 0;
    }

    public double getChangedEntriesPerSecond() {
      requireThroughput();
      return getChangedEntryCount() * 1_000_000_000.0 / elapsedNanos;
    }

    public double getValidatedCommitsPerSecond() {
      requireThroughput();
      return validatedCommitCount * 1_000_000_000.0 / elapsedNanos;
    }

    private void requireThroughput() {
      if (!isThroughputAvailable()) {
        throw new IllegalStateException("Serving apply throughput is unavailable");
      }
    }
  }

  /** Quiesces, detaches and closes owned resources without waiting for active query leases. */
  @Override
  public synchronized void close() throws IOException {
    if (state == State.CLOSED) {
      return;
    }
    if (state == State.FAILED_CLOSED) {
      throw terminalFailure;
    }
    if (state == State.RECOVERED) {
      IOException failure = closeParticipants();
      if (failure == null) {
        state = State.CLOSED;
        return;
      }
      terminalFailure = failure;
      state = State.FAILED_CLOSED;
      throw failure;
    }
    state = State.QUIESCING;
    if (queryGate != null) {
      queryGate.quiesce();
    }
    readableHead = null;
    if (!detached) {
      ArchiveRuntimeAttachment returned = snapshotManager.detachArchiveRuntime(attachment);
      if (returned != attachment) {
        throw new IllegalStateException("SnapshotManager returned a foreign archive attachment");
      }
      detached = true;
    }
    if (queryGate != null && !queryGate.isDrained()) {
      throw new IllegalStateException(
          "State Archive runtime still has active query leases: "
              + queryGate.getActiveLeaseCount());
    }
    if (queryGate != null) {
      queryGate.close();
    }

    IOException failure = null;
    if (latestCoordinator != null) {
      failure = closeOwned("latest coordinator", latestCoordinator, failure);
    }
    if (servingIndexBuilder != null) {
      failure = closeOwned("serving index runtime builder", servingIndexBuilder, failure);
    }
    if (servingCatalog != null) {
      failure = closeOwned("serving catalog", servingCatalog, failure);
    }
    for (int i = participants.size() - 1; i >= 0; i--) {
      failure = closeOwned("archive participant " + i, participants.get(i), failure);
    }
    failure = closeOwned("archive history sink", sink, failure);
    if (failure == null) {
      state = State.CLOSED;
      return;
    }
    terminalFailure = failure;
    state = State.FAILED_CLOSED;
    throw failure;
  }

  private IOException closeParticipants() {
    IOException failure = null;
    for (int i = participants.size() - 1; i >= 0; i--) {
      failure = closeOwned("archive participant " + i, participants.get(i), failure);
    }
    return failure;
  }

  private static void requireEmptyBootstrapTarget(Path root) throws IOException {
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    if (Files.isSymbolicLink(root)
        || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new ArchivePersistenceException("State Archive bootstrap target is not a directory");
    }
    try (java.util.stream.Stream<Path> entries = Files.list(root)) {
      if (entries.findAny().isPresent()) {
        throw new ArchivePersistenceException("State Archive bootstrap target is not empty");
      }
    }
  }

  private static List<String> storeNames() {
    List<String> names = new ArrayList<>(ArchiveStoreScope.getStateDatabases());
    Collections.sort(names);
    return names;
  }

  private static List<Closeable> immutableParticipants(
      List<? extends Closeable> participants) {
    List<? extends Closeable> source = Objects.requireNonNull(participants, "participants");
    List<Closeable> copy = new ArrayList<>(source.size());
    for (Closeable participant : source) {
      copy.add(Objects.requireNonNull(participant, "participant"));
    }
    return Collections.unmodifiableList(copy);
  }

  private void validateUniqueOwnership() {
    Set<Closeable> unique = Collections.newSetFromMap(new IdentityHashMap<Closeable, Boolean>());
    requireUnique(unique, latestCoordinator, "latestCoordinator");
    requireUnique(unique, servingCatalog, "servingCatalog");
    for (int i = 0; i < participants.size(); i++) {
      requireUnique(unique, participants.get(i), "participant[" + i + "]");
    }
    requireUnique(unique, sink, "sink");
  }

  private static void requireUnique(Set<Closeable> unique, Closeable resource, String name) {
    if (!unique.add(resource)) {
      throw new IllegalArgumentException("Archive runtime resource has multiple owners: " + name);
    }
  }

  private static IOException closeOwned(String name, Closeable resource, IOException current) {
    try {
      resource.close();
      return current;
    } catch (IOException failure) {
      return append(current, failure);
    } catch (RuntimeException failure) {
      return append(current, new IOException("Failed to close " + name, failure));
    }
  }

  private static IOException append(IOException current, IOException failure) {
    if (current == null) {
      return failure;
    }
    current.addSuppressed(failure);
    return current;
  }
}
