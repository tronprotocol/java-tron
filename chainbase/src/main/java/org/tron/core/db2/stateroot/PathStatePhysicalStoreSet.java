package org.tron.core.db2.stateroot;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.config.args.StorageConfig.PathStateDbSettingsConfig;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/**
 * Fresh-format physical owner for the TASK-018 27 participant databases and one super database.
 *
 * <p>Each native database owns three disjoint domains: {@code F|secureKey -> encodedLeaf},
 * {@code N|path -> nodeRlp}, and {@code M|name -> metadata}. This class deliberately does not
 * provide an upgrade path from the old shared {@code base/nodes} layout.
 */
public final class PathStatePhysicalStoreSet implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger("DB");

  static final long DEFAULT_CHECKPOINT_ROWS = 1_000_000L;
  static final long DEFAULT_CHECKPOINT_BYTES = 256L * 1024 * 1024;
  static final int BOOTSTRAP_WRITE_BATCH_ENTRIES = 4096;
  static final long BOOTSTRAP_WRITE_BATCH_BYTES = 8L * 1024 * 1024;
  static final long STEADY_NODE_CACHE_BYTES = 256L * 1024 * 1024;
  private static final int MAX_PARALLEL_PARTICIPANT_WRITES = 4;
  private static final int MAX_PARALLEL_PARTICIPANT_PREPARES = 4;
  private static final int MAX_PARALLEL_TRIE_BRANCHES = 8;
  private static final Set<String> LARGE_BOOTSTRAP_STORES = java.util.Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "account", "account-asset", "delegation", "storage-row")));
  private static final Set<String> GIANT_NATIVE_STORES = java.util.Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList("account", "account-asset", "storage-row")));
  private static final Set<String> LARGE_NATIVE_STORES = java.util.Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList("code", "contract", "delegation")));

  private static final String STORES_DIRECTORY = "stores";
  private static final String SUPER_DIRECTORY = "super";
  private static final String NODES_DIRECTORY = "nodes";
  private static final String REVERSE_DIRECTORY = "reverse";
  static final String INTENT_FILE = "INTENT";
  static final String CURRENT_FILE = "CURRENT";
  // Stable persisted keyspace tags. The descriptive names carry semantics; byte values keep ABI.
  private static final byte FLAT_STATE_PREFIX = 'F';
  private static final byte TRIE_NODE_PREFIX = 'N';
  private static final byte METADATA_PREFIX = 'M';
  private static final byte[] FLAT_ROOT_METADATA = new byte[]{'f', 'l', 'a', 't', '-', 'r', 'o',
      'o', 't'};
  private static final byte[] FLAT_COMPLETE_METADATA = new byte[]{'f', 'l', 'a', 't', '-', 'c',
      'o', 'm', 'p', 'l', 'e', 't', 'e'};
  private static final byte[] FLAT_INGEST_CHECKPOINT = new byte[]{'f', 'l', 'a', 't', '-', 'i',
      'n', 'g', 'e', 's', 't'};
  private static final byte[] FLAT_INGEST_COMPLETE = new byte[]{'f', 'l', 'a', 't', '-', 'i', 'n',
      'g', 'e', 's', 't', '-', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e'};
  private static final byte[] FLAT_DIGEST_METADATA = new byte[]{'f', 'l', 'a', 't', '-', 'd', 'i',
      'g', 'e', 's', 't'};
  private static final byte[] STORE_GENERATION_METADATA = new byte[]{'s', 't', 'o', 'r', 'e', '-',
      'g', 'e', 'n', 'e', 'r', 'a', 't', 'i', 'o', 'n'};
  private static final byte[] SUPER_GENERATION_METADATA = new byte[]{'s', 'u', 'p', 'e', 'r', '-',
      'g', 'e', 'n', 'e', 'r', 'a', 't', 'i', 'o', 'n'};
  private static final byte[] CHECKPOINT_TARGET_METADATA = new byte[]{'c', 'h', 'e', 'c', 'k',
      'p', 'o', 'i', 'n', 't', '-', 't', 'a', 'r', 'g', 'e', 't', '-', 'v', '1'};

  private final Path directory;
  private final PathStatePhysicalStoreManifest manifest;
  private final PathStateParticipantScope scope;
  private final Map<String, PhysicalStore> participants = new LinkedHashMap<>();
  private final PhysicalStore superStore;
  private final ExecutorService participantWriteExecutor;
  private final ExecutorService participantPrepareExecutor;
  private final ExecutorService trieBranchExecutor;
  private final ResidentNodeCache residentNodeCache;
  private Map<BytesKey, ReverseJournalIndexEntry> reverseJournalIndex;
  private byte[] cachedTrieTarget;
  private PathStateRoot.Snapshot cachedTrieSnapshot;
  private boolean rootClaimed;
  private boolean closed;

  private PathStatePhysicalStoreSet(PathStatePhysicalStoreManifest manifest,
      PathStateParticipantScope scope, long residentNodeCacheBytes,
      PathStateDbSettingsConfig dbSettings)
      throws IOException {
    this.manifest = manifest;
    this.directory = manifest.getDirectory();
    this.scope = requireExactScope(scope);
    this.participantWriteExecutor = newParticipantWriteExecutor();
    this.participantPrepareExecutor = newTrieExecutor("participant-prepare",
        MAX_PARALLEL_PARTICIPANT_PREPARES);
    this.trieBranchExecutor = newTrieExecutor("branch-prepare", MAX_PARALLEL_TRIE_BRANCHES);
    this.residentNodeCache = new ResidentNodeCache(residentNodeCacheBytes);
    try {
      for (PathStateParticipant participant : scope.getParticipants()) {
        Path participantDirectory = directory.resolve(STORES_DIRECTORY).resolve(String.format(
            "%02d-%s", participant.getStoreId(), participant.getDbName())).resolve(NODES_DIRECTORY);
        participants.put(participant.getDbName(), new PhysicalStore(participantDirectory,
            manifest.getEngine(), participant.getStoreId(), residentNodeCache,
            storageProfileNameFor(participant.getDbName()),
            storageProfileFor(participant.getDbName(), dbSettings)));
      }
      superStore = new PhysicalStore(directory.resolve(SUPER_DIRECTORY).resolve(NODES_DIRECTORY),
          manifest.getEngine(), 0, residentNodeCache, "small", dbSettings.getSmall());
    } catch (IOException | RuntimeException failure) {
      closeAfterFailure(failure);
      throw failure;
    }
  }

  /** Creates or opens only the TASK-018 fresh physical layout. */
  public static PathStatePhysicalStoreSet open(Path directory, PathStateParticipantScope scope,
      Engine engine) throws IOException {
    Path root = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    if (Files.isSymbolicLink(root)) {
      throw new IOException("path-state physical root must not be a symbolic link: " + root);
    }
    rejectLegacySharedNodes(root);
    PathStatePhysicalStoreManifest manifest = PathStatePhysicalStoreManifest.createOrOpen(root,
        Objects.requireNonNull(engine, "engine"));
    return new PathStatePhysicalStoreSet(manifest, Objects.requireNonNull(scope, "scope"),
        STEADY_NODE_CACHE_BYTES, configuredDbSettings());
  }

  /** Opens only a fully materialized physical layout; missing child databases fail closed. */
  public static PathStatePhysicalStoreSet openExisting(Path directory,
      PathStateParticipantScope scope, Engine engine) throws IOException {
    return openExisting(directory, scope, engine, STEADY_NODE_CACHE_BYTES);
  }

  /** Opens a complete physical layout with an explicit shared resident-node cache budget. */
  public static PathStatePhysicalStoreSet openExisting(Path directory,
      PathStateParticipantScope scope, Engine engine, long residentNodeCacheBytes)
      throws IOException {
    Path root = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    rejectLegacySharedNodes(root);
    PathStatePhysicalStoreManifest manifest = PathStatePhysicalStoreManifest.validateExisting(
        root, Objects.requireNonNull(engine, "engine"));
    PathStateParticipantScope admittedScope = requireExactScope(scope);
    for (PathStateParticipant participant : admittedScope.getParticipants()) {
      requireStoreDirectory(root.resolve(STORES_DIRECTORY).resolve(String.format(
          "%02d-%s", participant.getStoreId(), participant.getDbName()))
          .resolve(NODES_DIRECTORY));
    }
    requireStoreDirectory(root.resolve(SUPER_DIRECTORY).resolve(NODES_DIRECTORY));
    return new PathStatePhysicalStoreSet(manifest, admittedScope, residentNodeCacheBytes,
        configuredDbSettings());
  }

  static String storageProfileNameFor(String dbName) {
    String supplied = Objects.requireNonNull(dbName, "dbName");
    if (GIANT_NATIVE_STORES.contains(supplied)) {
      return "giant";
    }
    if (LARGE_NATIVE_STORES.contains(supplied)) {
      return "large";
    }
    return "small";
  }

  private static NativeDbConfig storageProfileFor(String dbName,
      PathStateDbSettingsConfig settings) {
    String profile = storageProfileNameFor(dbName);
    if ("giant".equals(profile)) {
      return settings.getGiant();
    }
    if ("large".equals(profile)) {
      return settings.getLarge();
    }
    return settings.getSmall();
  }

  private static PathStateDbSettingsConfig configuredDbSettings() {
    org.tron.core.config.args.Storage storage = CommonParameter.getInstance().getStorage();
    PathStateDbSettingsConfig settings = storage == null ? null
        : storage.getPathStateRootDbSettings();
    return settings == null ? new PathStateDbSettingsConfig() : settings;
  }

  public synchronized PhysicalStore participant(String dbName) {
    requireOpen();
    PhysicalStore store = participants.get(Objects.requireNonNull(dbName, "dbName"));
    if (store == null) {
      throw new IllegalArgumentException("unknown path-state participant: " + dbName);
    }
    return store;
  }

  public synchronized PhysicalStore superStore() {
    requireOpen();
    return superStore;
  }

  /** Creates the one in-memory root owner backed by this set's physically separate node stores. */
  public synchronized PathStateRoot createRoot() {
    requireOpen();
    if (rootClaimed) {
      throw new IllegalStateException("path-state physical store set already has a root owner");
    }
    rootClaimed = true;
    return new PathStateRoot(scope, participant -> participant(participant.getDbName()).nodeStore(),
        superStore.nodeStore());
  }

  public Path getDirectory() {
    return directory;
  }

  public byte[] getFormatDigest() {
    return manifest.getIdentityDigest();
  }

  PathStateParticipantScope participantScope() {
    return scope;
  }

  long residentNodeCacheBytes() {
    return residentNodeCache.bytes();
  }

  int residentNodeCacheEntries() {
    return residentNodeCache.size();
  }

  long residentNodeCacheEvictions() {
    return residentNodeCache.evictions();
  }

  synchronized void saveIngestCheckpoint(String dbName, PathStatePhysicalIngestCheckpoint value) {
    participant(dbName).putMetadata(FLAT_INGEST_CHECKPOINT,
        Objects.requireNonNull(value, "value").encode());
  }

  synchronized PathStatePhysicalIngestCheckpoint ingestCheckpoint(String dbName) {
    byte[] encoded = participant(dbName).getMetadata(FLAT_INGEST_CHECKPOINT);
    return encoded == null ? null : PathStatePhysicalIngestCheckpoint.decode(encoded);
  }

  /** Ingests exact physical rows into one F domain and durably advances its source cursor. */
  void ingestFlat(String dbName, PathStateRebuildCoordinator.SnapshotSource source,
      long rowThreshold, long byteThreshold) throws IOException {
    if (rowThreshold <= 0 || byteThreshold <= 0) {
      throw new IllegalArgumentException("ingest checkpoint thresholds must be positive");
    }
    PathStateParticipant participant = scope.require(dbName);
    PhysicalStore store = participants.get(dbName);
    if (store == null) {
      throw new IllegalArgumentException("unknown path-state participant: " + dbName);
    }
    PathStateRebuildCoordinator.SnapshotSource pinned = Objects.requireNonNull(source, "source");
    byte[] identity = pinned.sourceIdentityDigest();
    if (identity.length != PathStateCommitmentCodec.ROOT_LENGTH) {
      throw new IOException("physical ingest source identity must contain exactly 32 bytes");
    }
    byte[] complete = store.getMetadata(FLAT_INGEST_COMPLETE);
    if (complete != null) {
      if (!Arrays.equals(complete, identity)) {
        throw new IOException("physical ingest completion source identity differs: " + dbName);
      }
      return;
    }
    byte[] encodedCheckpoint = store.getMetadata(FLAT_INGEST_CHECKPOINT);
    PathStatePhysicalIngestCheckpoint prior = encodedCheckpoint == null ? null
        : PathStatePhysicalIngestCheckpoint.decode(encodedCheckpoint);
    if (prior != null && !Arrays.equals(prior.getSourceIdentity(), identity)) {
      throw new IOException("physical ingest checkpoint source identity differs: " + dbName);
    }
    long[] progress = prior == null ? new long[]{0, 0} : new long[]{prior.getRows(), prior.getBytes()};
    byte[][] cursor = new byte[][]{prior == null ? null : prior.getCursor()};
    List<PathStateNativeNodeStore.BatchMutation> pending =
        new ArrayList<>(BOOTSTRAP_WRITE_BATCH_ENTRIES + 2);
    long[] pendingBytes = new long[1];
    long[] sinceCheckpoint = new long[2];
    long startedNanos = System.nanoTime();
    long initialRows = progress[0];
    long initialBatchCalls = store.getWriteBatchCalls();
    long initialBatchMutations = store.getWriteBatchMutations();
    pinned.scanAfter(dbName, cursor[0], (physicalKey, physicalValue) -> {
      byte[] key = Arrays.copyOf(physicalKey, physicalKey.length);
      byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(participant.getStoreId(), key);
      byte[] encodedLeaf = PathStateCommitmentCodec.presentLeafValue(physicalValue);
      byte[] storedKey = prefixed(FLAT_STATE_PREFIX, secureKey, "secureKey");
      long mutationBytes = storedKey.length + encodedLeaf.length;
      if (!pending.isEmpty()
          && pendingBytes[0] + mutationBytes > BOOTSTRAP_WRITE_BATCH_BYTES) {
        store.writeBatch(pending);
        pending.clear();
        pendingBytes[0] = 0;
      }
      pending.add(PathStateNativeNodeStore.BatchMutation.put(storedKey, encodedLeaf));
      pendingBytes[0] = Math.addExact(pendingBytes[0], mutationBytes);
      cursor[0] = key;
      progress[0]++;
      progress[1] += key.length + physicalValue.length;
      sinceCheckpoint[0]++;
      sinceCheckpoint[1] += key.length + physicalValue.length;
      boolean checkpointDue = sinceCheckpoint[0] >= rowThreshold
          || sinceCheckpoint[1] >= byteThreshold;
      if (checkpointDue) {
        pending.add(PhysicalStore.metadataMutation(FLAT_INGEST_CHECKPOINT,
            new PathStatePhysicalIngestCheckpoint(identity, cursor[0], progress[0], progress[1])
                .encode()));
      }
      if (checkpointDue || pending.size() >= BOOTSTRAP_WRITE_BATCH_ENTRIES
          || pendingBytes[0] >= BOOTSTRAP_WRITE_BATCH_BYTES) {
        store.writeBatch(pending);
        pending.clear();
        pendingBytes[0] = 0;
      }
      if (checkpointDue) {
        logger.info("Path-state physical ingest checkpointed: storeId={}, dbName={}, rows={}, "
                + "inputBytes={}, batches={}, mutations={}, elapsedMs={}, rowsPerSecond={}",
            participant.getStoreId(), dbName, progress[0], progress[1],
            store.getWriteBatchCalls() - initialBatchCalls,
            store.getWriteBatchMutations() - initialBatchMutations,
            elapsedMillis(startedNanos), rowsPerSecond(progress[0] - initialRows, startedNanos));
        sinceCheckpoint[0] = 0;
        sinceCheckpoint[1] = 0;
      }
    });
    if (cursor[0] != null) {
      pending.add(PhysicalStore.metadataMutation(FLAT_INGEST_CHECKPOINT,
          new PathStatePhysicalIngestCheckpoint(identity, cursor[0], progress[0], progress[1])
              .encode()));
    }
    pending.add(PhysicalStore.metadataMutation(FLAT_INGEST_COMPLETE, identity));
    store.writeBatch(pending);
    logger.info("Path-state physical ingest completed: storeId={}, dbName={}, rows={}, "
            + "inputBytes={}, batches={}, mutations={}, elapsedMs={}, rowsPerSecond={}",
        participant.getStoreId(), dbName, progress[0], progress[1],
        store.getWriteBatchCalls() - initialBatchCalls,
        store.getWriteBatchMutations() - initialBatchMutations,
        elapsedMillis(startedNanos), rowsPerSecond(progress[0] - initialRows, startedNanos));
  }

  /** Validates one exact-27 pinned source, resumes all unfinished F ingests, then builds the root. */
  public synchronized PathStateRoot ingestAndBuild(
      PathStateRebuildCoordinator.SnapshotSource source)
      throws IOException {
    return ingestAndBuild(source, DEFAULT_CHECKPOINT_ROWS, DEFAULT_CHECKPOINT_BYTES);
  }

  /** Validates one exact-27 pinned source, resumes all unfinished F ingests, then builds the root. */
  synchronized PathStateRoot ingestAndBuild(PathStateRebuildCoordinator.SnapshotSource source,
      long rowThreshold, long byteThreshold) throws IOException {
    PathStateRebuildCoordinator.SnapshotSource pinned = Objects.requireNonNull(source, "source");
    PathStateRebuildCoordinator.SnapshotIdentity identity = Objects.requireNonNull(
        pinned.identity(), "source identity");
    pinned.verifyIdentity(identity);
    PathStateParticipantDescriptor.current().requireExactDatabases(pinned.databases());
    ingestFlatParticipants(pinned, rowThreshold, byteThreshold);
    pinned.verifyIdentity(identity);
    return buildRootFromFlat();
  }

  private void ingestFlatParticipants(PathStateRebuildCoordinator.SnapshotSource source,
      long rowThreshold, long byteThreshold) throws IOException {
    ExecutorService largeExecutor = newBootstrapExecutor("large");
    ExecutorService smallExecutor = newBootstrapExecutor("small");
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (PathStateParticipant participant : scope.getParticipants()) {
        ExecutorService executor = LARGE_BOOTSTRAP_STORES.contains(participant.getDbName())
            ? largeExecutor : smallExecutor;
        futures.add(executor.submit(() -> {
          ingestFlat(participant.getDbName(), source, rowThreshold, byteThreshold);
          return null;
        }));
      }
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          cancelOutstanding(futures);
          throw new IOException("path-state physical ingest interrupted", interrupted);
        } catch (ExecutionException failed) {
          cancelOutstanding(futures);
          Throwable cause = failed.getCause();
          if (cause instanceof IOException) {
            throw (IOException) cause;
          }
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }
          throw new IOException("path-state physical ingest failed", cause);
        }
      }
    } finally {
      largeExecutor.shutdownNow();
      smallExecutor.shutdownNow();
    }
  }

  private static ExecutorService newBootstrapExecutor(String tier) {
    return Executors.newSingleThreadExecutor(task -> {
      Thread thread = new Thread(task, "path-state-physical-bootstrap-" + tier);
      thread.setDaemon(true);
      return thread;
    });
  }

  private static ExecutorService newParticipantWriteExecutor() {
    return Executors.newFixedThreadPool(MAX_PARALLEL_PARTICIPANT_WRITES, task -> {
      Thread thread = new Thread(task, "path-state-physical-participant-write");
      thread.setDaemon(true);
      return thread;
    });
  }

  private static ExecutorService newTrieExecutor(String name, int threads) {
    return Executors.newFixedThreadPool(threads, task -> {
      Thread thread = new Thread(task, "path-state-physical-" + name);
      thread.setDaemon(true);
      return thread;
    });
  }

  private static void cancelOutstanding(List<Future<?>> futures) {
    for (Future<?> future : futures) {
      if (!future.isDone()) {
        future.cancel(true);
      }
    }
  }

  /**
   * Persists a complete local F-domain snapshot and its root marker.
   *
   * <p>This is intentionally not a 28-database global publication protocol. Callers must add the
   * TASK-018 global intent before treating this marker as a runtime CURRENT authority.
   */
  public synchronized void persistFlatSnapshot(PathStateRoot root) {
    requireOpen();
    PathStateRoot supplied = Objects.requireNonNull(root, "root");
    byte[] stateRoot = supplied.rootHash();
    for (PathStateRoot.LeafRecord record : supplied.leafRecords()) {
      PathStateParticipant participant = participant(record.getStoreId());
      participant(participant.getDbName()).putFlat(record.getSecureKey(),
          record.getEncodedValue());
    }
    superStore.putMetadata(FLAT_ROOT_METADATA, stateRoot);
  }

  /** Restores one root from every participant F domain and verifies the stored local root marker. */
  public synchronized PathStateRoot restoreRootFromFlat() throws IOException {
    requireOpen();
    byte[] expectedRoot = superStore.getMetadata(FLAT_ROOT_METADATA);
    if (expectedRoot == null || expectedRoot.length != PathStateCommitmentCodec.ROOT_LENGTH) {
      throw new IllegalStateException("path-state physical F root marker is missing or invalid");
    }
    PathStateRoot root = createRoot();
    List<PathStateRoot.LeafRecord> records = new ArrayList<>();
    for (PathStateParticipant participant : scope.getParticipants()) {
      PhysicalStore store = participant(participant.getDbName());
      store.scanFlat(entry -> records.add(new PathStateRoot.LeafRecord(participant.getStoreId(),
          unprefixedFlatKey(entry.getKey()), entry.getValue())));
    }
    root.restoreLeaves(records, expectedRoot);
    return root;
  }

  /**
   * Streams each participant F domain in secure-key order into its N domain and marks completion.
   *
   * <p>A valid per-Store completion marker skips that Store on retry. This method has no source
   * database parameter and therefore cannot trigger a source rescan.
   */
  public synchronized PathStateRoot buildRootFromFlat() throws IOException {
    return buildRootFromFlat((participant, storeRoot) -> { });
  }

  synchronized PathStateRoot buildRootFromFlat(BuildFaultHook faultHook) throws IOException {
    requireOpen();
    BuildFaultHook hook = Objects.requireNonNull(faultHook, "faultHook");
    PathStateRoot root = createRoot();
    List<PathStatePhysicalGlobalIntent.ParticipantTarget> targets = new ArrayList<>();
    for (PathStateParticipant participant : scope.getParticipants()) {
      PhysicalStore store = participant(participant.getDbName());
      byte[] completedRoot = store.getMetadata(FLAT_COMPLETE_METADATA);
      if (completedRoot != null) {
        if (completedRoot.length != PathStateCommitmentCodec.ROOT_LENGTH) {
          throw new IllegalStateException("path-state physical FLAT_COMPLETE marker is invalid");
        }
        byte[] flatDigest = requireDigest(store.getMetadata(FLAT_DIGEST_METADATA),
            "path-state physical flat digest is missing or invalid");
        byte[] generation = requireDigest(store.getMetadata(STORE_GENERATION_METADATA),
            "path-state physical Store generation is missing or invalid");
        requireSame(generation, participantGeneration(participant, flatDigest, completedRoot),
            "path-state physical Store generation differs");
        targets.add(new PathStatePhysicalGlobalIntent.ParticipantTarget(participant.getStoreId(),
            generation, flatDigest, completedRoot));
        root.completeRebuildParticipant(participant.getDbName(), completedRoot);
        continue;
      }
      store.clearNodes();
      long startedNanos = System.nanoTime();
      long initialBatchCalls = store.getWriteBatchCalls();
      long initialBatchMutations = store.getWriteBatchMutations();
      long[] rows = new long[1];
      PhysicalNodeBatchWriter nodeWriter = store.nodeBatchWriter();
      PathStateStackTrie trie = new PathStateStackTrie(nodeWriter::put);
      Hasher flatHasher = Hashing.sha256().newHasher();
      store.scanFlat(entry -> {
        byte[] secureKey = unprefixedFlatKey(entry.getKey());
        byte[] encodedValue = entry.getValue();
        flatHasher.putInt(secureKey.length).putBytes(secureKey)
            .putInt(encodedValue.length).putBytes(encodedValue);
        trie.update(secureKey, encodedValue);
        rows[0]++;
      });
      byte[] storeRoot = trie.rootHash();
      nodeWriter.flush();
      byte[] flatDigest = flatHasher.hash().asBytes();
      byte[] generation = participantGeneration(participant, flatDigest, storeRoot);
      hook.beforeCompletion(participant, storeRoot);
      store.completeFlatBuild(flatDigest, generation, storeRoot);
      logger.info("Path-state physical trie completed: storeId={}, dbName={}, rows={}, "
              + "batches={}, mutations={}, elapsedMs={}, rowsPerSecond={}",
          participant.getStoreId(), participant.getDbName(), rows[0],
          store.getWriteBatchCalls() - initialBatchCalls,
          store.getWriteBatchMutations() - initialBatchMutations,
          elapsedMillis(startedNanos), rowsPerSecond(rows[0], startedNanos));
      targets.add(new PathStatePhysicalGlobalIntent.ParticipantTarget(participant.getStoreId(),
          generation, flatDigest, storeRoot));
      root.completeRebuildParticipant(participant.getDbName(), storeRoot);
    }
    byte[] stateRoot = root.rootHash();
    superStore.putMetadata(SUPER_GENERATION_METADATA,
        superGeneration(targets, stateRoot));
    superStore.putMetadata(FLAT_ROOT_METADATA, stateRoot);
    return root;
  }

  /** Publishes the exact prepared 27+1 target through INTENT, CURRENT, and intent retirement. */
  public synchronized byte[] publishCurrent() throws IOException {
    return publishCurrent(syntheticMetadata(publicationTargetRoot()), stage -> { });
  }

  public synchronized byte[] publishCurrent(PathStateRootMetadata metadata) throws IOException {
    return publishCurrent(metadata, stage -> { });
  }

  synchronized byte[] publishCurrent(PublicationFaultHook faultHook) throws IOException {
    return publishCurrent(syntheticMetadata(publicationTargetRoot()), faultHook);
  }

  synchronized byte[] publishCurrent(PathStateRootMetadata metadata,
      PublicationFaultHook faultHook) throws IOException {
    requireOpen();
    PublicationFaultHook hook = Objects.requireNonNull(faultHook, "faultHook");
    PathStatePhysicalGlobalIntent target = publicationTarget(
        Objects.requireNonNull(metadata, "metadata"));
    byte[] encoded = target.encode();
    Path intent = directory.resolve(INTENT_FILE);
    Path current = directory.resolve(CURRENT_FILE);
    PathStateMetadataFile.publishImmutableBytes(intent, encoded);
    hook.after(PublicationStage.AFTER_INTENT);
    PathStateMetadataFile.replaceCurrentBytes(current, encoded);
    hook.after(PublicationStage.AFTER_CURRENT);
    PathStateMetadataFile.deleteDurable(intent);
    hook.after(PublicationStage.AFTER_RETIRE);
    return target.getSuperRoot();
  }

  /** Returns the exact block-bound metadata bound into the validated physical CURRENT. */
  public synchronized PathStateRootMetadata currentMetadata() throws IOException {
    requireOpen();
    PathStatePhysicalGlobalIntent current = currentTarget();
    return current.getMetadata();
  }

  synchronized void verifyReverseJournals(PathStateLayerLimits limits) throws IOException {
    requireOpen();
    reverseJournalIndex = loadReverseJournalIndex(Objects.requireNonNull(limits, "limits"));
  }

  /**
   * Loads one exact canonical reverse window without changing physical storage authority.
   *
   * <p>This diagnostic entry requires an offline coherent checkpoint. It deliberately reloads and
   * validates every retained journal instead of trusting the steady-state in-memory index. Missing
   * ancestry fails rather than returning a shorter window.
   */
  synchronized PathStatePhysicalOracleWindow loadOracleWindow(int blockCount,
      PathStateLayerLimits limits) throws IOException {
    requireOpen();
    if (blockCount <= 0) {
      throw new IllegalArgumentException("physical oracle block count must be positive");
    }
    if (Files.exists(directory.resolve(INTENT_FILE), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("physical oracle requires a settled CURRENT without INTENT");
    }
    PathStatePhysicalGlobalIntent current = currentTarget();
    Map<BytesKey, ReverseJournalIndexEntry> indexed = loadReverseJournalIndex(
        Objects.requireNonNull(limits, "limits"));
    List<PathStatePhysicalReverseJournal> journals = new ArrayList<>(blockCount);
    PathStatePhysicalGlobalIntent cursor = current;
    for (int index = 0; index < blockCount; index++) {
      ReverseJournalIndexEntry entry = indexed.get(new BytesKey(cursor.encode()));
      if (entry == null) {
        throw new IOException("physical oracle fixed window ancestry is missing at height "
            + cursor.getMetadata().getBlockNumber());
      }
      PathStatePhysicalReverseJournal journal = loadReverseJournal(entry.path);
      if (!Arrays.equals(cursor.encode(), journal.getChildTarget())) {
        throw new IOException("physical oracle journal child differs from canonical cursor");
      }
      journals.add(journal);
      cursor = PathStatePhysicalGlobalIntent.decode(journal.getParentTarget());
    }
    return new PathStatePhysicalOracleWindow(current.encode(), journals);
  }

  /** Computes one exact child target without changing F/N/M, INTENT, or CURRENT. */
  public synchronized PathStateRootMetadata previewTransition(PathStateBlockTransition transition)
      throws IOException {
    requireOpen();
    recoverPublication();
    return prepareTransition(transition).target.getMetadata();
  }

  /** Prepares one physical transition and its Snapshot-owned forward delta without any write. */
  synchronized PreparedPhysicalTransition prepareSnapshotDelta(BlockSnapshotMeta meta,
      PathStateBlockTransition transition) throws IOException {
    requireOpen();
    if (Files.exists(directory.resolve(INTENT_FILE), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("physical path-state prepare requires a settled CURRENT");
    }
    TransitionPlan plan = prepareTransition(transition);
    return new PreparedPhysicalTransition(transition, plan,
        snapshotDelta(Objects.requireNonNull(meta, "meta"), transition, plan));
  }

  /** Materializes an unchanged, previously prepared physical transition exactly once. */
  synchronized PathStateRootMetadata applyAndPublish(PreparedPhysicalTransition prepared,
      PathStateLayerLimits limits) throws IOException {
    PreparedPhysicalTransition admitted = Objects.requireNonNull(prepared, "prepared");
    return applyAndPublishInternal(admitted.transition, limits, stage -> { }, true,
        admitted.plan);
  }

  /** Applies one block-final child to the physical 27+1 stores and publishes its CURRENT. */
  public synchronized PathStateRootMetadata applyAndPublish(PathStateBlockTransition transition)
      throws IOException {
    return applyAndPublishInternal(transition, PathStateLayerLimits.defaults(), stage -> { },
        true);
  }

  synchronized PathStateRootMetadata applyAndPublish(PathStateBlockTransition transition,
      PathStateLayerLimits limits) throws IOException {
    return applyAndPublishInternal(transition, limits, stage -> { }, true);
  }

  synchronized PathStateRootMetadata applyAndPublish(PathStateBlockTransition transition,
      TransitionFaultHook faultHook) throws IOException {
    return applyAndPublishInternal(transition, PathStateLayerLimits.defaults(), faultHook, false);
  }

  synchronized PathStateRootMetadata applyAndPublish(PathStateBlockTransition transition,
      PathStateLayerLimits limits, TransitionFaultHook faultHook) throws IOException {
    return applyAndPublishInternal(transition, limits, faultHook, false);
  }

  private PathStateRootMetadata applyAndPublishInternal(PathStateBlockTransition transition,
      PathStateLayerLimits limits, TransitionFaultHook faultHook, boolean parallelParticipants)
      throws IOException {
    return applyAndPublishInternal(transition, limits, faultHook, parallelParticipants, null);
  }

  private PathStateRootMetadata applyAndPublishInternal(PathStateBlockTransition transition,
      PathStateLayerLimits limits, TransitionFaultHook faultHook, boolean parallelParticipants,
      TransitionPlan preparedPlan) throws IOException {
    requireOpen();
    long startedNanos = System.nanoTime();
    recoverPublication();
    TransitionPlan plan = preparedPlan == null ? prepareTransition(transition) : preparedPlan;
    if (preparedPlan != null
        && (!Arrays.equals(currentTarget().encode(), plan.parentTarget)
        || preparedPlan.transition != transition)) {
      throw new IOException("prepared physical transition no longer extends CURRENT");
    }
    long preparedNanos = System.nanoTime();
    TransitionFaultHook hook = Objects.requireNonNull(faultHook, "faultHook");
    byte[] encoded = plan.target.encode();
    byte[] encodedJournal = plan.journal.encode();
    Path journal = reverseJournalPath(plan.target.getMetadata());
    pruneReverseJournals(currentTarget(), Objects.requireNonNull(limits, "limits"), journal,
        encodedJournal.length);
    PathStateMetadataFile.publishImmutableBytes(journal, encodedJournal);
    rememberReverseJournal(plan.journal, journal, encodedJournal.length);
    long journalNanos = System.nanoTime();
    hook.after(TransitionStage.AFTER_JOURNAL);
    Path intent = directory.resolve(INTENT_FILE);
    Path current = directory.resolve(CURRENT_FILE);
    PathStateMetadataFile.publishImmutableBytes(intent, encoded);
    hook.after(TransitionStage.AFTER_INTENT);
    long intentNanos = System.nanoTime();
    if (parallelParticipants) {
      applyParticipantTransitionsInParallel(plan.participants);
      for (int completed = 0; completed < plan.participants.size(); completed++) {
        hook.after(TransitionStage.AFTER_PARTICIPANT_BATCH);
      }
    } else {
      for (ParticipantTransition participant : plan.participants) {
        applyParticipantTransition(participant);
        hook.after(TransitionStage.AFTER_PARTICIPANT_BATCH);
      }
    }
    long participantsNanos = System.nanoTime();
    plan.superStore.applySuperTransition(plan.superNodeMutations,
        plan.target.getSuperGeneration(), plan.target.getSuperRoot());
    hook.after(TransitionStage.AFTER_SUPER_BATCH);
    PathStateMetadataFile.replaceCurrentBytes(current, encoded);
    hook.after(TransitionStage.AFTER_CURRENT);
    PathStateMetadataFile.deleteDurable(intent);
    hook.after(TransitionStage.AFTER_RETIRE);
    cacheTrieSnapshot(encoded, plan.trieSnapshot);
    long completedNanos = System.nanoTime();
    logger.info("Path-state physical transition completed: head={}, changedStores={}, "
            + "journalBytes={}, journalCount={}, journalWindowBytes={}, nodeReadMisses={}, "
            + "nodeReadHits={}, residentNodeHits={}, residentCleanHits={}, "
            + "residentUpdatedHits={}, nativeNodeReads={}, residentCacheBytes={}, "
            + "residentEvictions={}, nodeDecodes={}, nodeHashVerifies={}, "
            + "hashReferencesCreated={}, hashReferencesResolved={}, flatReverseReads={}, "
            + "flatReverseReused={}, prepareMs={}, "
            + "journalMs={}, intentMs={}, participantWaitMs={}, finalizeMs={}, totalMs={}",
        plan.target.getMetadata().getBlockNumber(), plan.participants.size(),
        encodedJournal.length, reverseJournalCount(), reverseJournalBytes(),
        plan.nodeReadMisses, plan.nodeReadHits, plan.residentNodeHits,
        plan.residentCleanHits, plan.residentUpdatedHits, plan.nativeNodeReads,
        residentNodeCache.bytes(), residentNodeCache.evictions() - plan.initialResidentEvictions,
        plan.nodeDecodes,
        plan.nodeHashVerifies,
        plan.hashReferencesCreated,
        plan.hashReferencesResolved,
        plan.flatReverseReads,
        plan.flatReverseReused,
        elapsedMillis(startedNanos, preparedNanos), elapsedMillis(preparedNanos, journalNanos),
        elapsedMillis(journalNanos, intentNanos), elapsedMillis(intentNanos, participantsNanos),
        elapsedMillis(participantsNanos, completedNanos),
        elapsedMillis(startedNanos, completedNanos));
    return plan.target.getMetadata();
  }

  /** Rewinds through validated direct-parent journals to one exact bounded ancestor. */
  public synchronized PathStateRootMetadata rewindTo(long blockNumber, byte[] blockHash,
      PathStateLayerLimits limits) throws IOException {
    return rewindTo(blockNumber, blockHash, limits, stage -> { });
  }

  synchronized PathStateRootMetadata rewindTo(long blockNumber, byte[] blockHash,
      PathStateLayerLimits limits, RewindFaultHook faultHook) throws IOException {
    requireOpen();
    recoverPublication();
    byte[] targetHash = Arrays.copyOf(Objects.requireNonNull(blockHash, "blockHash"),
        blockHash.length);
    if (targetHash.length != PathStateRootMetadata.DIGEST_LENGTH) {
      throw new IOException("physical rewind block hash must contain exactly 32 bytes");
    }
    List<PathStatePhysicalReverseJournal> chain = loadRewindChain(currentTarget(), blockNumber,
        targetHash, Objects.requireNonNull(limits, "limits"));
    RewindFaultHook hook = Objects.requireNonNull(faultHook, "faultHook");
    for (PathStatePhysicalReverseJournal journal : chain) {
      applyReverseJournal(journal, hook);
    }
    PathStateRootMetadata rewound = currentMetadata();
    if (rewound.getBlockNumber() != blockNumber
        || !Arrays.equals(rewound.getBlockHash(), targetHash)) {
      throw new IOException("physical rewind did not reach the requested ancestor");
    }
    return rewound;
  }

  private TransitionPlan prepareTransition(PathStateBlockTransition supplied) throws IOException {
    long initialResidentEvictions = residentNodeCache.evictions();
    PathStateBlockTransition transition = Objects.requireNonNull(supplied, "transition");
    PathStatePhysicalGlobalIntent current = currentTarget();
    PathStateRootMetadata parent = current.getMetadata();
    if (transition.getBlockNumber() != parent.getBlockNumber() + 1
        || !Arrays.equals(transition.getParentHash(), parent.getBlockHash())) {
      throw new IOException("physical path-state transition does not extend CURRENT");
    }

    Map<Integer, RecordingNodeStore> recordings = new LinkedHashMap<>();
    PathStateRoot.PathNodeStoreFactory storeFactory = participant -> recordings.computeIfAbsent(
        participant.getStoreId(), ignored ->
            new RecordingNodeStore(participant(participant.getDbName()).nodeStore()));
    RecordingNodeStore recordingSuper = recordings.computeIfAbsent(0, ignored ->
        new RecordingNodeStore(superStore.nodeStore()));
    PathStateRoot candidate;
    boolean reusedTrieSnapshot = cachedTrieSnapshot != null
        && Arrays.equals(cachedTrieTarget, current.encode());
    if (reusedTrieSnapshot) {
      candidate = PathStateRoot.fromSnapshot(scope, storeFactory, recordingSuper,
          cachedTrieSnapshot);
    } else {
      candidate = new PathStateRoot(scope, storeFactory, recordingSuper);
      candidate.restoreStoredRoots(current.getSuperRoot());
      requireParticipantRoots(candidate, current);
    }
    if (!transition.getMutations().isEmpty()) {
      candidate.applyParallel(transition.getMutations(), participantPrepareExecutor,
          trieBranchExecutor);
    }
    byte[] stateRoot = candidate.rootHash();
    PathStateRoot.Snapshot trieSnapshot = candidate.snapshot();

    Map<Integer, List<FlatMutation>> flatByStore = new LinkedHashMap<>();
    for (PathStateMutation mutation : transition.getMutations()) {
      PathStateParticipant participant = scope.require(mutation.getDbName());
      byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(participant.getStoreId(),
          mutation.getPhysicalKey());
      byte[] encodedValue = mutation.isDelete() ? null
          : PathStateCommitmentCodec.presentLeafValue(mutation.getPhysicalValue());
      byte[] previousEncodedValue = null;
      if (mutation.isPreviousValueKnown() && mutation.getPreviousPhysicalValue() != null) {
        previousEncodedValue = PathStateCommitmentCodec.presentLeafValue(
            mutation.getPreviousPhysicalValue());
      }
      flatByStore.computeIfAbsent(participant.getStoreId(), ignored -> new ArrayList<>())
          .add(new FlatMutation(secureKey, encodedValue, mutation.isPreviousValueKnown(),
              previousEncodedValue));
    }

    List<PathStatePhysicalGlobalIntent.ParticipantTarget> targets = new ArrayList<>();
    List<ParticipantTransition> participantTransitions = new ArrayList<>();
    List<PathStatePhysicalReverseJournal.StoreReverse> reverseStores = new ArrayList<>();
    long flatReverseReads = 0;
    long flatReverseReused = 0;
    for (PathStatePhysicalGlobalIntent.ParticipantTarget oldTarget
        : current.getParticipants()) {
      List<FlatMutation> flatMutations = flatByStore.get(oldTarget.getStoreId());
      if (flatMutations == null) {
        targets.add(oldTarget);
        continue;
      }
      PathStateParticipant participant = participant(oldTarget.getStoreId());
      byte[] storeRoot = candidate.participantRoot(participant.getDbName());
      byte[] flatDigest = nextFlatDigest(participant, oldTarget.getFlatDigest(), transition,
          flatMutations);
      byte[] generation = participantGeneration(participant, flatDigest, storeRoot);
      targets.add(new PathStatePhysicalGlobalIntent.ParticipantTarget(participant.getStoreId(),
          generation, flatDigest, storeRoot));
      participantTransitions.add(new ParticipantTransition(participant,
          participant(participant.getDbName()), flatMutations,
          recordings.get(participant.getStoreId()).mutations(), flatDigest, generation,
          storeRoot));
      List<PathStatePhysicalReverseJournal.Entry> reverseFlat = new ArrayList<>();
      for (FlatMutation mutation : flatMutations) {
        byte[] oldValue;
        if (mutation.previousValueKnown) {
          oldValue = mutation.previousEncodedValue;
          flatReverseReused++;
        } else {
          oldValue = participant(participant.getDbName()).getFlat(mutation.secureKey);
          flatReverseReads++;
        }
        reverseFlat.add(new PathStatePhysicalReverseJournal.Entry(mutation.secureKey,
            oldValue));
      }
      reverseStores.add(new PathStatePhysicalReverseJournal.StoreReverse(
          participant.getStoreId(), reverseFlat,
          recordings.get(participant.getStoreId()).reverseEntries()));
    }
    byte[] superGeneration = superGeneration(targets, stateRoot);
    PathStateRootMetadata metadata = PathStateRootMetadata.layer(transition.getBlockNumber(),
        transition.getBlockHash(), transition.getParentHash(), transition.getTimestamp(),
        transition.getPhase(), manifest.getIdentityDigest(), parent.getStateRoot(), stateRoot,
        transition.getPayloadDigest());
    PathStatePhysicalGlobalIntent target = new PathStatePhysicalGlobalIntent(
        manifest.getIdentityDigest(), metadata, targets, superGeneration, stateRoot);
    PathStatePhysicalReverseJournal journal = new PathStatePhysicalReverseJournal(target.encode(),
        current.encode(), reverseStores, recordings.get(0).reverseEntries());
    long nodeReadMisses = recordings.values().stream()
        .mapToLong(RecordingNodeStore::getBaseReadMisses).sum();
    long nodeReadHits = recordings.values().stream()
        .mapToLong(RecordingNodeStore::getBaseReadHits).sum();
    long residentNodeHits = recordings.values().stream()
        .mapToLong(RecordingNodeStore::getResidentReadHits).sum();
    long nativeNodeReads = recordings.values().stream()
        .mapToLong(RecordingNodeStore::getNativeReads).sum();
    long residentCleanHits = recordings.values().stream()
        .mapToLong(RecordingNodeStore::getResidentCleanHits).sum();
    long residentUpdatedHits = recordings.values().stream()
        .mapToLong(RecordingNodeStore::getResidentUpdatedHits).sum();
    return new TransitionPlan(transition, current.encode(), target, participantTransitions,
        superStore,
        recordings.get(0).mutations(), journal, nodeReadMisses, nodeReadHits,
        residentNodeHits, residentCleanHits, residentUpdatedHits, nativeNodeReads,
        initialResidentEvictions, candidate.nodeDecodeCount(),
        candidate.nodeHashVerifyCount(), candidate.hashReferenceCreateCount(),
        candidate.hashReferenceResolveCount(), flatReverseReads, flatReverseReused,
        trieSnapshot, reusedTrieSnapshot);
  }

  private PathStateSnapshotDelta snapshotDelta(BlockSnapshotMeta meta,
      PathStateBlockTransition transition, TransitionPlan plan) {
    List<PathStateSnapshotDelta.StoreDelta> stores = new ArrayList<>();
    for (ParticipantTransition participant : plan.participants) {
      stores.add(new PathStateSnapshotDelta.StoreDelta(participant.participant,
          participant.storeRoot, snapshotMutations(participant.flatMutations),
          snapshotNodeMutations(participant.nodeMutations)));
    }
    return PathStateSnapshotDelta.fromPhysical(meta,
        PathStatePhysicalGlobalIntent.decode(plan.parentTarget).getMetadata(), transition,
        plan.trieSnapshot, stores, snapshotNodeMutations(plan.superNodeMutations));
  }

  private static List<PathStateSnapshotDelta.Mutation> snapshotMutations(
      List<FlatMutation> mutations) {
    List<PathStateSnapshotDelta.Mutation> result = new ArrayList<>();
    for (FlatMutation mutation : mutations) {
      result.add(new PathStateSnapshotDelta.Mutation(mutation.secureKey,
          mutation.encodedValue));
    }
    return result;
  }

  private static List<PathStateSnapshotDelta.Mutation> snapshotNodeMutations(
      List<NodeMutation> mutations) {
    List<PathStateSnapshotDelta.Mutation> result = new ArrayList<>();
    for (NodeMutation mutation : mutations) {
      result.add(new PathStateSnapshotDelta.Mutation(mutation.path, mutation.encodedNode));
    }
    return result;
  }

  private void cacheTrieSnapshot(byte[] target, PathStateRoot.Snapshot snapshot) {
    cachedTrieTarget = Arrays.copyOf(target, target.length);
    cachedTrieSnapshot = Objects.requireNonNull(snapshot, "snapshot");
  }

  private void applyParticipantTransitionsInParallel(List<ParticipantTransition> transitions)
      throws IOException {
    List<Runnable> writes = new ArrayList<>();
    for (ParticipantTransition participant : transitions) {
      writes.add(() -> applyParticipantTransition(participant));
    }
    awaitParallelWrites(participantWriteExecutor, writes);
  }

  static void awaitParallelWrites(ExecutorService executor, List<Runnable> writes)
      throws IOException {
    List<Future<?>> futures = new ArrayList<>();
    for (Runnable write : Objects.requireNonNull(writes, "writes")) {
      futures.add(Objects.requireNonNull(executor, "executor").submit(
          Objects.requireNonNull(write, "write")));
    }
    IOException failure = null;
    boolean interrupted = false;
    for (Future<?> future : futures) {
      boolean complete = false;
      while (!complete) {
        try {
          future.get();
          complete = true;
        } catch (InterruptedException interruptedFailure) {
          interrupted = true;
        } catch (ExecutionException writeFailure) {
          complete = true;
          Throwable cause = writeFailure.getCause();
          IOException participantFailure = cause instanceof IOException
              ? (IOException) cause
              : new IOException("path-state participant batch failed", cause);
          failure = append(failure, participantFailure);
        }
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
      failure = append(failure,
          new IOException("path-state participant batch wait was interrupted"));
    }
    if (failure != null) {
      throw failure;
    }
  }

  private static void applyParticipantTransition(ParticipantTransition participant) {
    participant.store.applyParticipantTransition(participant.flatMutations,
        participant.nodeMutations, participant.flatDigest, participant.generation,
        participant.storeRoot);
  }

  private void applyReverseJournal(PathStatePhysicalReverseJournal journal,
      RewindFaultHook hook) throws IOException {
    PathStatePhysicalGlobalIntent child = PathStatePhysicalGlobalIntent.decode(
        journal.getChildTarget());
    PathStatePhysicalGlobalIntent parent = PathStatePhysicalGlobalIntent.decode(
        journal.getParentTarget());
    if (!Arrays.equals(currentTarget().encode(), child.encode())) {
      throw new IOException("physical reverse journal child differs from CURRENT");
    }
    Path intent = directory.resolve(INTENT_FILE);
    Path current = directory.resolve(CURRENT_FILE);
    PathStateMetadataFile.publishImmutableBytes(intent, parent.encode());
    hook.after(RewindStage.AFTER_INTENT);
    for (PathStatePhysicalReverseJournal.StoreReverse reverse : journal.getStores()) {
      PathStatePhysicalGlobalIntent.ParticipantTarget target = participantTarget(parent,
          reverse.getStoreId());
      PhysicalStore store = participant(participant(reverse.getStoreId()).getDbName());
      store.applyParticipantTransition(flatMutations(reverse.getFlatEntries()),
          nodeMutations(reverse.getNodeEntries()), target.getFlatDigest(),
          target.getGeneration(), target.getStoreRoot());
      hook.after(RewindStage.AFTER_PARTICIPANT_BATCH);
    }
    superStore.applySuperTransition(nodeMutations(journal.getSuperNodes()),
        parent.getSuperGeneration(), parent.getSuperRoot());
    hook.after(RewindStage.AFTER_SUPER_BATCH);
    PathStateMetadataFile.replaceCurrentBytes(current, parent.encode());
    hook.after(RewindStage.AFTER_CURRENT);
    PathStateMetadataFile.deleteDurable(intent);
    cachedTrieTarget = null;
    cachedTrieSnapshot = null;
    hook.after(RewindStage.AFTER_RETIRE);
  }

  private List<PathStatePhysicalReverseJournal> loadRewindChain(
      PathStatePhysicalGlobalIntent current, long targetNumber, byte[] targetHash,
      PathStateLayerLimits limits) throws IOException {
    if (targetNumber < 0 || targetNumber > current.getMetadata().getBlockNumber()) {
      throw new IOException("physical rewind target height is outside CURRENT ancestry");
    }
    Map<BytesKey, ReverseJournalIndexEntry> journals = reverseJournalIndex(limits);
    List<PathStatePhysicalReverseJournal> chain = new ArrayList<>();
    PathStatePhysicalGlobalIntent cursor = current;
    while (cursor.getMetadata().getBlockNumber() > targetNumber) {
      ReverseJournalIndexEntry entry = journals.get(new BytesKey(cursor.encode()));
      if (entry == null) {
        throw new IOException("physical reverse journal ancestry is missing");
      }
      PathStatePhysicalReverseJournal journal = loadReverseJournal(entry.path);
      chain.add(journal);
      cursor = PathStatePhysicalGlobalIntent.decode(entry.parentTarget);
    }
    if (cursor.getMetadata().getBlockNumber() != targetNumber
        || !Arrays.equals(cursor.getMetadata().getBlockHash(), targetHash)) {
      throw new IOException("physical rewind target is not an exact ancestor");
    }
    return chain;
  }

  private Map<BytesKey, ReverseJournalIndexEntry> loadReverseJournalIndex(
      PathStateLayerLimits limits) throws IOException {
    Map<BytesKey, ReverseJournalIndexEntry> journals = new LinkedHashMap<>();
    Path reverse = directory.resolve(REVERSE_DIRECTORY);
    if (!Files.exists(reverse, LinkOption.NOFOLLOW_LINKS)) {
      return journals;
    }
    if (!Files.isDirectory(reverse, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(reverse)) {
      throw new IOException("physical reverse journal root is not a direct directory");
    }
    long total = 0;
    int count = 0;
    try (Stream<Path> files = Files.list(reverse)) {
      for (Path file : (Iterable<Path>) files::iterator) {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
          throw new IOException("physical reverse journal is not a regular file: " + file);
        }
        long length = Files.size(file);
        total = Math.addExact(total, length);
        count = Math.addExact(count, 1);
        PathStatePhysicalReverseJournal journal = loadReverseJournal(file);
        ReverseJournalIndexEntry entry = new ReverseJournalIndexEntry(file, length,
            journal.getChildTarget(), journal.getParentTarget());
        if (journals.put(new BytesKey(entry.childTarget), entry) != null) {
          throw new IOException("physical reverse journal child identity is duplicated");
        }
      }
    } catch (ArithmeticException overflow) {
      throw new IOException("physical reverse journal usage overflow", overflow);
    }
    if (count > limits.getMaxLayers() || total > limits.getMaxLogicalBytes()) {
      throw new IOException("physical reverse journal exceeds configured bounds");
    }
    return journals;
  }

  private PathStatePhysicalReverseJournal loadReverseJournal(Path file) throws IOException {
    try {
      return PathStatePhysicalReverseJournal.decode(
          PathStateMetadataFile.loadImmutableBytes(file,
              PathStatePhysicalReverseJournal.MAX_ENCODED_LENGTH));
    } catch (IllegalArgumentException invalid) {
      throw new IOException("physical reverse journal is corrupt: " + file, invalid);
    }
  }

  private Map<BytesKey, ReverseJournalIndexEntry> reverseJournalIndex(
      PathStateLayerLimits limits) throws IOException {
    if (reverseJournalIndex == null) {
      reverseJournalIndex = loadReverseJournalIndex(limits);
    } else {
      requireReverseJournalLimits(reverseJournalIndex, limits);
    }
    return reverseJournalIndex;
  }

  private static void requireReverseJournalLimits(
      Map<BytesKey, ReverseJournalIndexEntry> journals, PathStateLayerLimits limits)
      throws IOException {
    long total = 0;
    try {
      for (ReverseJournalIndexEntry entry : journals.values()) {
        total = Math.addExact(total, entry.length);
      }
    } catch (ArithmeticException overflow) {
      throw new IOException("physical reverse journal usage overflow", overflow);
    }
    if (journals.size() > limits.getMaxLayers() || total > limits.getMaxLogicalBytes()) {
      throw new IOException("physical reverse journal exceeds configured bounds");
    }
  }

  private void rememberReverseJournal(PathStatePhysicalReverseJournal journal, Path path,
      long length) throws IOException {
    Map<BytesKey, ReverseJournalIndexEntry> journals = reverseJournalIndex(
        new PathStateLayerLimits(Integer.MAX_VALUE, Long.MAX_VALUE));
    ReverseJournalIndexEntry entry = new ReverseJournalIndexEntry(path, length,
        journal.getChildTarget(), journal.getParentTarget());
    BytesKey identity = new BytesKey(entry.childTarget);
    ReverseJournalIndexEntry previous = journals.put(identity, entry);
    if (previous != null && !previous.path.equals(entry.path)) {
      journals.put(identity, previous);
      throw new IOException("physical reverse journal child identity is duplicated");
    }
  }

  private int reverseJournalCount() {
    return reverseJournalIndex == null ? 0 : reverseJournalIndex.size();
  }

  private long reverseJournalBytes() throws IOException {
    if (reverseJournalIndex == null) {
      return 0;
    }
    long total = 0;
    try {
      for (ReverseJournalIndexEntry entry : reverseJournalIndex.values()) {
        total = Math.addExact(total, entry.length);
      }
      return total;
    } catch (ArithmeticException overflow) {
      throw new IOException("physical reverse journal usage overflow", overflow);
    }
  }

  private void pruneReverseJournals(PathStatePhysicalGlobalIntent current,
      PathStateLayerLimits limits, Path candidate, long candidateBytes) throws IOException {
    if (candidateBytes > limits.getMaxLogicalBytes()) {
      throw new IOException("physical reverse journal candidate exceeds byte limit");
    }
    Path reverse = directory.resolve(REVERSE_DIRECTORY);
    if (!Files.exists(reverse, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    Map<BytesKey, ReverseJournalIndexEntry> indexed = reverseJournalIndex(
        new PathStateLayerLimits(Integer.MAX_VALUE, Long.MAX_VALUE));
    Set<BytesKey> keep = new HashSet<>();
    byte[] cursor = current.encode();
    int remainingCount = limits.getMaxLayers() - 1;
    long remainingBytes = limits.getMaxLogicalBytes() - candidateBytes;
    while (remainingCount > 0) {
      BytesKey identity = new BytesKey(cursor);
      ReverseJournalIndexEntry entry = indexed.get(identity);
      if (entry == null) {
        break;
      }
      if (entry.length > remainingBytes) {
        break;
      }
      keep.add(identity);
      remainingCount--;
      remainingBytes -= entry.length;
      cursor = entry.parentTarget;
    }
    java.util.Iterator<Map.Entry<BytesKey, ReverseJournalIndexEntry>> iterator =
        indexed.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<BytesKey, ReverseJournalIndexEntry> indexedJournal = iterator.next();
      ReverseJournalIndexEntry entry = indexedJournal.getValue();
      if (!entry.path.equals(candidate) && !keep.contains(indexedJournal.getKey())) {
        PathStateMetadataFile.deleteDurable(entry.path);
        iterator.remove();
      }
    }
  }

  private Path reverseJournalPath(PathStateRootMetadata child) {
    StringBuilder hash = new StringBuilder(child.getBlockHash().length * 2);
    for (byte value : child.getBlockHash()) {
      hash.append(String.format("%02x", value & 0xff));
    }
    return directory.resolve(REVERSE_DIRECTORY).resolve(String.format("%020d-%s.journal",
        child.getBlockNumber(), hash));
  }

  private static PathStatePhysicalGlobalIntent.ParticipantTarget participantTarget(
      PathStatePhysicalGlobalIntent target, int storeId) throws IOException {
    for (PathStatePhysicalGlobalIntent.ParticipantTarget participant : target.getParticipants()) {
      if (participant.getStoreId() == storeId) {
        return participant;
      }
    }
    throw new IOException("physical reverse target Store is absent");
  }

  private static List<FlatMutation> flatMutations(
      List<PathStatePhysicalReverseJournal.Entry> entries) {
    List<FlatMutation> mutations = new ArrayList<>();
    for (PathStatePhysicalReverseJournal.Entry entry : entries) {
      mutations.add(new FlatMutation(entry.getKey(), entry.getOldValue()));
    }
    return mutations;
  }

  private static List<NodeMutation> nodeMutations(
      List<PathStatePhysicalReverseJournal.Entry> entries) {
    List<NodeMutation> mutations = new ArrayList<>();
    for (PathStatePhysicalReverseJournal.Entry entry : entries) {
      mutations.add(new NodeMutation(entry.getKey(), entry.getOldValue()));
    }
    return mutations;
  }

  private byte[] nextFlatDigest(PathStateParticipant participant, byte[] previous,
      PathStateBlockTransition transition, List<FlatMutation> mutations) {
    Hasher hasher = Hashing.sha256().newHasher()
        .putString("java-tron/path-state/flat-transition/v1", StandardCharsets.US_ASCII)
        .putBytes(manifest.getIdentityDigest()).putInt(participant.getStoreId())
        .putBytes(previous).putBytes(transition.getPayloadDigest()).putInt(mutations.size());
    for (FlatMutation mutation : mutations) {
      hasher.putInt(mutation.secureKey.length).putBytes(mutation.secureKey);
      if (mutation.encodedValue == null) {
        hasher.putInt(-1);
      } else {
        hasher.putInt(mutation.encodedValue.length).putBytes(mutation.encodedValue);
      }
    }
    return hasher.hash().asBytes();
  }

  /** Reconciles an interrupted global publication without accepting a partial 28-DB target. */
  public synchronized PublicationRecovery recoverPublication() throws IOException {
    requireOpen();
    Path intentPath = directory.resolve(INTENT_FILE);
    Path currentPath = directory.resolve(CURRENT_FILE);
    boolean hasIntent = Files.exists(intentPath, LinkOption.NOFOLLOW_LINKS);
    boolean hasCurrent = Files.exists(currentPath, LinkOption.NOFOLLOW_LINKS);
    boolean currentValid = false;
    IOException invalidCurrent = null;
    if (hasCurrent) {
      try {
        currentValid = matchesPreparedTarget(loadGlobalTarget(currentPath));
      } catch (IOException failure) {
        invalidCurrent = failure;
      }
    }
    if (!hasIntent) {
      if (hasCurrent && !currentValid) {
        throw new IOException("path-state physical CURRENT is not the exact prepared 28-DB target",
            invalidCurrent);
      }
      return PublicationRecovery.NONE;
    }

    PathStatePhysicalGlobalIntent intent;
    boolean intentValid;
    try {
      intent = loadGlobalTarget(intentPath);
      intentValid = matchesPreparedTarget(intent);
    } catch (IOException invalidIntent) {
      if (!currentValid) {
        throw invalidIntent;
      }
      PathStateMetadataFile.deleteDurable(intentPath);
      return PublicationRecovery.RETAINED_CURRENT;
    }
    if (!intentValid) {
      if (!currentValid) {
        throw new IOException("path-state physical INTENT is not the exact prepared 28-DB target");
      }
      PathStateMetadataFile.deleteDurable(intentPath);
      return PublicationRecovery.RETAINED_CURRENT;
    }
    PathStateMetadataFile.replaceCurrentBytes(currentPath, intent.encode());
    PathStateMetadataFile.deleteDurable(intentPath);
    return PublicationRecovery.COMPLETED_INTENT;
  }

  /** Deletes one exact physical key, commits changed paths, and republishes the 28-DB target. */
  public synchronized byte[] deleteAndPublish(String dbName, byte[] physicalKey)
      throws IOException {
    return deleteAndPublish(dbName, physicalKey, stage -> { }).getStateRoot();
  }

  synchronized PhysicalDeleteResult deleteAndPublish(String dbName, byte[] physicalKey,
      DeleteFaultHook faultHook) throws IOException {
    requireOpen();
    DeleteFaultHook hook = Objects.requireNonNull(faultHook, "faultHook");
    recoverPublication();
    PathStatePhysicalGlobalIntent current = currentTarget();
    PathStateRoot restored = createRoot();
    restored.restoreStoredRoots(current.getSuperRoot());
    requireParticipantRoots(restored, current);
    PathStateRoot.Snapshot snapshot = restored.snapshot();

    Map<Integer, RecordingNodeStore> recordings = new LinkedHashMap<>();
    PathStateRoot candidate = PathStateRoot.fromSnapshot(scope,
        participant -> recordings.computeIfAbsent(participant.getStoreId(), ignored ->
            new RecordingNodeStore(participant(participant.getDbName()).nodeStore())),
        recordings.computeIfAbsent(0, ignored -> new RecordingNodeStore(superStore.nodeStore())),
        snapshot);
    PathStateParticipant targetParticipant = scope.require(dbName);
    byte[] rawKey = Arrays.copyOf(Objects.requireNonNull(physicalKey, "physicalKey"),
        physicalKey.length);
    byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(targetParticipant.getStoreId(),
        rawKey);
    PhysicalStore targetStore = participant(targetParticipant.getDbName());
    if (targetStore.getFlat(secureKey) == null) {
      throw new IOException("path-state physical delete leaf is missing: " + dbName);
    }
    candidate.delete(targetParticipant.getDbName(), rawKey);
    byte[] stateRoot = candidate.rootHash();
    byte[] storeRoot = candidate.participantRoot(targetParticipant.getDbName());
    RecordingNodeStore participantChanges = recordings.get(targetParticipant.getStoreId());
    requireOnlyTargetParticipantChanged(recordings, targetParticipant.getStoreId());
    byte[] flatDigest = flatDigestExcluding(targetStore, secureKey);
    byte[] generation = participantGeneration(targetParticipant, flatDigest, storeRoot);
    targetStore.applyParticipantDelete(secureKey, participantChanges.mutations(), flatDigest,
        generation, storeRoot);
    hook.after(DeleteStage.AFTER_PARTICIPANT_BATCH);

    List<PathStatePhysicalGlobalIntent.ParticipantTarget> targets = replaceParticipantTarget(
        current.getParticipants(), targetParticipant.getStoreId(), generation, flatDigest,
        storeRoot);
    RecordingNodeStore superChanges = recordings.get(0);
    byte[] nextSuperGeneration = superGeneration(targets, stateRoot);
    superStore.applySuperTransition(superChanges.mutations(), nextSuperGeneration, stateRoot);
    hook.after(DeleteStage.AFTER_SUPER_BATCH);

    byte[] published = publishCurrent(metadataWithRoot(current.getMetadata(), stateRoot));
    hook.after(DeleteStage.AFTER_CURRENT);
    return new PhysicalDeleteResult(published, participantChanges.putCount(),
        participantChanges.deleteCount(), superChanges.putCount(), superChanges.deleteCount());
  }

  private PathStatePhysicalGlobalIntent currentTarget() throws IOException {
    Path currentPath = directory.resolve(CURRENT_FILE);
    if (!Files.exists(currentPath, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("path-state physical CURRENT is missing");
    }
    PathStatePhysicalGlobalIntent current = loadGlobalTarget(currentPath);
    if (!matchesPreparedTarget(current)) {
      throw new IOException("path-state physical CURRENT differs from prepared target");
    }
    return current;
  }

  private void requireParticipantRoots(PathStateRoot root,
      PathStatePhysicalGlobalIntent current) throws IOException {
    for (PathStatePhysicalGlobalIntent.ParticipantTarget target : current.getParticipants()) {
      PathStateParticipant participant = participant(target.getStoreId());
      requireSame(target.getStoreRoot(), root.participantRoot(participant.getDbName()),
          "path-state physical participant root differs from CURRENT: "
              + participant.getDbName());
    }
  }

  private static void requireOnlyTargetParticipantChanged(
      Map<Integer, RecordingNodeStore> recordings, int targetStoreId) {
    for (Map.Entry<Integer, RecordingNodeStore> entry : recordings.entrySet()) {
      if (entry.getKey() != 0 && entry.getKey() != targetStoreId
          && !entry.getValue().isEmpty()) {
        throw new IllegalStateException("physical delete changed another participant Store");
      }
    }
  }

  private static List<PathStatePhysicalGlobalIntent.ParticipantTarget> replaceParticipantTarget(
      List<PathStatePhysicalGlobalIntent.ParticipantTarget> current, int storeId,
      byte[] generation, byte[] flatDigest, byte[] storeRoot) {
    List<PathStatePhysicalGlobalIntent.ParticipantTarget> targets = new ArrayList<>();
    boolean replaced = false;
    for (PathStatePhysicalGlobalIntent.ParticipantTarget target : current) {
      if (target.getStoreId() == storeId) {
        targets.add(new PathStatePhysicalGlobalIntent.ParticipantTarget(storeId, generation,
            flatDigest, storeRoot));
        replaced = true;
      } else {
        targets.add(target);
      }
    }
    if (!replaced) {
      throw new IllegalArgumentException("physical delete Store ID is not in CURRENT");
    }
    return targets;
  }

  private static byte[] flatDigestExcluding(PhysicalStore store, byte[] excludedSecureKey)
      throws IOException {
    Hasher hasher = Hashing.sha256().newHasher();
    boolean[] excluded = new boolean[1];
    store.scanFlat(entry -> {
      byte[] secureKey = unprefixedFlatKey(entry.getKey());
      if (Arrays.equals(secureKey, excludedSecureKey)) {
        excluded[0] = true;
        return;
      }
      byte[] encodedValue = entry.getValue();
      hasher.putInt(secureKey.length).putBytes(secureKey)
          .putInt(encodedValue.length).putBytes(encodedValue);
    });
    if (!excluded[0]) {
      throw new IOException("path-state physical delete leaf disappeared during digest scan");
    }
    return hasher.hash().asBytes();
  }

  private byte[] publicationTargetRoot() throws IOException {
    return requireDigest(superStore.getMetadata(FLAT_ROOT_METADATA),
        "path-state physical super root is missing or invalid");
  }

  private PathStatePhysicalGlobalIntent publicationTarget(PathStateRootMetadata metadata)
      throws IOException {
    List<PathStatePhysicalGlobalIntent.ParticipantTarget> targets = new ArrayList<>();
    for (PathStateParticipant participant : scope.getParticipants()) {
      PhysicalStore store = participant(participant.getDbName());
      byte[] storeRoot = requireDigest(store.getMetadata(FLAT_COMPLETE_METADATA),
          "path-state physical Store root is missing or invalid: " + participant.getDbName());
      requireRootNode(store, storeRoot,
          "path-state physical Store root node differs: " + participant.getDbName());
      byte[] flatDigest = requireDigest(store.getMetadata(FLAT_DIGEST_METADATA),
          "path-state physical flat digest is missing or invalid: " + participant.getDbName());
      byte[] generation = requireDigest(store.getMetadata(STORE_GENERATION_METADATA),
          "path-state physical Store generation is missing or invalid: "
              + participant.getDbName());
      requireSame(generation, participantGeneration(participant, flatDigest, storeRoot),
          "path-state physical Store generation differs: " + participant.getDbName());
      targets.add(new PathStatePhysicalGlobalIntent.ParticipantTarget(participant.getStoreId(),
          generation, flatDigest, storeRoot));
    }
    byte[] superRoot = requireDigest(superStore.getMetadata(FLAT_ROOT_METADATA),
        "path-state physical super root is missing or invalid");
    requireRootNode(superStore, superRoot,
        "path-state physical super root node differs");
    byte[] generation = requireDigest(superStore.getMetadata(SUPER_GENERATION_METADATA),
        "path-state physical super generation is missing or invalid");
    requireSame(generation, superGeneration(targets, superRoot),
        "path-state physical super generation differs");
    return new PathStatePhysicalGlobalIntent(manifest.getIdentityDigest(), metadata, targets,
        generation, superRoot);
  }

  private boolean matchesPreparedTarget(PathStatePhysicalGlobalIntent target) throws IOException {
    try {
      return Arrays.equals(target.encode(), publicationTarget(target.getMetadata()).encode());
    } catch (IllegalArgumentException invalidTarget) {
      throw new IOException("path-state physical target metadata differs from prepared storage",
          invalidTarget);
    }
  }

  private PathStateRootMetadata syntheticMetadata(byte[] stateRoot) {
    byte[] zero = new byte[PathStateRootMetadata.DIGEST_LENGTH];
    return PathStateRootMetadata.base(0, zero, zero, 0,
        PathStateCanonicalizer.P66Phase.P66_OFF, manifest.getIdentityDigest(), stateRoot, zero);
  }

  private PathStateRootMetadata metadataWithRoot(PathStateRootMetadata previous,
      byte[] stateRoot) {
    if (previous.getKind() == PathStateRootMetadata.Kind.BASE) {
      return PathStateRootMetadata.base(previous.getBlockNumber(), previous.getBlockHash(),
          previous.getParentHash(), previous.getTimestamp(), previous.getPhase(),
          manifest.getIdentityDigest(), stateRoot, previous.getPayloadDigest());
    }
    return PathStateRootMetadata.layer(previous.getBlockNumber(), previous.getBlockHash(),
        previous.getParentHash(), previous.getTimestamp(), previous.getPhase(),
        manifest.getIdentityDigest(), previous.getParentStateRoot(), stateRoot,
        previous.getPayloadDigest());
  }

  private static PathStatePhysicalGlobalIntent loadGlobalTarget(Path path) throws IOException {
    try {
      return PathStatePhysicalGlobalIntent.decode(PathStateMetadataFile.loadImmutableBytes(path,
          PathStatePhysicalGlobalIntent.MAX_ENCODED_LENGTH));
    } catch (IllegalArgumentException invalid) {
      throw new IOException("path-state physical global target is corrupt: " + path, invalid);
    }
  }

  private byte[] participantGeneration(PathStateParticipant participant, byte[] flatDigest,
      byte[] storeRoot) {
    return Hashing.sha256().newHasher()
        .putString("java-tron/path-state/participant-generation/v1", StandardCharsets.US_ASCII)
        .putBytes(manifest.getIdentityDigest()).putInt(participant.getStoreId())
        .putBytes(flatDigest).putBytes(storeRoot).hash().asBytes();
  }

  private byte[] superGeneration(
      List<PathStatePhysicalGlobalIntent.ParticipantTarget> targets, byte[] superRoot) {
    Hasher hasher = Hashing.sha256().newHasher()
        .putString("java-tron/path-state/super-generation/v1", StandardCharsets.US_ASCII)
        .putBytes(manifest.getIdentityDigest()).putInt(targets.size());
    for (PathStatePhysicalGlobalIntent.ParticipantTarget target : targets) {
      hasher.putInt(target.getStoreId()).putBytes(target.getGeneration())
          .putBytes(target.getFlatDigest()).putBytes(target.getStoreRoot());
    }
    return hasher.putBytes(superRoot).hash().asBytes();
  }

  private static byte[] requireDigest(byte[] value, String error) throws IOException {
    if (value == null || value.length != PathStatePhysicalGlobalIntent.DIGEST_LENGTH) {
      throw new IOException(error);
    }
    return Arrays.copyOf(value, value.length);
  }

  private static void requireSame(byte[] expected, byte[] actual, String error)
      throws IOException {
    if (!Arrays.equals(expected, actual)) {
      throw new IOException(error);
    }
  }

  private static void requireRootNode(PhysicalStore store, byte[] expectedRoot, String error)
      throws IOException {
    byte[] encodedRoot = store.nodeStore().get(new byte[0]);
    if (Arrays.equals(expectedRoot, Hash.EMPTY_TRIE_HASH)) {
      if (encodedRoot != null) {
        throw new IOException(error);
      }
    } else if (encodedRoot == null || !Arrays.equals(expectedRoot, Hash.sha3(encodedRoot))) {
      throw new IOException(error);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    participantWriteExecutor.shutdownNow();
    participantPrepareExecutor.shutdownNow();
    trieBranchExecutor.shutdownNow();
    IOException failure = null;
    for (PhysicalStore store : participants.values()) {
      try {
        store.close();
      } catch (IOException closeFailure) {
        failure = append(failure, closeFailure);
      }
    }
    try {
      superStore.close();
    } catch (IOException closeFailure) {
      failure = append(failure, closeFailure);
    }
    if (failure != null) {
      throw failure;
    }
  }

  private void closeAfterFailure(Throwable original) {
    participantWriteExecutor.shutdownNow();
    participantPrepareExecutor.shutdownNow();
    trieBranchExecutor.shutdownNow();
    for (PhysicalStore store : participants.values()) {
      try {
        store.close();
      } catch (IOException closeFailure) {
        original.addSuppressed(closeFailure);
      }
    }
  }

  private static void rejectLegacySharedNodes(Path root) throws IOException {
    Path oldNodes = root.resolve(PathStateStoreManifest.BASE_DIRECTORY).resolve(NODES_DIRECTORY);
    if (Files.exists(oldNodes, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("TASK-018 physical layout rejects legacy shared base/nodes: "
          + oldNodes);
    }
  }

  private static void requireStoreDirectory(Path path) throws IOException {
    if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("path-state physical Store directory is missing: " + path);
    }
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("path-state physical store set is closed: " + directory);
    }
  }

  private PathStateParticipant participant(int storeId) {
    for (PathStateParticipant participant : scope.getParticipants()) {
      if (participant.getStoreId() == storeId) {
        return participant;
      }
    }
    throw new IllegalArgumentException("unknown path-state Store ID: " + storeId);
  }

  private static IOException append(IOException previous, IOException next) {
    if (previous == null) {
      return next;
    }
    previous.addSuppressed(next);
    return previous;
  }

  private static PathStateParticipantScope requireExactScope(PathStateParticipantScope scope) {
    PathStateParticipantScope supplied = Objects.requireNonNull(scope, "scope");
    List<String> dbNames = new ArrayList<>();
    for (PathStateParticipant participant : supplied.getParticipants()) {
      dbNames.add(participant.getDbName());
    }
    PathStateParticipantDescriptor descriptor = PathStateParticipantDescriptor.current();
    descriptor.requireExactDatabases(dbNames);
    for (PathStateParticipant participant : supplied.getParticipants()) {
      if (descriptor.require(participant.getDbName()).getStoreId() != participant.getStoreId()) {
        throw new IllegalArgumentException("path-state physical Store ID differs: "
            + participant.getDbName());
      }
    }
    return supplied;
  }

  private static byte[] unprefixedFlatKey(byte[] storedKey) {
    byte[] key = Arrays.copyOf(Objects.requireNonNull(storedKey, "storedKey"), storedKey.length);
    if (key.length != PathStateCommitmentCodec.ROOT_LENGTH + 1
        || key[0] != FLAT_STATE_PREFIX) {
      throw new IllegalStateException("path-state physical F key is corrupt");
    }
    return Arrays.copyOfRange(key, 1, key.length);
  }

  private static long elapsedMillis(long startedNanos) {
    return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
        System.nanoTime() - startedNanos);
  }

  private static long elapsedMillis(long startedNanos, long completedNanos) {
    return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(completedNanos - startedNanos);
  }

  private static long rowsPerSecond(long rows, long startedNanos) {
    long elapsedNanos = Math.max(1L, System.nanoTime() - startedNanos);
    return (long) (rows * 1_000_000_000D / elapsedNanos);
  }

  /** One participant or super database with independent F/N/M key domains. */
  public static final class PhysicalStore implements Closeable {

    private final PathStateNativeNodeStore nativeStore;
    private final ResidentNodeStore nodeStore;

    private PhysicalStore(Path directory, Engine engine, int storeId,
        ResidentNodeCache residentNodeCache, String storageProfile, NativeDbConfig dbSettings)
        throws IOException {
      nativeStore = PathStateNativeNodeStore.open(directory, engine, storageProfile, dbSettings);
      nodeStore = new ResidentNodeStore(new PhysicalNodeStore(nativeStore), residentNodeCache,
          storeId);
    }

    public void putFlat(byte[] secureKey, byte[] encodedLeaf) {
      nativeStore.put(prefixed(FLAT_STATE_PREFIX, secureKey, "secureKey"), encodedLeaf);
    }

    private void writeBatch(List<PathStateNativeNodeStore.BatchMutation> mutations) {
      nativeStore.writeBatch(new ArrayList<>(mutations));
    }

    public byte[] getFlat(byte[] secureKey) {
      return nativeStore.get(prefixed(FLAT_STATE_PREFIX, secureKey, "secureKey"));
    }

    public void deleteFlat(byte[] secureKey) {
      nativeStore.delete(prefixed(FLAT_STATE_PREFIX, secureKey, "secureKey"));
    }

    void scanFlat(PathStateNativeNodeStore.EntryConsumer consumer) throws IOException {
      nativeStore.scanPrefix(new byte[]{FLAT_STATE_PREFIX},
          Objects.requireNonNull(consumer, "consumer"));
    }

    public PathNodeStore nodeStore() {
      return nodeStore;
    }

    private PhysicalNodeBatchWriter nodeBatchWriter() {
      return new PhysicalNodeBatchWriter(nativeStore);
    }

    private void completeFlatBuild(byte[] flatDigest, byte[] generation, byte[] storeRoot) {
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>(3);
      mutations.add(metadataMutation(FLAT_DIGEST_METADATA, flatDigest));
      mutations.add(metadataMutation(STORE_GENERATION_METADATA, generation));
      mutations.add(metadataMutation(FLAT_COMPLETE_METADATA, storeRoot));
      nativeStore.writeBatch(mutations);
    }

    long getWriteBatchCalls() {
      return nativeStore.getWriteBatchCalls();
    }

    long getWriteBatchMutations() {
      return nativeStore.getWriteBatchMutations();
    }

    long getSyncedWriteBatchCalls() {
      return nativeStore.getSyncedWriteBatchCalls();
    }

    long getUnsyncedWriteBatchCalls() {
      return nativeStore.getUnsyncedWriteBatchCalls();
    }

    void clearNodes() throws IOException {
      nodeStore.clear();
      List<PathStateNativeNodeStore.BatchMutation> pending = new ArrayList<>(4096);
      nativeStore.scanPrefix(new byte[]{TRIE_NODE_PREFIX}, entry -> {
        pending.add(PathStateNativeNodeStore.BatchMutation.delete(entry.getKey()));
        if (pending.size() == 4096) {
          nativeStore.writeBatch(new ArrayList<>(pending));
          pending.clear();
        }
      });
      if (!pending.isEmpty()) {
        nativeStore.writeBatch(pending);
      }
    }

    void applyParticipantDelete(byte[] secureKey, List<NodeMutation> nodeMutations,
        byte[] flatDigest, byte[] generation, byte[] storeRoot) {
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>();
      mutations.add(PathStateNativeNodeStore.BatchMutation.delete(
          prefixed(FLAT_STATE_PREFIX, secureKey, "secureKey")));
      appendNodeMutations(mutations, nodeMutations);
      mutations.add(metadataMutation(FLAT_DIGEST_METADATA, flatDigest));
      mutations.add(metadataMutation(STORE_GENERATION_METADATA, generation));
      mutations.add(metadataMutation(FLAT_COMPLETE_METADATA, storeRoot));
      nativeStore.writeBatchUnsynced(mutations);
      nodeStore.apply(nodeMutations);
    }

    void applyParticipantTransition(List<FlatMutation> flatMutations,
        List<NodeMutation> nodeMutations, byte[] flatDigest, byte[] generation,
        byte[] storeRoot) {
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>();
      for (FlatMutation mutation : Objects.requireNonNull(flatMutations, "flatMutations")) {
        byte[] key = prefixed(FLAT_STATE_PREFIX, mutation.secureKey, "secureKey");
        mutations.add(mutation.encodedValue == null
            ? PathStateNativeNodeStore.BatchMutation.delete(key)
            : PathStateNativeNodeStore.BatchMutation.put(key, mutation.encodedValue));
      }
      appendNodeMutations(mutations, nodeMutations);
      mutations.add(metadataMutation(FLAT_DIGEST_METADATA, flatDigest));
      mutations.add(metadataMutation(STORE_GENERATION_METADATA, generation));
      mutations.add(metadataMutation(FLAT_COMPLETE_METADATA, storeRoot));
      nativeStore.writeBatchUnsynced(mutations);
      nodeStore.apply(nodeMutations);
    }

    void applySuperTransition(List<NodeMutation> nodeMutations, byte[] generation,
        byte[] superRoot) {
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>();
      appendNodeMutations(mutations, nodeMutations);
      mutations.add(metadataMutation(SUPER_GENERATION_METADATA, generation));
      mutations.add(metadataMutation(FLAT_ROOT_METADATA, superRoot));
      nativeStore.writeBatchUnsynced(mutations);
      nodeStore.apply(nodeMutations);
    }

    void applyCheckpointParticipant(CommonCheckpointPayload.PathStoreTarget target,
        byte[] marker) {
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>();
      for (CommonCheckpointPayload.Mutation mutation : target.getFlatMutations()) {
        byte[] key = prefixed(FLAT_STATE_PREFIX, mutation.getKey(), "secureKey");
        mutations.add(mutation.isDelete()
            ? PathStateNativeNodeStore.BatchMutation.delete(key)
            : PathStateNativeNodeStore.BatchMutation.put(key, mutation.getValue()));
      }
      List<NodeMutation> cacheMutations = new ArrayList<>();
      for (CommonCheckpointPayload.Mutation mutation : target.getNodeMutations()) {
        byte[] path = mutation.getKey();
        byte[] value = mutation.getValue();
        mutations.add(mutation.isDelete()
            ? PathStateNativeNodeStore.BatchMutation.delete(
                prefixed(TRIE_NODE_PREFIX, path, "path"))
            : PathStateNativeNodeStore.BatchMutation.put(
                prefixed(TRIE_NODE_PREFIX, path, "path"),
                value));
        cacheMutations.add(new NodeMutation(path, value));
      }
      mutations.add(metadataMutation(CHECKPOINT_TARGET_METADATA, marker));
      nativeStore.writeBatch(mutations);
      nodeStore.apply(cacheMutations);
    }

    void applyCheckpointSuper(List<CommonCheckpointPayload.Mutation> supplied, byte[] marker) {
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>();
      List<NodeMutation> cacheMutations = new ArrayList<>();
      for (CommonCheckpointPayload.Mutation mutation : supplied) {
        byte[] path = mutation.getKey();
        byte[] value = mutation.getValue();
        mutations.add(mutation.isDelete()
            ? PathStateNativeNodeStore.BatchMutation.delete(
                prefixed(TRIE_NODE_PREFIX, path, "path"))
            : PathStateNativeNodeStore.BatchMutation.put(
                prefixed(TRIE_NODE_PREFIX, path, "path"),
                value));
        cacheMutations.add(new NodeMutation(path, value));
      }
      mutations.add(metadataMutation(CHECKPOINT_TARGET_METADATA, marker));
      nativeStore.writeBatch(mutations);
      nodeStore.apply(cacheMutations);
    }

    byte[] checkpointTargetMarker() {
      return getMetadata(CHECKPOINT_TARGET_METADATA);
    }

    private static void appendNodeMutations(
        List<PathStateNativeNodeStore.BatchMutation> target,
        List<NodeMutation> nodeMutations) {
      for (NodeMutation mutation : Objects.requireNonNull(nodeMutations, "nodeMutations")) {
        byte[] key = prefixed(TRIE_NODE_PREFIX, mutation.path, "path");
        target.add(mutation.encodedNode == null
            ? PathStateNativeNodeStore.BatchMutation.delete(key)
            : PathStateNativeNodeStore.BatchMutation.put(key, mutation.encodedNode));
      }
    }

    private static PathStateNativeNodeStore.BatchMutation metadataMutation(byte[] name,
        byte[] value) {
      return PathStateNativeNodeStore.BatchMutation.put(prefixed(METADATA_PREFIX, name,
          "metadata name"), value);
    }

    public void putMetadata(byte[] name, byte[] value) {
      nativeStore.put(prefixed(METADATA_PREFIX, name, "metadata name"), value);
    }

    public byte[] getMetadata(byte[] name) {
      return nativeStore.get(prefixed(METADATA_PREFIX, name, "metadata name"));
    }

    void deleteMetadata(byte[] name) {
      nativeStore.delete(prefixed(METADATA_PREFIX, name, "metadata name"));
    }

    public Path getDirectory() {
      return nativeStore.getDirectory();
    }

    @Override
    public void close() throws IOException {
      nodeStore.clear();
      nativeStore.close();
    }
  }

  private static final class PhysicalNodeStore implements PathNodeStore {

    private final PathStateNativeNodeStore nativeStore;

    private PhysicalNodeStore(PathStateNativeNodeStore nativeStore) {
      this.nativeStore = nativeStore;
    }

    @Override
    public byte[] get(byte[] path) {
      return nativeStore.get(prefixed(TRIE_NODE_PREFIX, path, "path"));
    }

    @Override
    public void put(byte[] path, byte[] encodedNode) {
      nativeStore.put(prefixed(TRIE_NODE_PREFIX, path, "path"), encodedNode);
    }

    @Override
    public void delete(byte[] path) {
      nativeStore.delete(prefixed(TRIE_NODE_PREFIX, path, "path"));
    }
  }

  static final class ResidentNodeStore implements PathNodeStore {

    private final PathNodeStore base;
    private final ResidentNodeCache cache;
    private final int storeId;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong cleanHits = new AtomicLong();
    private final AtomicLong updatedHits = new AtomicLong();
    private final AtomicLong nativeReads = new AtomicLong();

    ResidentNodeStore(PathNodeStore base, ResidentNodeCache cache, int storeId) {
      this.base = Objects.requireNonNull(base, "base");
      this.cache = Objects.requireNonNull(cache, "cache");
      this.storeId = storeId;
    }

    @Override
    public byte[] get(byte[] path) {
      ResidentNodeCache.Lookup lookup = cache.get(storeId, path);
      if (lookup.found) {
        hits.incrementAndGet();
        if (lookup.updated) {
          updatedHits.incrementAndGet();
        } else {
          cleanHits.incrementAndGet();
        }
        return owned(lookup.value);
      }
      byte[] value = base.get(path);
      nativeReads.incrementAndGet();
      cache.put(storeId, path, value, false);
      return owned(value);
    }

    @Override
    public void put(byte[] path, byte[] encodedNode) {
      base.put(path, encodedNode);
      cache.put(storeId, path, Objects.requireNonNull(encodedNode, "encodedNode"), true);
    }

    @Override
    public void delete(byte[] path) {
      base.delete(path);
      cache.put(storeId, path, null, true);
    }

    void apply(List<NodeMutation> mutations) {
      for (NodeMutation mutation : Objects.requireNonNull(mutations, "mutations")) {
        cache.put(storeId, mutation.path, mutation.encodedNode, true);
      }
    }

    void clear() {
      cache.clear(storeId);
    }

    long getHits() {
      return hits.get();
    }

    long getCleanHits() {
      return cleanHits.get();
    }

    long getUpdatedHits() {
      return updatedHits.get();
    }

    long getNativeReads() {
      return nativeReads.get();
    }

    private static byte[] owned(byte[] value) {
      return value == null ? null : Arrays.copyOf(value, value.length);
    }
  }

  static final class ResidentNodeCache {

    private static final long ENTRY_OVERHEAD_BYTES = 64;
    private final long maxBytes;
    private final Map<ResidentNodeCacheKey, ResidentNodeCacheValue> values =
        new LinkedHashMap<>(1024, 0.75f, true);
    private long bytes;
    private long evictions;

    ResidentNodeCache(long maxBytes) {
      if (maxBytes <= 0) {
        throw new IllegalArgumentException("resident node cache bytes must be positive");
      }
      this.maxBytes = maxBytes;
    }

    synchronized Lookup get(int storeId, byte[] path) {
      ResidentNodeCacheValue value = values.get(new ResidentNodeCacheKey(storeId, path));
      return value == null ? Lookup.missing() : Lookup.found(value.value, value.updated);
    }

    synchronized void put(int storeId, byte[] path, byte[] value, boolean updated) {
      ResidentNodeCacheKey key = new ResidentNodeCacheKey(storeId, path);
      ResidentNodeCacheValue replacement = new ResidentNodeCacheValue(value, updated,
          ENTRY_OVERHEAD_BYTES + Integer.BYTES + key.path.length
              + (value == null ? 0 : value.length));
      ResidentNodeCacheValue previous = values.put(key, replacement);
      if (previous != null) {
        bytes -= previous.weight;
      }
      bytes += replacement.weight;
      java.util.Iterator<Map.Entry<ResidentNodeCacheKey, ResidentNodeCacheValue>> iterator =
          values.entrySet().iterator();
      while (bytes > maxBytes && iterator.hasNext()) {
        Map.Entry<ResidentNodeCacheKey, ResidentNodeCacheValue> eldest = iterator.next();
        bytes -= eldest.getValue().weight;
        iterator.remove();
        evictions++;
      }
    }

    synchronized void clear(int storeId) {
      java.util.Iterator<Map.Entry<ResidentNodeCacheKey, ResidentNodeCacheValue>> iterator =
          values.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<ResidentNodeCacheKey, ResidentNodeCacheValue> entry = iterator.next();
        if (entry.getKey().storeId == storeId) {
          bytes -= entry.getValue().weight;
          iterator.remove();
        }
      }
    }

    synchronized long bytes() {
      return bytes;
    }

    synchronized int size() {
      return values.size();
    }

    synchronized long evictions() {
      return evictions;
    }

    static final class Lookup {

      private final boolean found;
      private final byte[] value;
      private final boolean updated;

      private Lookup(boolean found, byte[] value, boolean updated) {
        this.found = found;
        this.value = value == null ? null : Arrays.copyOf(value, value.length);
        this.updated = updated;
      }

      private static Lookup missing() {
        return new Lookup(false, null, false);
      }

      private static Lookup found(byte[] value, boolean updated) {
        return new Lookup(true, value, updated);
      }
    }
  }

  private static final class ResidentNodeCacheKey {

    private final int storeId;
    private final byte[] path;

    private ResidentNodeCacheKey(int storeId, byte[] path) {
      this.storeId = storeId;
      this.path = Arrays.copyOf(Objects.requireNonNull(path, "path"), path.length);
    }

    @Override
    public boolean equals(Object other) {
      return this == other || other instanceof ResidentNodeCacheKey
          && storeId == ((ResidentNodeCacheKey) other).storeId
          && Arrays.equals(path, ((ResidentNodeCacheKey) other).path);
    }

    @Override
    public int hashCode() {
      return 31 * storeId + Arrays.hashCode(path);
    }
  }

  private static final class ResidentNodeCacheValue {

    private final byte[] value;
    private final boolean updated;
    private final long weight;

    private ResidentNodeCacheValue(byte[] value, boolean updated, long weight) {
      this.value = value == null ? null : Arrays.copyOf(value, value.length);
      this.updated = updated;
      this.weight = weight;
    }
  }

  private static final class PhysicalNodeBatchWriter implements PathNodeStore {

    private final PathStateNativeNodeStore nativeStore;
    private final List<PathStateNativeNodeStore.BatchMutation> pending =
        new ArrayList<>(BOOTSTRAP_WRITE_BATCH_ENTRIES);
    private long pendingBytes;

    private PhysicalNodeBatchWriter(PathStateNativeNodeStore nativeStore) {
      this.nativeStore = nativeStore;
    }

    @Override
    public byte[] get(byte[] path) {
      flush();
      return nativeStore.get(prefixed(TRIE_NODE_PREFIX, path, "path"));
    }

    @Override
    public void put(byte[] path, byte[] encodedNode) {
      byte[] key = prefixed(TRIE_NODE_PREFIX, path, "path");
      byte[] value = Arrays.copyOf(Objects.requireNonNull(encodedNode, "encodedNode"),
          encodedNode.length);
      flushBeforeOversizedMutation(key.length + value.length);
      pending.add(PathStateNativeNodeStore.BatchMutation.put(key, value));
      pendingBytes = Math.addExact(pendingBytes, key.length + value.length);
      flushIfFull();
    }

    @Override
    public void delete(byte[] path) {
      byte[] key = prefixed(TRIE_NODE_PREFIX, path, "path");
      flushBeforeOversizedMutation(key.length);
      pending.add(PathStateNativeNodeStore.BatchMutation.delete(key));
      pendingBytes = Math.addExact(pendingBytes, key.length);
      flushIfFull();
    }

    private void flushIfFull() {
      if (pending.size() >= BOOTSTRAP_WRITE_BATCH_ENTRIES
          || pendingBytes >= BOOTSTRAP_WRITE_BATCH_BYTES) {
        flush();
      }
    }

    private void flushBeforeOversizedMutation(long mutationBytes) {
      if (!pending.isEmpty() && pendingBytes + mutationBytes > BOOTSTRAP_WRITE_BATCH_BYTES) {
        flush();
      }
    }

    private void flush() {
      if (!pending.isEmpty()) {
        nativeStore.writeBatch(new ArrayList<>(pending));
        pending.clear();
        pendingBytes = 0;
      }
    }
  }

  static final class RecordingNodeStore implements PathNodeStore {

    private static final byte[] ABSENT = new byte[0];
    private final PathNodeStore base;
    private final Map<BytesKey, byte[]> changes = new LinkedHashMap<>();
    private final Map<BytesKey, byte[]> baseReads = new ConcurrentHashMap<>();
    private final long initialResidentReadHits;
    private final long initialResidentCleanHits;
    private final long initialResidentUpdatedHits;
    private final long initialNativeReads;
    private final AtomicLong baseReadMisses = new AtomicLong();
    private final AtomicLong baseReadHits = new AtomicLong();

    RecordingNodeStore(PathNodeStore base) {
      this.base = Objects.requireNonNull(base, "base");
      ResidentNodeStore resident = base instanceof ResidentNodeStore
          ? (ResidentNodeStore) base : null;
      initialResidentReadHits = resident == null ? 0 : resident.getHits();
      initialResidentCleanHits = resident == null ? 0 : resident.getCleanHits();
      initialResidentUpdatedHits = resident == null ? 0 : resident.getUpdatedHits();
      initialNativeReads = resident == null ? 0 : resident.getNativeReads();
    }

    @Override
    public byte[] get(byte[] path) {
      BytesKey key = new BytesKey(path);
      if (changes.containsKey(key)) {
        byte[] value = changes.get(key);
        return value == null ? null : Arrays.copyOf(value, value.length);
      }
      return baseValue(key);
    }

    @Override
    public void put(byte[] path, byte[] encodedNode) {
      changes.put(new BytesKey(path), Arrays.copyOf(
          Objects.requireNonNull(encodedNode, "encodedNode"), encodedNode.length));
    }

    @Override
    public void delete(byte[] path) {
      changes.put(new BytesKey(path), null);
    }

    private boolean isEmpty() {
      return changes.isEmpty();
    }

    private List<NodeMutation> mutations() {
      List<NodeMutation> mutations = new ArrayList<>();
      for (Map.Entry<BytesKey, byte[]> change : changes.entrySet()) {
        mutations.add(new NodeMutation(change.getKey().bytes, change.getValue()));
      }
      return mutations;
    }

    private List<PathStatePhysicalReverseJournal.Entry> reverseEntries() {
      List<PathStatePhysicalReverseJournal.Entry> entries = new ArrayList<>();
      for (BytesKey key : changes.keySet()) {
        entries.add(new PathStatePhysicalReverseJournal.Entry(key.bytes, baseValue(key)));
      }
      return entries;
    }

    private byte[] baseValue(BytesKey key) {
      byte[] value = baseReads.get(key);
      if (value == null) {
        byte[] loaded = base.get(key.bytes);
        baseReadMisses.incrementAndGet();
        byte[] owned = loaded == null ? ABSENT : Arrays.copyOf(loaded, loaded.length);
        byte[] raced = baseReads.putIfAbsent(key, owned);
        value = raced == null ? owned : raced;
      } else {
        baseReadHits.incrementAndGet();
      }
      return value == ABSENT ? null : Arrays.copyOf(value, value.length);
    }

    long getBaseReadMisses() {
      return baseReadMisses.get();
    }

    long getBaseReadHits() {
      return baseReadHits.get();
    }

    long getResidentReadHits() {
      return base instanceof ResidentNodeStore
          ? ((ResidentNodeStore) base).getHits() - initialResidentReadHits : 0;
    }

    long getResidentCleanHits() {
      return base instanceof ResidentNodeStore
          ? ((ResidentNodeStore) base).getCleanHits() - initialResidentCleanHits : 0;
    }

    long getResidentUpdatedHits() {
      return base instanceof ResidentNodeStore
          ? ((ResidentNodeStore) base).getUpdatedHits() - initialResidentUpdatedHits : 0;
    }

    long getNativeReads() {
      return base instanceof ResidentNodeStore
          ? ((ResidentNodeStore) base).getNativeReads() - initialNativeReads
          : baseReadMisses.get();
    }

    private int putCount() {
      int count = 0;
      for (byte[] value : changes.values()) {
        if (value != null) {
          count++;
        }
      }
      return count;
    }

    private int deleteCount() {
      return changes.size() - putCount();
    }
  }

  private static final class NodeMutation {

    private final byte[] path;
    private final byte[] encodedNode;

    private NodeMutation(byte[] path, byte[] encodedNode) {
      this.path = Arrays.copyOf(Objects.requireNonNull(path, "path"), path.length);
      this.encodedNode = encodedNode == null ? null
          : Arrays.copyOf(encodedNode, encodedNode.length);
    }
  }

  private static final class FlatMutation {

    private final byte[] secureKey;
    private final byte[] encodedValue;
    private final boolean previousValueKnown;
    private final byte[] previousEncodedValue;

    private FlatMutation(byte[] secureKey, byte[] encodedValue) {
      this(secureKey, encodedValue, false, null);
    }

    private FlatMutation(byte[] secureKey, byte[] encodedValue, boolean previousValueKnown,
        byte[] previousEncodedValue) {
      this.secureKey = Arrays.copyOf(Objects.requireNonNull(secureKey, "secureKey"),
          secureKey.length);
      this.encodedValue = encodedValue == null ? null
          : Arrays.copyOf(encodedValue, encodedValue.length);
      this.previousValueKnown = previousValueKnown;
      this.previousEncodedValue = previousEncodedValue == null ? null
          : Arrays.copyOf(previousEncodedValue, previousEncodedValue.length);
    }
  }

  private static final class ParticipantTransition {

    private final PathStateParticipant participant;
    private final PhysicalStore store;
    private final List<FlatMutation> flatMutations;
    private final List<NodeMutation> nodeMutations;
    private final byte[] flatDigest;
    private final byte[] generation;
    private final byte[] storeRoot;

    private ParticipantTransition(PathStateParticipant participant, PhysicalStore store,
        List<FlatMutation> flatMutations, List<NodeMutation> nodeMutations, byte[] flatDigest,
        byte[] generation, byte[] storeRoot) {
      this.participant = participant;
      this.store = store;
      this.flatMutations = flatMutations;
      this.nodeMutations = nodeMutations;
      this.flatDigest = flatDigest;
      this.generation = generation;
      this.storeRoot = storeRoot;
    }
  }

  private static final class TransitionPlan {

    private final PathStateBlockTransition transition;
    private final byte[] parentTarget;
    private final PathStatePhysicalGlobalIntent target;
    private final List<ParticipantTransition> participants;
    private final PhysicalStore superStore;
    private final List<NodeMutation> superNodeMutations;
    private final PathStatePhysicalReverseJournal journal;
    private final long nodeReadMisses;
    private final long nodeReadHits;
    private final long residentNodeHits;
    private final long residentCleanHits;
    private final long residentUpdatedHits;
    private final long nativeNodeReads;
    private final long initialResidentEvictions;
    private final long nodeDecodes;
    private final long nodeHashVerifies;
    private final long hashReferencesCreated;
    private final long hashReferencesResolved;
    private final long flatReverseReads;
    private final long flatReverseReused;
    private final PathStateRoot.Snapshot trieSnapshot;
    private final boolean reusedTrieSnapshot;

    private TransitionPlan(PathStateBlockTransition transition, byte[] parentTarget,
        PathStatePhysicalGlobalIntent target,
        List<ParticipantTransition> participants, PhysicalStore superStore,
        List<NodeMutation> superNodeMutations, PathStatePhysicalReverseJournal journal,
        long nodeReadMisses, long nodeReadHits, long residentNodeHits,
        long residentCleanHits, long residentUpdatedHits, long nativeNodeReads,
        long initialResidentEvictions, long nodeDecodes, long nodeHashVerifies,
        long hashReferencesCreated, long hashReferencesResolved,
        long flatReverseReads, long flatReverseReused,
        PathStateRoot.Snapshot trieSnapshot, boolean reusedTrieSnapshot) {
      this.transition = transition;
      this.parentTarget = Arrays.copyOf(parentTarget, parentTarget.length);
      this.target = target;
      this.participants = participants;
      this.superStore = superStore;
      this.superNodeMutations = superNodeMutations;
      this.journal = journal;
      this.nodeReadMisses = nodeReadMisses;
      this.nodeReadHits = nodeReadHits;
      this.residentNodeHits = residentNodeHits;
      this.residentCleanHits = residentCleanHits;
      this.residentUpdatedHits = residentUpdatedHits;
      this.nativeNodeReads = nativeNodeReads;
      this.initialResidentEvictions = initialResidentEvictions;
      this.nodeDecodes = nodeDecodes;
      this.nodeHashVerifies = nodeHashVerifies;
      this.hashReferencesCreated = hashReferencesCreated;
      this.hashReferencesResolved = hashReferencesResolved;
      this.flatReverseReads = flatReverseReads;
      this.flatReverseReused = flatReverseReused;
      this.trieSnapshot = Objects.requireNonNull(trieSnapshot, "trieSnapshot");
      this.reusedTrieSnapshot = reusedTrieSnapshot;
    }
  }

  static final class PreparedPhysicalTransition {

    private final PathStateBlockTransition transition;
    private final TransitionPlan plan;
    private final PathStateSnapshotDelta snapshotDelta;

    private PreparedPhysicalTransition(PathStateBlockTransition transition,
        TransitionPlan plan, PathStateSnapshotDelta snapshotDelta) {
      this.transition = transition;
      this.plan = plan;
      this.snapshotDelta = snapshotDelta;
    }

    PathStateSnapshotDelta getSnapshotDelta() {
      return snapshotDelta;
    }

    boolean reusedTrieSnapshot() {
      return plan.reusedTrieSnapshot;
    }

    boolean matches(PathStateBlockTransition supplied) {
      return transition == supplied;
    }
  }

  private static final class BytesKey {

    private final byte[] bytes;

    private BytesKey(byte[] bytes) {
      this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes"), bytes.length);
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

  private static final class ReverseJournalIndexEntry {

    private final Path path;
    private final long length;
    private final byte[] childTarget;
    private final byte[] parentTarget;

    private ReverseJournalIndexEntry(Path path, long length, byte[] childTarget,
        byte[] parentTarget) {
      this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
      if (length <= 0) {
        throw new IllegalArgumentException("reverse journal length must be positive");
      }
      this.length = length;
      this.childTarget = Arrays.copyOf(Objects.requireNonNull(childTarget, "childTarget"),
          childTarget.length);
      this.parentTarget = Arrays.copyOf(Objects.requireNonNull(parentTarget, "parentTarget"),
          parentTarget.length);
    }
  }

  @FunctionalInterface
  interface BuildFaultHook {

    void beforeCompletion(PathStateParticipant participant, byte[] storeRoot) throws IOException;
  }

  enum PublicationStage {
    AFTER_INTENT,
    AFTER_CURRENT,
    AFTER_RETIRE
  }

  public enum PublicationRecovery {
    NONE,
    RETAINED_CURRENT,
    COMPLETED_INTENT
  }

  enum DeleteStage {
    AFTER_PARTICIPANT_BATCH,
    AFTER_SUPER_BATCH,
    AFTER_CURRENT
  }

  enum TransitionStage {
    AFTER_JOURNAL,
    AFTER_INTENT,
    AFTER_PARTICIPANT_BATCH,
    AFTER_SUPER_BATCH,
    AFTER_CURRENT,
    AFTER_RETIRE
  }

  @FunctionalInterface
  interface TransitionFaultHook {

    void after(TransitionStage stage) throws IOException;
  }

  enum RewindStage {
    AFTER_INTENT,
    AFTER_PARTICIPANT_BATCH,
    AFTER_SUPER_BATCH,
    AFTER_CURRENT,
    AFTER_RETIRE
  }

  @FunctionalInterface
  interface RewindFaultHook {

    void after(RewindStage stage) throws IOException;
  }

  static final class PhysicalDeleteResult {

    private final byte[] stateRoot;
    private final int participantNodePuts;
    private final int participantNodeDeletes;
    private final int superNodePuts;
    private final int superNodeDeletes;

    private PhysicalDeleteResult(byte[] stateRoot, int participantNodePuts,
        int participantNodeDeletes, int superNodePuts, int superNodeDeletes) {
      this.stateRoot = Arrays.copyOf(stateRoot, stateRoot.length);
      this.participantNodePuts = participantNodePuts;
      this.participantNodeDeletes = participantNodeDeletes;
      this.superNodePuts = superNodePuts;
      this.superNodeDeletes = superNodeDeletes;
    }

    byte[] getStateRoot() {
      return Arrays.copyOf(stateRoot, stateRoot.length);
    }

    int getParticipantNodePuts() {
      return participantNodePuts;
    }

    int getParticipantNodeDeletes() {
      return participantNodeDeletes;
    }

    int getSuperNodePuts() {
      return superNodePuts;
    }

    int getSuperNodeDeletes() {
      return superNodeDeletes;
    }
  }

  @FunctionalInterface
  interface PublicationFaultHook {

    void after(PublicationStage stage) throws IOException;
  }

  @FunctionalInterface
  interface DeleteFaultHook {

    void after(DeleteStage stage) throws IOException;
  }

  private static byte[] prefixed(byte prefix, byte[] suffix, String name) {
    byte[] supplied = Arrays.copyOf(Objects.requireNonNull(suffix, name), suffix.length);
    byte[] key = new byte[supplied.length + 1];
    key[0] = prefix;
    System.arraycopy(supplied, 0, key, 1, supplied.length);
    return key;
  }
}
