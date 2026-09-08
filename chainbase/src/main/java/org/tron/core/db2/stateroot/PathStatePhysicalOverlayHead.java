package org.tron.core.db2.stateroot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.core.CommonCheckpointBaseline;
import org.tron.core.db2.core.CommonCheckpointMemoryRebaser;
import org.tron.core.db2.core.CommonCheckpointMaterializedStore;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/**
 * PathState head that reads one durable physical checkpoint and advances in memory.
 *
 * <p>No transition method writes F/N/M, INTENT, CURRENT, or a reverse journal. The durable base is
 * changed only by the common-checkpoint materializer. The same implementation remains available
 * to the explicitly configured volatile benchmark mode.
 */
@Slf4j(topic = "DB")
public final class PathStatePhysicalOverlayHead implements PathStateHead {

  private static final byte[] ABSENT = new byte[0];
  static final int DEFAULT_PARTICIPANT_THREADS = 4;
  static final int DEFAULT_BRANCH_THREADS = 8;

  private final PathStatePhysicalStoreSet stores;
  private final PathStateParticipantScope scope;
  private final byte[] formatDigest;
  private final int maxHistory;
  private final ExecutorService participantExecutor;
  private final ExecutorService branchExecutor;
  private final List<HeadState> history = new ArrayList<>();
  private PathStateRootMetadata head;
  private PathStateRoot.Snapshot snapshot;
  private PreparedOverlay pending;
  private boolean failed;
  private boolean closed;

  private PathStatePhysicalOverlayHead(PathStatePhysicalStoreSet stores,
      PathStateRootMetadata head, PathStateRoot.Snapshot snapshot, int maxHistory,
      int participantThreads, int branchThreads) {
    this.stores = Objects.requireNonNull(stores, "stores");
    this.scope = stores.participantScope();
    this.formatDigest = stores.getFormatDigest();
    this.head = Objects.requireNonNull(head, "head");
    this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    this.maxHistory = maxHistory;
    participantExecutor = newExecutor(participantThreads, "participant");
    branchExecutor = newExecutor(branchThreads, "branch");
  }

  /** Opens the physical CURRENT as a read-only base for a volatile benchmark overlay. */
  public static PathStatePhysicalOverlayHead open(Path directory, Engine engine,
      PathStateLayerLimits limits) throws IOException {
    return open(directory, engine, limits, PathStatePhysicalStoreSet.STEADY_NODE_CACHE_BYTES);
  }

  /** Opens a next-format published target without invoking legacy journal/CURRENT recovery. */
  public static PathStatePhysicalOverlayHead openCommonCheckpoint(Path directory, Engine engine,
      PathStateLayerLimits limits, long residentNodeCacheBytes, int participantThreads,
      int branchThreads, byte[] formatIdentity, BlockSnapshotMeta canonicalHead, P66Phase phase)
      throws IOException {
    requireThreadCount(participantThreads, "participantThreads");
    requireThreadCount(branchThreads, "branchThreads");
    PathStatePhysicalStoreSet opened = PathStatePhysicalStoreSet.openExisting(directory,
        new PathStateCanonicalizer().participantScope(), engine, residentNodeCacheBytes);
    try {
      PathStateCheckpointMaterializer.PublishedHead current =
          PathStateCheckpointMaterializer.loadPublishedHead(directory, formatIdentity);
      PathStateRoot root = opened.createRoot();
      root.restoreStoredRoots(current.getStateRoot());
      PathStateRoot.Snapshot restored = root.snapshot();
      PathStateRootMetadata metadata = PathStateRootMetadata.base(current.getBlockNumber(),
          current.getBlockHash(), canonicalHead.getParentHash(), canonicalHead.getTimestamp(),
          phase, opened.getFormatDigest(), current.getStateRoot(), current.getPayloadDigest());
      return new PathStatePhysicalOverlayHead(opened, metadata, restored,
          Objects.requireNonNull(limits, "limits").getMaxLayers(), participantThreads,
          branchThreads);
    } catch (IOException | RuntimeException failure) {
      try {
        opened.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  /** Opens the immutable fresh baseline after its legacy pointer has been retired. */
  public static PathStatePhysicalOverlayHead openCommonBaseline(Path directory, Engine engine,
      PathStateLayerLimits limits, long residentNodeCacheBytes, int participantThreads,
      int branchThreads) throws IOException {
    requireThreadCount(participantThreads, "participantThreads");
    requireThreadCount(branchThreads, "branchThreads");
    PathStatePhysicalStoreSet opened = PathStatePhysicalStoreSet.openExisting(directory,
        new PathStateCanonicalizer().participantScope(), engine, residentNodeCacheBytes);
    try {
      PathStateRootMetadata current = PathStateRootMetadata.decode(Files.readAllBytes(
          directory.resolve(PathStateCheckpointMaterializer.COMMON_BASELINE_HEAD_FILE)));
      PathStateRoot root = opened.createRoot();
      root.restoreStoredRoots(current.getStateRoot());
      return new PathStatePhysicalOverlayHead(opened, current, root.snapshot(),
          Objects.requireNonNull(limits, "limits").getMaxLayers(), participantThreads,
          branchThreads);
    } catch (IOException | RuntimeException failure) {
      try {
        opened.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  /** Refreshes the in-memory head after startup redo has published a newer common target. */
  public synchronized void synchronizePublishedCheckpoint(byte[] formatIdentity,
      BlockSnapshotMeta canonicalHead, P66Phase phase) throws IOException {
    requireHealthy();
    PathStateCheckpointMaterializer.PublishedHead current =
        PathStateCheckpointMaterializer.loadPublishedHead(stores.getDirectory(), formatIdentity);
    if (current.getEpoch() != canonicalHead.getEpoch()
        || current.getBlockNumber() != canonicalHead.getBlockNumber()
        || !Arrays.equals(current.getBlockHash(), canonicalHead.getBlockHash())) {
      throw new IOException("PathState common CURRENT differs after startup redo");
    }
    PathStateRoot restored = new PathStateRoot(scope,
        participant -> stores.participant(participant.getDbName()).nodeStore(),
        stores.superStore().nodeStore());
    restored.restoreStoredRoots(current.getStateRoot());
    snapshot = restored.snapshot();
    head = PathStateRootMetadata.base(current.getBlockNumber(), current.getBlockHash(),
        canonicalHead.getParentHash(), canonicalHead.getTimestamp(), phase, formatDigest,
        current.getStateRoot(), current.getPayloadDigest());
    history.clear();
    pending = null;
  }

  /** Creates the PathState authority over the same stores used by this in-memory head. */
  public synchronized PathStateCheckpointMaterializer checkpointMaterializer(
      byte[] formatIdentity, CommonCheckpointBaseline baseline) throws IOException {
    return checkpointMaterializer(formatIdentity, baseline, null);
  }

  public synchronized PathStateCheckpointMaterializer checkpointMaterializer(
      byte[] formatIdentity, CommonCheckpointBaseline baseline,
      CommonCheckpointMaterializedStore materializedStore) throws IOException {
    requireHealthy();
    return new PathStateCheckpointMaterializer(stores, scope, formatIdentity, baseline,
        materializedStore);
  }

  /** Builds a fully validated parentless-target plus reversible-suffix memory rebase. */
  public synchronized CommonCheckpointMemoryRebaser.RebasePlan prepareCommonCheckpointRebase(
      CommonCheckpointTarget target) throws IOException {
    requireHealthy();
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    if (pending != null) {
      throw new IOException("path-state checkpoint cannot rebase a pending transition");
    }
    PathStateCheckpointMaterializer.PublishedHead published =
        PathStateCheckpointMaterializer.loadPublishedHead(stores.getDirectory(),
            admitted.getFormatIdentity());
    BlockSnapshotMeta targetBlock = admitted.getLastBlock();
    if (published.getEpoch() != targetBlock.getEpoch()
        || published.getBlockNumber() != targetBlock.getBlockNumber()
        || !Arrays.equals(published.getBlockHash(), targetBlock.getBlockHash())
        || !Arrays.equals(published.getStateRoot(), admitted.getStateRoot())
        || !Arrays.equals(published.getPayloadDigest(), admitted.getPayloadDigest())) {
      throw new IOException("PathState published CURRENT differs from common checkpoint target");
    }

    TargetPosition position = targetPosition(admitted);
    PathStateRoot baseline = new PathStateRoot(scope,
        participant -> stores.participant(participant.getDbName()).nodeStore(),
        stores.superStore().nodeStore());
    baseline.restoreStoredRoots(admitted.getStateRoot());
    PathStateRoot.Snapshot candidateSnapshot = baseline.snapshot();
    PathStateRootMetadata candidateHead = PathStateRootMetadata.base(
        targetBlock.getBlockNumber(), targetBlock.getBlockHash(), targetBlock.getParentHash(),
        targetBlock.getTimestamp(), position.metadata.getPhase(), formatDigest,
        admitted.getStateRoot(), admitted.getPayloadDigest());
    List<HeadState> candidateHistory = new ArrayList<>();
    BlockSnapshotMeta replayParent = targetBlock;

    for (int index = position.historyIndex; index < history.size(); index++) {
      HeadState retained = history.get(index);
      PathStateRootMetadata child = index + 1 < history.size()
          ? history.get(index + 1).metadata : head;
      validateReplayStep(candidateHead, replayParent, retained.deltaToChild, child);
      candidateHistory.add(new HeadState(candidateHead, candidateSnapshot,
          retained.deltaToChild));
      candidateSnapshot = replay(candidateSnapshot, retained.deltaToChild);
      candidateHead = copy(child);
      replayParent = retained.deltaToChild.getMeta();
    }
    if (!Arrays.equals(candidateSnapshot.getStateRoot(), head.getStateRoot())
        || candidateHead.getBlockNumber() != head.getBlockNumber()
        || !Arrays.equals(candidateHead.getBlockHash(), head.getBlockHash())) {
      throw new IOException("path-state checkpoint rebase candidate differs from live head");
    }

    PathStateRootMetadata installedHead = candidateHead;
    PathStateRoot.Snapshot installedSnapshot = candidateSnapshot;
    List<HeadState> installedHistory = new ArrayList<>(candidateHistory);
    return () -> installRebase(installedHead, installedSnapshot, installedHistory);
  }

  /** Retires this freshly rebuilt legacy pointer before the first common checkpoint is enabled. */
  public synchronized void admitFreshCommonBaseline(CommonCheckpointBaseline baseline)
      throws IOException {
    requireHealthy();
    PathStateCheckpointMaterializer.admitFreshBaseline(stores, head, baseline);
  }

  /** Opens a benchmark overlay with an explicit shared resident-node cache budget. */
  public static PathStatePhysicalOverlayHead open(Path directory, Engine engine,
      PathStateLayerLimits limits, long residentNodeCacheBytes) throws IOException {
    return open(directory, engine, limits, residentNodeCacheBytes,
        DEFAULT_PARTICIPANT_THREADS, DEFAULT_BRANCH_THREADS);
  }

  /** Opens a benchmark overlay with explicit cache and bounded prepare worker budgets. */
  public static PathStatePhysicalOverlayHead open(Path directory, Engine engine,
      PathStateLayerLimits limits, long residentNodeCacheBytes, int participantThreads,
      int branchThreads) throws IOException {
    requireThreadCount(participantThreads, "participantThreads");
    requireThreadCount(branchThreads, "branchThreads");
    PathStatePhysicalStoreSet opened = PathStatePhysicalStoreSet.openExisting(directory,
        new PathStateCanonicalizer().participantScope(), engine, residentNodeCacheBytes);
    try {
      opened.recoverPublication();
      PathStateRootMetadata current = opened.currentMetadata();
      PathStateRoot root = opened.createRoot();
      root.restoreStoredRoots(current.getStateRoot());
      PathStateRoot.Snapshot restored = root.snapshot();
      if (!Arrays.equals(restored.getStateRoot(), current.getStateRoot())) {
        throw new IOException("path-state overlay root mismatch");
      }
      return new PathStatePhysicalOverlayHead(opened, current, restored,
          Objects.requireNonNull(limits, "limits").getMaxLayers(), participantThreads,
          branchThreads);
    } catch (IOException | RuntimeException failure) {
      try {
        opened.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  @Override
  public synchronized PathStateRootMetadata advance(PathStateBlockTransition transition)
      throws IOException {
    requireHealthy();
    PathStateBlockTransition admitted = Objects.requireNonNull(transition, "transition");
    if (pending == null || pending.transition != admitted) {
      throw new IOException("path-state overlay publication differs from prepared transition");
    }
    history.add(new HeadState(head, snapshot, pending.delta));
    while (history.size() > maxHistory) {
      history.remove(0);
    }
    head = pending.metadata;
    snapshot = pending.snapshot;
    compactRetainedSnapshotsIfNeeded();
    logger.info("Path-state volatile overlay advanced: head={}, mutations={}, "
            + "nodeMutations={}, nativeNodeReads={}, cacheBytes={}, cacheEntries={}, "
            + "cacheEvictions={}, changedParticipants={}, maxParticipantMutations={}, "
            + "authoritativePreviousValues={}, "
            + "maxParticipantStoreId={}, maxParticipantMs={}, participantWorkMs={}, "
            + "participantWallMs={}, prepareMs={}, trieMs={}, artifactMs={}, "
            + "nodePlanWorkMs={}, nodeStoreWorkMs={}, nodeFinalizeWorkMs={}, "
            + "nodePuts={}, nodeDeletes={}, nodeRlpBytes={}, nodeRlpFinalBytes={}, "
            + "uniqueNodePaths={}, overwriteWrites={}, nodeCreates={}, nodeEncodes={}, "
            + "avoidedNodeEncodes={}, unencodedCreatesAfterTopLevelDecodes={}, nodeKeccaks={}, "
            + "nodeDecodes={}, nodeHashVerifies={}, hashRefsCreated={}, hashRefsResolved={}, "
            + "durableWrites=0, journal=0",
        head.getBlockNumber(), admitted.getMutations().size(), pending.nodeMutations,
        pending.nativeNodeReads, stores.residentNodeCacheBytes(),
        stores.residentNodeCacheEntries(), stores.residentNodeCacheEvictions(),
        pending.changedParticipants, pending.maxParticipantMutations,
        pending.authoritativePreviousValues,
        pending.maxParticipantStoreId, pending.maxParticipantMillis,
        pending.participantWorkMillis, pending.participantWallMillis,
        pending.prepareMillis, pending.trieMillis, pending.artifactMillis,
        pending.nodePlanWorkMillis, pending.nodeStoreWorkMillis,
        pending.nodeFinalizeWorkMillis,
        pending.stats.nodePuts, pending.stats.nodeDeletes, pending.stats.nodeRlpBytes,
        pending.stats.nodeRlpFinalBytes, pending.stats.uniqueNodePaths,
        pending.stats.overwriteWrites(), pending.nodeCreates, pending.nodeEncodes,
        Math.max(0L, pending.nodeCreates - pending.nodeEncodes),
        Math.max(0L, pending.nodeCreates - pending.nodeEncodes - pending.nodeDecodes),
        pending.nodeKeccaks,
        pending.nodeDecodes, pending.nodeHashVerifies, pending.hashRefsCreated,
        pending.hashRefsResolved);
    logger.info("Path-state artifact stores: head={}, perStore={}",
        head.getBlockNumber(), pending.stats.perStore);
    pending = null;
    return copy(head);
  }

  private void compactRetainedSnapshotsIfNeeded() throws IOException {
    int previousDepth = snapshot.maxTrieDepth();
    if (history.isEmpty() || previousDepth <= (long) maxHistory * 2) {
      return;
    }
    HeadState first = history.get(0);
    PathStateRootMetadata candidateHead = first.metadata;
    PathStateRoot.Snapshot candidateSnapshot = first.snapshot.detach();
    List<HeadState> candidateHistory = new ArrayList<>(history.size());
    BlockSnapshotMeta replayParent = BlockSnapshotMeta.forBlock(
        candidateHead.getBlockNumber(), candidateHead.getBlockHash(),
        candidateHead.getParentHash(), candidateHead.getTimestamp());
    for (int index = 0; index < history.size(); index++) {
      HeadState retained = history.get(index);
      PathStateRootMetadata child = index + 1 < history.size()
          ? history.get(index + 1).metadata : head;
      validateReplayStep(candidateHead, replayParent, retained.deltaToChild, child);
      candidateHistory.add(new HeadState(candidateHead, candidateSnapshot,
          retained.deltaToChild));
      PathStateRoot.Snapshot childSnapshot = index + 1 < history.size()
          ? history.get(index + 1).snapshot : snapshot;
      candidateSnapshot = childSnapshot.reparent(candidateSnapshot);
      candidateHead = copy(child);
      replayParent = retained.deltaToChild.getMeta();
    }
    if (!Arrays.equals(candidateSnapshot.getStateRoot(), snapshot.getStateRoot())
        || candidateHead.getBlockNumber() != head.getBlockNumber()
        || !Arrays.equals(candidateHead.getBlockHash(), head.getBlockHash())) {
      throw new IOException("path-state retained snapshot compaction differs from live head");
    }
    snapshot = candidateSnapshot;
    history.clear();
    history.addAll(candidateHistory);
    logger.info("Path-state volatile snapshot parents compacted: head={}, suffixBlocks={}, "
            + "previousDepth={}, retainedDepth={}", head.getBlockNumber(), history.size(),
        previousDepth, snapshot.maxTrieDepth());
  }

  @Override
  public synchronized PathStateRootMetadata rewindTo(long blockNumber, byte[] blockHash)
      throws IOException {
    requireHealthy();
    byte[] admittedHash = Arrays.copyOf(Objects.requireNonNull(blockHash, "blockHash"),
        blockHash.length);
    if (matches(head, blockNumber, admittedHash)) {
      pending = null;
      return copy(head);
    }
    for (int index = history.size() - 1; index >= 0; index--) {
      HeadState candidate = history.get(index);
      if (matches(candidate.metadata, blockNumber, admittedHash)) {
        head = candidate.metadata;
        snapshot = candidate.snapshot;
        history.subList(index, history.size()).clear();
        pending = null;
        return copy(head);
      }
    }
    throw new IOException("path-state overlay ancestor is outside memory history");
  }

  @Override
  public synchronized PathStateRootMetadata flushBaseThrough(long blockNumber, byte[] blockHash)
      throws IOException {
    requireHealthy();
    logger.info("Path-state volatile overlay skipped durable base flush: target={}, "
        + "benchmarkOnly=true", blockNumber);
    return copy(head);
  }

  @Override
  public synchronized byte[] preview(PathStateBlockTransition transition) throws IOException {
    requireHealthy();
    return prepare(null, Objects.requireNonNull(transition, "transition")).metadata.getStateRoot();
  }

  @Override
  public synchronized PathStateSnapshotDelta prepareSnapshotDelta(BlockSnapshotMeta meta,
      PathStateBlockTransition transition) throws IOException {
    requireHealthy();
    if (pending != null) {
      throw new IOException("path-state overlay transition is already prepared");
    }
    pending = prepare(Objects.requireNonNull(meta, "meta"),
        Objects.requireNonNull(transition, "transition"));
    return pending.delta;
  }

  @Override
  public synchronized PathStateRootMetadata getHead() throws IOException {
    requireHealthy();
    return copy(head);
  }

  long durableWriteBatchCalls() {
    long calls = stores.superStore().getWriteBatchCalls();
    for (PathStateParticipant participant : scope.getParticipants()) {
      calls += stores.participant(participant.getDbName()).getWriteBatchCalls();
    }
    return calls;
  }

  synchronized RetentionCensus retentionCensus() throws IOException {
    requireHealthy();
    return new RetentionCensus(head.getBlockNumber(), history.size(), snapshot.trieCount(),
        snapshot.maxTrieDepth());
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    participantExecutor.shutdownNow();
    branchExecutor.shutdownNow();
    stores.close();
  }

  private PreparedOverlay prepare(BlockSnapshotMeta meta, PathStateBlockTransition transition)
      throws IOException {
    requireChild(transition);
    long startedNanos = System.nanoTime();
    long nodeCreatesBefore = PathMerkleTrie.nodeCreateCountTotal();
    long nodeEncodesBefore = PathMerkleTrie.nodeEncodeCountTotal();
    long nodeKeccaksBefore = PathMerkleTrie.nodeKeccakCountTotal();
    Map<Integer, RecordingStore> recordings = new LinkedHashMap<>();
    PathStateRoot candidate = PathStateRoot.fromSnapshot(scope,
        participant -> recordings.computeIfAbsent(participant.getStoreId(), ignored ->
            new RecordingStore(stores.participant(participant.getDbName()).nodeStore())),
        recordings.computeIfAbsent(0, ignored ->
            new RecordingStore(stores.superStore().nodeStore())), snapshot);
    PathStateRoot.ParallelApplyStats parallelStats = transition.getMutations().isEmpty() ? null
        : candidate.applyParallel(transition.getMutations(), participantExecutor, branchExecutor);
    PathStateRoot.Snapshot nextSnapshot = candidate.snapshot();
    long trieNanos = System.nanoTime();
    Map<Integer, List<PathStateSnapshotDelta.Mutation>> flatByStore = new LinkedHashMap<>();
    for (PathStateMutation mutation : transition.getMutations()) {
      PathStateParticipant participant = scope.require(mutation.getDbName());
      byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(participant.getStoreId(),
          mutation.getPhysicalKey());
      byte[] encodedValue = mutation.isDelete() ? null
          : PathStateCommitmentCodec.presentLeafValue(mutation.getPhysicalValue());
      flatByStore.computeIfAbsent(participant.getStoreId(), ignored -> new ArrayList<>())
          .add(new PathStateSnapshotDelta.Mutation(secureKey, encodedValue));
    }

    List<PathStateSnapshotDelta.StoreDelta> deltas = new ArrayList<>();
    int nodeMutations = 0;
    for (PathStateParticipant participant : scope.getParticipants()) {
      List<PathStateSnapshotDelta.Mutation> flats = flatByStore.get(participant.getStoreId());
      if (flats == null) {
        continue;
      }
      List<PathStateSnapshotDelta.Mutation> nodes = recordings.get(participant.getStoreId())
          .mutations();
      nodeMutations += nodes.size();
      deltas.add(new PathStateSnapshotDelta.StoreDelta(participant,
          nextSnapshot.participantRoot(participant.getDbName()), flats, nodes));
    }
    List<PathStateSnapshotDelta.Mutation> superNodes = recordings.get(0).mutations();
    nodeMutations += superNodes.size();
    PathStateRootMetadata metadata = PathStateRootMetadata.layer(transition.getBlockNumber(),
        transition.getBlockHash(), transition.getParentHash(), transition.getTimestamp(),
        transition.getPhase(), formatDigest, head.getStateRoot(), nextSnapshot.getStateRoot(),
        transition.getPayloadDigest());
    PathStateSnapshotDelta delta = meta == null ? null : PathStateSnapshotDelta.fromPhysical(meta,
        head, transition, nextSnapshot, deltas, superNodes);
    long nativeReads = recordings.values().stream().mapToLong(RecordingStore::nativeReads).sum();
    RecordingStats stats = RecordingStats.collect(recordings);
    long finishedNanos = System.nanoTime();
    long prepareMillis = TimeUnit.NANOSECONDS.toMillis(finishedNanos - startedNanos);
    return new PreparedOverlay(transition, metadata, nextSnapshot, delta, nodeMutations,
        nativeReads, parallelStats, prepareMillis,
        TimeUnit.NANOSECONDS.toMillis(trieNanos - startedNanos),
        TimeUnit.NANOSECONDS.toMillis(finishedNanos - trieNanos),
        TimeUnit.NANOSECONDS.toMillis(candidate.nodeCommitPlanNanos()),
        TimeUnit.NANOSECONDS.toMillis(candidate.nodeCommitStoreNanos()),
        TimeUnit.NANOSECONDS.toMillis(candidate.nodeCommitFinalizeNanos()),
        PathMerkleTrie.nodeCreateCountTotal() - nodeCreatesBefore,
        PathMerkleTrie.nodeEncodeCountTotal() - nodeEncodesBefore,
        PathMerkleTrie.nodeKeccakCountTotal() - nodeKeccaksBefore,
        candidate.nodeDecodeCount(), candidate.nodeHashVerifyCount(),
        candidate.hashReferenceCreateCount(), candidate.hashReferenceResolveCount(), stats);
  }

  private TargetPosition targetPosition(CommonCheckpointTarget target) throws IOException {
    if (matches(head, target.getLastBlock().getBlockNumber(),
        target.getLastBlock().getBlockHash())
        && Arrays.equals(head.getStateRoot(), target.getStateRoot())) {
      return new TargetPosition(head, history.size());
    }
    for (int index = history.size() - 1; index >= 0; index--) {
      HeadState candidate = history.get(index);
      if (matches(candidate.metadata, target.getLastBlock().getBlockNumber(),
          target.getLastBlock().getBlockHash())
          && Arrays.equals(candidate.metadata.getStateRoot(), target.getStateRoot())) {
        return new TargetPosition(candidate.metadata, index);
      }
    }
    throw new IOException("PathState checkpoint target is outside reversible history");
  }

  private PathStateRoot.Snapshot replay(PathStateRoot.Snapshot parent,
      PathStateSnapshotDelta delta) throws IOException {
    Map<Integer, RecordingStore> recordings = new LinkedHashMap<>();
    PathStateRoot candidate = PathStateRoot.fromSnapshot(scope,
        participant -> recordings.computeIfAbsent(participant.getStoreId(), ignored ->
            new RecordingStore(stores.participant(participant.getDbName()).nodeStore())),
        recordings.computeIfAbsent(0, ignored ->
            new RecordingStore(stores.superStore().nodeStore())), parent);
    try {
      candidate.replaySnapshotDelta(delta);
      return candidate.snapshot();
    } catch (IllegalArgumentException | IllegalStateException failure) {
      throw new IOException("path-state checkpoint suffix replay failed", failure);
    }
  }

  private void validateReplayStep(PathStateRootMetadata parent, BlockSnapshotMeta parentBlock,
      PathStateSnapshotDelta delta, PathStateRootMetadata child) throws IOException {
    if (delta == null || child.getKind() != PathStateRootMetadata.Kind.LAYER
        || delta.getMeta().getEpoch() != parentBlock.getEpoch() + 1
        || delta.getMeta().getBlockNumber() != child.getBlockNumber()
        || !Arrays.equals(delta.getMeta().getBlockHash(), child.getBlockHash())
        || !Arrays.equals(delta.getMeta().getParentHash(), child.getParentHash())
        || delta.getMeta().getTimestamp() != child.getTimestamp()
        || child.getBlockNumber() != parent.getBlockNumber() + 1
        || !Arrays.equals(child.getParentHash(), parent.getBlockHash())
        || !Arrays.equals(delta.getParentStateRoot(), parent.getStateRoot())
        || !Arrays.equals(child.getParentStateRoot(), parent.getStateRoot())
        || !Arrays.equals(delta.getStateRoot(), child.getStateRoot())
        || !Arrays.equals(delta.getTransitionPayloadDigest(), child.getPayloadDigest())
        || !Arrays.equals(child.getFormatDigest(), formatDigest)) {
      throw new IOException("path-state checkpoint suffix identity mismatch");
    }
  }

  private synchronized void installRebase(PathStateRootMetadata installedHead,
      PathStateRoot.Snapshot installedSnapshot, List<HeadState> installedHistory) {
    head = installedHead;
    snapshot = installedSnapshot;
    history.clear();
    history.addAll(installedHistory);
  }

  private void requireChild(PathStateBlockTransition transition) throws IOException {
    if (transition.getBlockNumber() != head.getBlockNumber() + 1
        || !Arrays.equals(transition.getParentHash(), head.getBlockHash())) {
      throw new IOException("path-state benchmark transition does not extend volatile head");
    }
  }

  private void requireHealthy() throws IOException {
    if (closed) {
      throw new IOException("path-state overlay is closed");
    }
    if (failed) {
      throw new IOException("path-state overlay failed closed");
    }
  }

  private static ExecutorService newExecutor(int threads, String role) {
    return Executors.newFixedThreadPool(threads, task -> {
      Thread thread = new Thread(task, "path-state-overlay-" + role);
      thread.setDaemon(true);
      return thread;
    });
  }

  private static void requireThreadCount(int threads, String label) {
    if (threads <= 0 || threads > 64) {
      throw new IllegalArgumentException(label + " must be in [1, 64]");
    }
  }

  private static boolean matches(PathStateRootMetadata metadata, long blockNumber,
      byte[] blockHash) {
    return metadata.getBlockNumber() == blockNumber
        && Arrays.equals(metadata.getBlockHash(), blockHash);
  }

  private static PathStateRootMetadata copy(PathStateRootMetadata metadata) {
    return PathStateRootMetadata.decode(metadata.encode());
  }

  private static final class HeadState {

    private final PathStateRootMetadata metadata;
    private final PathStateRoot.Snapshot snapshot;
    private final PathStateSnapshotDelta deltaToChild;

    private HeadState(PathStateRootMetadata metadata, PathStateRoot.Snapshot snapshot,
        PathStateSnapshotDelta deltaToChild) {
      this.metadata = metadata;
      this.snapshot = snapshot;
      this.deltaToChild = deltaToChild;
    }
  }

  private static final class TargetPosition {

    private final PathStateRootMetadata metadata;
    private final int historyIndex;

    private TargetPosition(PathStateRootMetadata metadata, int historyIndex) {
      this.metadata = metadata;
      this.historyIndex = historyIndex;
    }
  }

  static final class RetentionCensus {

    private final long headBlockNumber;
    private final int suffixBlocks;
    private final int trieCount;
    private final int maxSnapshotDepth;

    private RetentionCensus(long headBlockNumber, int suffixBlocks, int trieCount,
        int maxSnapshotDepth) {
      this.headBlockNumber = headBlockNumber;
      this.suffixBlocks = suffixBlocks;
      this.trieCount = trieCount;
      this.maxSnapshotDepth = maxSnapshotDepth;
    }

    long getHeadBlockNumber() {
      return headBlockNumber;
    }

    int getSuffixBlocks() {
      return suffixBlocks;
    }

    int getTrieCount() {
      return trieCount;
    }

    int getMaxSnapshotDepth() {
      return maxSnapshotDepth;
    }
  }

  private static final class PreparedOverlay {

    private final PathStateBlockTransition transition;
    private final PathStateRootMetadata metadata;
    private final PathStateRoot.Snapshot snapshot;
    private final PathStateSnapshotDelta delta;
    private final int nodeMutations;
    private final long nativeNodeReads;
    private final int changedParticipants;
    private final int maxParticipantMutations;
    private final int authoritativePreviousValues;
    private final int maxParticipantStoreId;
    private final long maxParticipantMillis;
    private final long participantWorkMillis;
    private final long participantWallMillis;
    private final long prepareMillis;
    private final long trieMillis;
    private final long artifactMillis;
    private final long nodePlanWorkMillis;
    private final long nodeStoreWorkMillis;
    private final long nodeFinalizeWorkMillis;
    private final long nodeCreates;
    private final long nodeEncodes;
    private final long nodeKeccaks;
    private final long nodeDecodes;
    private final long nodeHashVerifies;
    private final long hashRefsCreated;
    private final long hashRefsResolved;
    private final RecordingStats stats;

    private PreparedOverlay(PathStateBlockTransition transition, PathStateRootMetadata metadata,
      PathStateRoot.Snapshot snapshot, PathStateSnapshotDelta delta, int nodeMutations,
        long nativeNodeReads, PathStateRoot.ParallelApplyStats parallelStats,
        long prepareMillis, long trieMillis, long artifactMillis, long nodePlanWorkMillis,
        long nodeStoreWorkMillis, long nodeFinalizeWorkMillis, long nodeCreates,
        long nodeEncodes, long nodeKeccaks, long nodeDecodes, long nodeHashVerifies,
        long hashRefsCreated,
        long hashRefsResolved, RecordingStats stats) {
      this.transition = transition;
      this.metadata = metadata;
      this.snapshot = snapshot;
      this.delta = delta;
      this.nodeMutations = nodeMutations;
      this.nativeNodeReads = nativeNodeReads;
      changedParticipants = parallelStats == null ? 0 : parallelStats.participantCount();
      maxParticipantMutations = parallelStats == null
          ? 0 : parallelStats.maxParticipantMutations();
      authoritativePreviousValues = parallelStats == null
          ? 0 : parallelStats.authoritativePreviousValues();
      maxParticipantStoreId = parallelStats == null
          ? 0 : parallelStats.maxParticipantStoreId();
      maxParticipantMillis = parallelStats == null ? 0 : parallelStats.maxParticipantMillis();
      participantWorkMillis = parallelStats == null ? 0 : parallelStats.participantWorkMillis();
      participantWallMillis = parallelStats == null ? 0 : parallelStats.wallMillis();
      this.prepareMillis = prepareMillis;
      this.trieMillis = trieMillis;
      this.artifactMillis = artifactMillis;
      this.nodePlanWorkMillis = nodePlanWorkMillis;
      this.nodeStoreWorkMillis = nodeStoreWorkMillis;
      this.nodeFinalizeWorkMillis = nodeFinalizeWorkMillis;
      this.nodeCreates = nodeCreates;
      this.nodeEncodes = nodeEncodes;
      this.nodeKeccaks = nodeKeccaks;
      this.nodeDecodes = nodeDecodes;
      this.nodeHashVerifies = nodeHashVerifies;
      this.hashRefsCreated = hashRefsCreated;
      this.hashRefsResolved = hashRefsResolved;
      this.stats = stats;
    }
  }

  /** Aggregated per-block node-artifact counters across all recording stores. */
  private static final class RecordingStats {

    private final long nodePuts;
    private final long nodeDeletes;
    private final long nodeRlpBytes;
    private final long nodeRlpFinalBytes;
    private final long uniqueNodePaths;
    private final String perStore;

    private RecordingStats(long nodePuts, long nodeDeletes, long nodeRlpBytes,
        long nodeRlpFinalBytes, long uniqueNodePaths, String perStore) {
      this.nodePuts = nodePuts;
      this.nodeDeletes = nodeDeletes;
      this.nodeRlpBytes = nodeRlpBytes;
      this.nodeRlpFinalBytes = nodeRlpFinalBytes;
      this.uniqueNodePaths = uniqueNodePaths;
      this.perStore = perStore;
    }

    private long overwriteWrites() {
      return nodePuts + nodeDeletes - uniqueNodePaths;
    }

    private static RecordingStats collect(Map<Integer, RecordingStore> recordings) {
      long puts = 0;
      long deletes = 0;
      long bytes = 0;
      long finalBytes = 0;
      long unique = 0;
      StringBuilder detail = new StringBuilder();
      for (Map.Entry<Integer, RecordingStore> entry : recordings.entrySet()) {
        RecordingStore store = entry.getValue();
        if (store.putCalls() + store.deleteCalls() == 0) {
          continue;
        }
        puts += store.putCalls();
        deletes += store.deleteCalls();
        bytes += store.putBytes();
        finalBytes += store.finalBytes();
        unique += store.uniquePaths();
        detail.append(entry.getKey()).append(':').append(store.putCalls()).append('/')
            .append(store.deleteCalls()).append('/').append(store.putBytes()).append('/')
            .append(store.uniquePaths()).append(';');
      }
      return new RecordingStats(puts, deletes, bytes, finalBytes, unique, detail.toString());
    }
  }

  private static final class RecordingStore implements PathNodeStore {

    private final PathNodeStore base;
    private final Map<BytesKey, byte[]> changes = new ConcurrentHashMap<>();
    private final Map<BytesKey, byte[]> reads = new ConcurrentHashMap<>();
    private final long initialNativeReads;
    private final AtomicLong directReads = new AtomicLong();
    private final AtomicLong putCalls = new AtomicLong();
    private final AtomicLong deleteCalls = new AtomicLong();
    private final AtomicLong putBytes = new AtomicLong();

    private RecordingStore(PathNodeStore base) {
      this.base = Objects.requireNonNull(base, "base");
      initialNativeReads = base instanceof PathStatePhysicalStoreSet.ResidentNodeStore
          ? ((PathStatePhysicalStoreSet.ResidentNodeStore) base).getNativeReads() : 0;
    }

    @Override
    public byte[] get(byte[] path) {
      BytesKey key = new BytesKey(path);
      byte[] changed = changes.get(key);
      if (changed != null) {
        return changed == ABSENT ? null : Arrays.copyOf(changed, changed.length);
      }
      byte[] value = reads.computeIfAbsent(key, ignored -> {
        directReads.incrementAndGet();
        byte[] loaded = base.get(path);
        return loaded == null ? ABSENT : Arrays.copyOf(loaded, loaded.length);
      });
      return value == ABSENT ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public void put(byte[] path, byte[] encodedNode) {
      byte[] present = Objects.requireNonNull(encodedNode, "encodedNode");
      putCalls.incrementAndGet();
      putBytes.addAndGet(present.length);
      changes.put(new BytesKey(path), Arrays.copyOf(present, present.length));
    }

    @Override
    public void delete(byte[] path) {
      deleteCalls.incrementAndGet();
      changes.put(new BytesKey(path), ABSENT);
    }

    private long putCalls() {
      return putCalls.get();
    }

    private long deleteCalls() {
      return deleteCalls.get();
    }

    private long putBytes() {
      return putBytes.get();
    }

    private int uniquePaths() {
      return changes.size();
    }

    private long finalBytes() {
      long bytes = 0;
      for (byte[] value : changes.values()) {
        if (value != ABSENT) {
          bytes += value.length;
        }
      }
      return bytes;
    }

    private List<PathStateSnapshotDelta.Mutation> mutations() {
      List<PathStateSnapshotDelta.Mutation> result = new ArrayList<>(changes.size());
      for (Map.Entry<BytesKey, byte[]> entry : changes.entrySet()) {
        byte[] value = entry.getValue();
        result.add(new PathStateSnapshotDelta.Mutation(entry.getKey().bytes,
            value == ABSENT ? null : value));
      }
      return result;
    }

    private long nativeReads() {
      return base instanceof PathStatePhysicalStoreSet.ResidentNodeStore
          ? ((PathStatePhysicalStoreSet.ResidentNodeStore) base).getNativeReads()
              - initialNativeReads : directReads.get();
    }

  }

  private static final class BytesKey {

    private final byte[] bytes;

    private BytesKey(byte[] bytes) {
      this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "path"), bytes.length);
    }

    @Override
    public boolean equals(Object other) {
      return this == other || other instanceof BytesKey
          && Arrays.equals(bytes, ((BytesKey) other).bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }
}
