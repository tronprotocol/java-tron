package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import com.google.common.hash.Hasher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.config.args.StorageConfig.StateArchiveHotStoreConfig;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/**
 * Independent hot State Archive database with one writable and bounded sealed generations.
 *
 * <p>This store owns history data and native WAL durability. It deliberately has no dependency on
 * the common-checkpoint WAL; a later coordinator integration may only consume its durable metadata.
 */
public final class StateArchiveHotStore implements Closeable {

  static final String CURRENT = "CURRENT";
  static final String CURRENT_TEMP = "CURRENT.tmp";
  static final String GENERATIONS = "generations";
  static final String DATABASE = "keys";

  private static final int HASH_LENGTH = 32;
  private static final int CATALOG_MAGIC = 0x53414843; // SAHC
  private static final short CATALOG_VERSION = 1;
  private static final int CATALOG_LENGTH = Integer.BYTES + 2 * Short.BYTES + Long.BYTES
      + HASH_LENGTH + Integer.BYTES;
  private static final int RECORD_MAGIC = 0x53414842; // SAHB
  private static final short RECORD_VERSION = 2;
  private static final byte[] BODY_PREFIX = new byte[]{0x42};
  private static final byte[] INDEX_PREFIX = new byte[]{0x4b};
  private static final String BLOCKS_COLUMN =
      StateArchiveIndexDatabase.HOT_BLOCKS_COLUMN_FAMILY;
  private static final byte[] META_FORMAT = bytes("meta/format");
  private static final byte[] META_ID = bytes("meta/id");
  private static final byte[] META_BASE_BLOCK = bytes("meta/base-block");
  private static final byte[] META_BASE_HASH = bytes("meta/base-hash");
  private static final byte[] META_START_BLOCK = bytes("meta/start-block");
  private static final byte[] META_END_BLOCK = bytes("meta/end-block");
  private static final byte[] META_HEAD_HASH = bytes("meta/head-hash");
  private static final byte[] META_BLOCK_COUNT = bytes("meta/block-count");
  private static final byte[] META_ENCODED_BYTES = bytes("meta/encoded-bytes");
  private static final byte[] META_CONTENT_DIGEST = bytes("meta/content-digest");
  private static final byte[] META_SEALED = bytes("meta/sealed");
  private static final byte[] META_PUBLISHED_BLOCK = bytes("meta/published-block");
  private static final byte[] META_PUBLISHED_HASH = bytes("meta/published-hash");
  private static final byte[] META_PUBLISHED_CONTENT_DIGEST =
      bytes("meta/published-content-digest");
  private static final byte[] META_PUBLISHED_TARGET = bytes("meta/published-target");
  private static final byte[] META_PUBLISHED_DESCRIPTOR = bytes("meta/published-descriptor");
  private static final byte[] META_PREPARED_TARGET = bytes("meta/prepared-target");
  private static final byte[] META_PREPARED_DESCRIPTOR = bytes("meta/prepared-descriptor");
  private static final byte[] META_TRUNCATE_BLOCK = bytes("meta/truncate-block");
  private static final byte[] META_TRUNCATE_HASH = bytes("meta/truncate-hash");
  private static final byte[] ZERO_DIGEST = new byte[HASH_LENGTH];

  private final Path root;
  private final Path generations;
  private final byte[] formatIdentity;
  private final Engine engine;
  private final int maxFrozenGenerations;
  private final long maxBlocks;
  private final long maxEncodedBytes;
  private final int yellowFrozenGenerations;
  private final int redFrozenGenerations;
  private final NativeDbConfig dbSettings;
  private final BlockHistoryCodec historyCodec = new BlockHistoryCodec();
  private final StateArchiveHotBatchDescriptorCodec descriptorCodec =
      new StateArchiveHotBatchDescriptorCodec();
  private final FaultHook faultHook;
  private final List<GenerationMeta> frozen = new ArrayList<>();

  private GenerationMeta current;
  private StateArchiveIndexDatabase.Writer writer;
  private boolean closed;

  private StateArchiveHotStore(Path root, byte[] formatIdentity, Engine engine,
      int maxFrozenGenerations, long maxBlocks, long maxEncodedBytes,
      int yellowFrozenGenerations, int redFrozenGenerations, NativeDbConfig dbSettings,
      FaultHook faultHook) {
    this.root = root;
    this.generations = root.resolve(GENERATIONS);
    this.formatIdentity = copyDigest(formatIdentity, "formatIdentity");
    this.engine = Objects.requireNonNull(engine, "engine");
    this.maxFrozenGenerations = positive(maxFrozenGenerations, "maxFrozenGenerations");
    this.maxBlocks = positive(maxBlocks, "maxBlocks");
    this.maxEncodedBytes = positive(maxEncodedBytes, "maxEncodedBytes");
    this.yellowFrozenGenerations = positive(yellowFrozenGenerations,
        "yellowFrozenGenerations");
    this.redFrozenGenerations = positive(redFrozenGenerations, "redFrozenGenerations");
    if (yellowFrozenGenerations > redFrozenGenerations
        || redFrozenGenerations > maxFrozenGenerations) {
      throw new IllegalArgumentException("frozen watermarks must satisfy yellow <= red <= max");
    }
    this.dbSettings = Objects.requireNonNull(dbSettings, "dbSettings");
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
  }

  /** Opens the isolated Hot DB with its dedicated, default-off configuration object. */
  public static StateArchiveHotStore openOrCreate(Path root, byte[] formatIdentity,
      Engine engine, long baseBlockNumber, byte[] baseBlockHash,
      StateArchiveHotStoreConfig config) throws IOException {
    Objects.requireNonNull(config, "config");
    config.validate();
    if (!config.isEnabled()) {
      throw new IllegalStateException("State Archive Hot DB is disabled");
    }
    return openOrCreate(root, formatIdentity, engine, baseBlockNumber, baseBlockHash,
        config.getMaxFrozenGenerations(), config.getMaxBlocks(), config.getMaxEncodedBytes(),
        config.getYellowFrozenGenerations(), config.getRedFrozenGenerations(),
        config.getDbSettings(), stage -> { });
  }

  public static StateArchiveHotStore openOrCreate(Path root, byte[] formatIdentity,
      Engine engine, long baseBlockNumber, byte[] baseBlockHash, int maxFrozenGenerations,
      long maxBlocks, long maxEncodedBytes) throws IOException {
    return openOrCreate(root, formatIdentity, engine, baseBlockNumber, baseBlockHash,
        maxFrozenGenerations, maxBlocks, maxEncodedBytes,
        Math.max(1, maxFrozenGenerations / 2), maxFrozenGenerations,
        NativeDbConfig.large(), stage -> { });
  }

  static StateArchiveHotStore openOrCreate(Path suppliedRoot, byte[] formatIdentity,
      Engine engine, long baseBlockNumber, byte[] baseBlockHash, int maxFrozenGenerations,
      long maxBlocks, long maxEncodedBytes, FaultHook faultHook) throws IOException {
    return openOrCreate(suppliedRoot, formatIdentity, engine, baseBlockNumber, baseBlockHash,
        maxFrozenGenerations, maxBlocks, maxEncodedBytes,
        Math.max(1, maxFrozenGenerations / 2), maxFrozenGenerations,
        NativeDbConfig.large(), faultHook);
  }

  private static StateArchiveHotStore openOrCreate(Path suppliedRoot, byte[] formatIdentity,
      Engine engine, long baseBlockNumber, byte[] baseBlockHash, int maxFrozenGenerations,
      long maxBlocks, long maxEncodedBytes, int yellowFrozenGenerations,
      int redFrozenGenerations, NativeDbConfig dbSettings, FaultHook faultHook) throws IOException {
    if (baseBlockNumber < 0) {
      throw new IllegalArgumentException("baseBlockNumber must not be negative");
    }
    byte[] baseHash = copyDigest(baseBlockHash, "baseBlockHash");
    Path root = Objects.requireNonNull(suppliedRoot, "root").toAbsolutePath().normalize();
    StateArchiveHotStore store = new StateArchiveHotStore(root, formatIdentity, engine,
        maxFrozenGenerations, maxBlocks, maxEncodedBytes, yellowFrozenGenerations,
        redFrozenGenerations, dbSettings, faultHook);
    store.initialize(baseBlockNumber, baseHash);
    return store;
  }

  private void initialize(long baseBlockNumber, byte[] baseBlockHash) throws IOException {
    Files.createDirectories(generations);
    Catalog catalog;
    Path currentFile = root.resolve(CURRENT);
    if (Files.isRegularFile(currentFile, LinkOption.NOFOLLOW_LINKS)) {
      catalog = loadCatalog(currentFile);
      catalog.require(formatIdentity, engine);
    } else {
      List<Long> existing = generationIds();
      if (existing.isEmpty()) {
        createGeneration(0, baseBlockNumber, baseBlockHash, ZERO_DIGEST, null);
        persistCatalog(0);
        catalog = new Catalog(formatIdentity, engine, 0);
      } else if (existing.size() == 1 && existing.get(0) == 0) {
        GenerationMeta initial = loadGeneration(0);
        initial.requireBase(baseBlockNumber, baseBlockHash);
        persistCatalog(0);
        catalog = new Catalog(formatIdentity, engine, 0);
      } else {
        throw new ArchivePersistenceException(
            "Hot Archive CURRENT is missing with ambiguous generations");
      }
    }

    GenerationMeta selected = loadGeneration(catalog.currentGeneration);
    if (selected.sealed) {
      selected = recoverRotation(selected);
    }
    if (selected.id == 0) {
      selected.requireBase(baseBlockNumber, baseBlockHash);
    }
    loadAndValidateGenerations(selected);
    current = selected;
    writer = StateArchiveIndexDatabase.openHotWriter(
        generationPath(current.id).resolve(DATABASE),
        engine, dbSettings);
    resumeTruncateIfPresent();
  }

  /** Atomically appends a contiguous group of solidified block diffs to the current Hot DB. */
  public synchronized void appendSolidified(List<BlockReverseDiff> diffs) throws IOException {
    append(diffs, null, null);
  }

  /** Materializes one checkpoint target without advancing the reader-visible published head. */
  public synchronized void prepareCheckpoint(byte[] targetDigest, List<BlockReverseDiff> diffs)
      throws IOException {
    byte[] target = copyDigest(targetDigest, "targetDigest");
    if (Objects.requireNonNull(diffs, "diffs").isEmpty()) {
      throw new IllegalArgumentException("Hot Archive checkpoint must contain blocks");
    }
    HotCheckpointStatus status = inspectCheckpoint(target);
    if (status != HotCheckpointStatus.NEEDS_MATERIALIZATION) {
      return;
    }
    prepareCheckpoint(target, planCheckpoint(diffs), diffs);
  }

  /** Materializes only when the caller's v2 binding equals a fresh no-write plan. */
  public synchronized void prepareCheckpoint(byte[] targetDigest,
      StateArchiveHotBatchDescriptor descriptor, List<BlockReverseDiff> diffs)
      throws IOException {
    byte[] target = copyDigest(targetDigest, "targetDigest");
    StateArchiveHotBatchDescriptor supplied = Objects.requireNonNull(descriptor, "descriptor");
    if (inspectCheckpoint(target, supplied) != HotCheckpointStatus.NEEDS_MATERIALIZATION) {
      return;
    }
    if (!supplied.equals(planCheckpoint(diffs))) {
      throw new ArchivePersistenceException("Hot Archive checkpoint descriptor differs");
    }
    append(diffs, target, supplied);
  }

  /** Computes the exact logical and encoded identity of one candidate batch without writing. */
  public synchronized StateArchiveHotBatchDescriptor planCheckpoint(List<BlockReverseDiff> diffs)
      throws IOException {
    ensureOpen();
    if (current.prepared != null || current.publishedBlock != current.endBlock) {
      throw new ArchivePersistenceException(
          "Hot Archive cannot plan across an unpublished checkpoint");
    }
    return planCheckpointDescriptor(engine, current.publishedBlock, current.publishedHash,
        current.publishedContentDigest, diffs, historyCodec);
  }

  /** Pure v2 descriptor planner shared with append-file Common compatibility binding. */
  static StateArchiveHotBatchDescriptor planCheckpointDescriptor(Engine engine,
      long baseBlock, byte[] baseHash, byte[] baseContentDigest,
      List<BlockReverseDiff> diffs) {
    return planCheckpointDescriptor(engine, baseBlock, baseHash, baseContentDigest, diffs,
        new BlockHistoryCodec());
  }

  private static StateArchiveHotBatchDescriptor planCheckpointDescriptor(Engine engine,
      long baseBlock, byte[] baseHash, byte[] baseContentDigest,
      List<BlockReverseDiff> diffs, BlockHistoryCodec codec) {
    List<BlockReverseDiff> admitted = new ArrayList<>(Objects.requireNonNull(diffs, "diffs"));
    if (admitted.isEmpty()) {
      throw new IllegalArgumentException("Hot Archive checkpoint must contain blocks");
    }
    long previousBlock = baseBlock;
    byte[] previousHash = Arrays.copyOf(Objects.requireNonNull(baseHash, "baseHash"),
        baseHash.length);
    byte[] resultContentDigest = Arrays.copyOf(
        Objects.requireNonNull(baseContentDigest, "baseContentDigest"),
        baseContentDigest.length);
    long encodedBytes = 0;
    Hasher orderedRecords = Hashing.sha256().newHasher();
    List<StateArchiveHotBatchDescriptor.BlockDigest> blocks = new ArrayList<>();
    for (BlockReverseDiff diff : admitted) {
      BlockReverseDiff block = Objects.requireNonNull(diff, "diff");
      BlockSnapshotMeta meta = block.getMeta();
      if (meta.getEpoch() != meta.getBlockNumber()
          || meta.getBlockNumber() != previousBlock + 1
          || !Arrays.equals(meta.getParentHash(), previousHash)) {
        throw new IllegalArgumentException(
            "Hot Archive checkpoint identity is not contiguous");
      }
      byte[] record = encodeRecord(block, codec);
      byte[] recordDigest = Hashing.sha256().hashBytes(record).asBytes();
      encodedBytes = Math.addExact(encodedBytes, record.length);
      resultContentDigest = nextContentDigest(resultContentDigest, meta, record);
      orderedRecords.putLong(meta.getBlockNumber()).putBytes(recordDigest);
      blocks.add(new StateArchiveHotBatchDescriptor.BlockDigest(meta, recordDigest));
      previousBlock = meta.getBlockNumber();
      previousHash = meta.getBlockHash();
    }
    return new StateArchiveHotBatchDescriptor(Objects.requireNonNull(engine, "engine"), baseBlock,
        baseHash, admitted.get(0).getMeta(),
        admitted.get(admitted.size() - 1).getMeta(), encodedBytes,
        baseContentDigest, resultContentDigest,
        orderedRecords.hash().asBytes(), blocks);
  }

  private void append(List<BlockReverseDiff> diffs, byte[] preparedTarget,
      StateArchiveHotBatchDescriptor preparedDescriptor) throws IOException {
    ensureOpen();
    Objects.requireNonNull(diffs, "diffs");
    if (diffs.isEmpty()) {
      return;
    }
    if (current.sealed) {
      throw new ArchivePersistenceException("Hot Archive current generation is sealed");
    }
    if (current.prepared != null) {
      throw new ArchivePersistenceException(
          "Hot Archive cannot append across a prepared checkpoint target");
    }

    List<StateArchiveIndexDatabase.Mutation> mutations = new ArrayList<>();
    Set<ByteArrayKey> newKeys = new HashSet<>();
    long previousBlock = current.endBlock;
    byte[] previousHash = current.headHash;
    long startBlock = current.startBlock;
    long blockCount = current.blockCount;
    long encodedBytes = current.encodedBytes;
    byte[] contentDigest = current.contentDigest;
    for (BlockReverseDiff diff : diffs) {
      Objects.requireNonNull(diff, "diff");
      BlockSnapshotMeta meta = diff.getMeta();
      if (meta.getEpoch() != meta.getBlockNumber()) {
        throw new IllegalArgumentException("Hot Archive requires block-boundary epochs");
      }
      if (meta.getBlockNumber() != previousBlock + 1
          || !Arrays.equals(meta.getParentHash(), previousHash)) {
        throw new IllegalArgumentException("Hot Archive block sequence is not contiguous");
      }
      byte[] bodyKey = bodyKey(meta.getBlockNumber());
      requireNewKey(BLOCKS_COLUMN, bodyKey, newKeys);
      byte[] record = encodeRecord(diff);
      mutations.add(StateArchiveIndexDatabase.put(BLOCKS_COLUMN, bodyKey, record));
      for (DbGroup group : diff.getGroups()) {
        String storeColumn = storeColumn(group.getDbName());
        for (Entry entry : group.getEntries()) {
          byte[] indexKey = indexKey(group.getDbName(), entry.getKey(), meta.getBlockNumber());
          requireNewKey(storeColumn, indexKey, newKeys);
          mutations.add(StateArchiveIndexDatabase.put(storeColumn, indexKey,
              longBytes(meta.getBlockNumber())));
        }
      }
      if (startBlock < 0) {
        startBlock = meta.getBlockNumber();
      }
      previousBlock = meta.getBlockNumber();
      previousHash = meta.getBlockHash();
      blockCount++;
      encodedBytes = Math.addExact(encodedBytes, record.length);
      contentDigest = nextContentDigest(contentDigest, meta, record);
    }
    mutations.add(StateArchiveIndexDatabase.put(META_START_BLOCK, longBytes(startBlock)));
    mutations.add(StateArchiveIndexDatabase.put(META_END_BLOCK, longBytes(previousBlock)));
    mutations.add(StateArchiveIndexDatabase.put(META_HEAD_HASH, previousHash));
    mutations.add(StateArchiveIndexDatabase.put(META_BLOCK_COUNT, longBytes(blockCount)));
    mutations.add(StateArchiveIndexDatabase.put(META_ENCODED_BYTES, longBytes(encodedBytes)));
    mutations.add(StateArchiveIndexDatabase.put(META_CONTENT_DIGEST, contentDigest));
    PreparedTarget prepared = null;
    long publishedBlock = previousBlock;
    byte[] publishedHash = previousHash;
    byte[] publishedContentDigest = contentDigest;
    byte[] publishedTarget = current.publishedTarget;
    StateArchiveHotBatchDescriptor publishedDescriptor = current.publishedDescriptor;
    if (preparedTarget != null) {
      prepared = new PreparedTarget(preparedTarget,
          Objects.requireNonNull(preparedDescriptor, "preparedDescriptor"));
      publishedBlock = current.publishedBlock;
      publishedHash = current.publishedHash;
      publishedContentDigest = current.publishedContentDigest;
      mutations.add(StateArchiveIndexDatabase.put(META_PREPARED_TARGET, prepared.targetDigest));
      mutations.add(StateArchiveIndexDatabase.put(META_PREPARED_DESCRIPTOR,
          descriptorCodec.encode(prepared.descriptor)));
    } else {
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_BLOCK,
          longBytes(publishedBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_HASH, publishedHash));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_CONTENT_DIGEST,
          publishedContentDigest));
    }
    if (prepared != null
        && (blockCount >= maxBlocks || encodedBytes >= maxEncodedBytes)
        && frozen.size() >= maxFrozenGenerations) {
      throw new ArchivePersistenceException(
          "Hot Archive cannot prepare a rotation with a full frozen backlog");
    }
    writer.write(mutations, true);
    current = new GenerationMeta(current.id, current.baseBlock, current.baseHash, startBlock,
        previousBlock, previousHash, blockCount, encodedBytes, contentDigest, false,
        publishedBlock, publishedHash, publishedContentDigest, publishedTarget,
        publishedDescriptor, prepared);
    if (prepared != null) {
      faultHook.after(Stage.AFTER_PREPARE);
    }
  }

  /** Atomically publishes an already materialized exact checkpoint target. */
  public synchronized void publishCheckpoint(byte[] targetDigest) throws IOException {
    ensureOpen();
    byte[] target = copyDigest(targetDigest, "targetDigest");
    HotCheckpointStatus status = inspectCheckpoint(target);
    if (status == HotCheckpointStatus.PUBLISHED) {
      rotatePublishedCurrentIfDue();
      return;
    }
    if (status != HotCheckpointStatus.MATERIALIZED || current.prepared == null) {
      throw new ArchivePersistenceException(
          "Hot Archive checkpoint target is not materialized");
    }
    List<StateArchiveIndexDatabase.Mutation> mutations = Arrays.asList(
        StateArchiveIndexDatabase.put(META_PUBLISHED_BLOCK, longBytes(current.endBlock)),
        StateArchiveIndexDatabase.put(META_PUBLISHED_HASH, current.headHash),
        StateArchiveIndexDatabase.put(META_PUBLISHED_CONTENT_DIGEST, current.contentDigest),
        StateArchiveIndexDatabase.put(META_PUBLISHED_TARGET, target),
        StateArchiveIndexDatabase.put(META_PUBLISHED_DESCRIPTOR,
            descriptorCodec.encode(current.prepared.descriptor)),
        StateArchiveIndexDatabase.delete(META_PREPARED_TARGET),
        StateArchiveIndexDatabase.delete(META_PREPARED_DESCRIPTOR));
    writer.write(mutations, true);
    current = current.published(target, current.prepared.descriptor);
    faultHook.after(Stage.AFTER_PUBLISH);
    rotatePublishedCurrentIfDue();
  }

  synchronized void requireFormatIdentity(byte[] expected) throws IOException {
    ensureOpen();
    if (!Arrays.equals(formatIdentity, copyDigest(expected, "formatIdentity"))) {
      throw new ArchivePersistenceException("Hot Archive checkpoint format differs");
    }
  }

  private void rotatePublishedCurrentIfDue() throws IOException {
    if (shouldSealCurrent()) {
      sealCurrent();
    }
  }

  public synchronized HotCheckpointStatus inspectCheckpoint(byte[] targetDigest)
      throws IOException {
    ensureOpen();
    byte[] target = copyDigest(targetDigest, "targetDigest");
    if (Arrays.equals(current.publishedTarget, target)) {
      return HotCheckpointStatus.PUBLISHED;
    }
    if (current.prepared != null) {
      if (Arrays.equals(current.prepared.targetDigest, target)) {
        return HotCheckpointStatus.MATERIALIZED;
      }
      throw new ArchivePersistenceException(
          "Hot Archive prepared checkpoint target differs");
    }
    return HotCheckpointStatus.NEEDS_MATERIALIZATION;
  }

  public synchronized HotCheckpointStatus inspectCheckpoint(byte[] targetDigest,
      StateArchiveHotBatchDescriptor descriptor) throws IOException {
    StateArchiveHotBatchDescriptor expected = Objects.requireNonNull(descriptor, "descriptor");
    HotCheckpointStatus status = inspectCheckpoint(targetDigest);
    if (status == HotCheckpointStatus.MATERIALIZED
        && !expected.equals(current.prepared.descriptor)) {
      throw new ArchivePersistenceException("Hot Archive prepared descriptor differs");
    }
    if (status == HotCheckpointStatus.PUBLISHED
        && !expected.equals(current.publishedDescriptor)) {
      throw new ArchivePersistenceException("Hot Archive published descriptor differs");
    }
    return status;
  }

  public synchronized void publishCheckpoint(byte[] targetDigest,
      StateArchiveHotBatchDescriptor descriptor) throws IOException {
    HotCheckpointStatus status = inspectCheckpoint(targetDigest, descriptor);
    if (status == HotCheckpointStatus.NEEDS_MATERIALIZATION) {
      throw new ArchivePersistenceException("Hot Archive checkpoint target is not materialized");
    }
    publishCheckpoint(targetDigest);
  }

  /** Seals current and atomically publishes a new writable generation. */
  public synchronized long sealCurrent() throws IOException {
    ensureOpen();
    if (current.blockCount == 0) {
      throw new IllegalStateException("Hot Archive cannot seal an empty generation");
    }
    if (current.prepared != null || current.publishedBlock != current.endBlock) {
      throw new ArchivePersistenceException(
          "Hot Archive cannot seal an unpublished current generation");
    }
    if (frozen.size() >= maxFrozenGenerations) {
      throw new ArchivePersistenceException("Hot Archive frozen backlog reached its limit");
    }
    writer.write(Collections.singletonList(
        StateArchiveIndexDatabase.put(META_SEALED, new byte[]{1})), true);
    current = current.sealed();
    writer.close();
    writer = null;
    faultHook.after(Stage.AFTER_SEAL);

    long frozenId = current.id;
    long nextId = Math.addExact(frozenId, 1);
    GenerationMeta next = createOrValidateNext(current, nextId);
    faultHook.after(Stage.AFTER_NEW_GENERATION);
    persistCatalog(nextId);
    faultHook.after(Stage.AFTER_CURRENT);
    frozen.add(current);
    current = next;
    writer = StateArchiveIndexDatabase.openHotWriter(generationPath(nextId).resolve(DATABASE),
        engine, dbSettings);
    return frozenId;
  }

  public synchronized boolean shouldSealCurrent() {
    ensureOpenUnchecked();
    return current.blockCount >= maxBlocks || current.encodedBytes >= maxEncodedBytes;
  }

  /** Finds the first changed block after {@code targetBlock} across frozen and current Hot DBs. */
  public synchronized Optional<HotLookup> findOldValueAfter(String dbName, byte[] rawKey,
      long targetBlock) throws IOException {
    ensureOpen();
    Objects.requireNonNull(dbName, "dbName");
    Objects.requireNonNull(rawKey, "rawKey");
    if (targetBlock < -1) {
      throw new IllegalArgumentException("targetBlock must not be less than -1");
    }
    if (targetBlock == Long.MAX_VALUE) {
      return Optional.empty();
    }
    byte[] prefix = indexPrefix(dbName, rawKey);
    String storeColumn;
    try {
      storeColumn = storeColumn(dbName);
    } catch (IllegalArgumentException unknownStore) {
      return Optional.empty();
    }
    byte[] seek = ByteBuffer.allocate(prefix.length + Long.BYTES).put(prefix)
        .putLong(targetBlock + 1).array();
    long candidate = Long.MAX_VALUE;
    for (GenerationMeta generation : allGenerations()) {
      if (generation.blockCount == 0 || generation.publishedBlock <= targetBlock) {
        continue;
      }
      try (StateArchiveIndexDatabase.Reader reader = StateArchiveIndexDatabase.openHotReader(
          generationPath(generation.id).resolve(DATABASE), engine, dbSettings)) {
        StateArchiveIndexDatabase.KeyValue found = reader.seek(storeColumn, seek);
        if (found != null && isIndexCandidate(found.getKey(), prefix)) {
          long block = ByteBuffer.wrap(found.getKey(), prefix.length, Long.BYTES).getLong();
          if (!Arrays.equals(found.getValue(), longBytes(block))) {
            throw new ArchivePersistenceException("Hot Archive index locator is corrupt");
          }
          if (block <= generation.publishedBlock) {
            candidate = Math.min(candidate, block);
          }
        }
      }
    }
    if (candidate == Long.MAX_VALUE) {
      return Optional.empty();
    }
    BlockReverseDiff diff = loadBlock(candidate);
    return Optional.of(new HotLookup(candidate, findExactOldValue(diff, dbName, rawKey)));
  }

  public synchronized BlockReverseDiff loadBlock(long blockNumber) throws IOException {
    ensureOpen();
    for (GenerationMeta generation : allGenerations()) {
      if (generation.blockCount == 0 || blockNumber < generation.startBlock
          || blockNumber > generation.publishedBlock) {
        continue;
      }
      try (StateArchiveIndexDatabase.Reader reader = StateArchiveIndexDatabase.openHotReader(
          generationPath(generation.id).resolve(DATABASE), engine, dbSettings)) {
        byte[] encoded = reader.get(BLOCKS_COLUMN, bodyKey(blockNumber));
        if (encoded == null) {
          throw new ArchivePersistenceException("Hot Archive body is missing for indexed block");
        }
        return decodeRecord(encoded);
      }
    }
    throw new ArchivePersistenceException("Hot Archive block is outside committed coverage");
  }

  public synchronized long getCurrentGenerationId() {
    ensureOpenUnchecked();
    return current.id;
  }

  public synchronized List<Long> getFrozenGenerationIds() {
    ensureOpenUnchecked();
    return frozen.stream().map(generation -> generation.id).collect(Collectors.toList());
  }

  public synchronized long getCommittedHead() {
    ensureOpenUnchecked();
    return current.publishedBlock;
  }

  public synchronized byte[] getCommittedHeadHash() {
    ensureOpenUnchecked();
    return Arrays.copyOf(current.publishedHash, current.publishedHash.length);
  }

  public synchronized Optional<byte[]> getPublishedTargetDigest() {
    ensureOpenUnchecked();
    return current.publishedTarget == null ? Optional.empty()
        : Optional.of(Arrays.copyOf(current.publishedTarget, current.publishedTarget.length));
  }

  public synchronized long getMaterializedHead() {
    ensureOpenUnchecked();
    return current.endBlock;
  }

  /**
   * Removes only the writable generation suffix above an externally recovered authority.
   * Frozen generations are never changed, and equal-height identity drift fails closed.
   */
  public synchronized long reconcilePreparedTail(BlockSnapshotMeta authority) throws IOException {
    ensureOpen();
    BlockSnapshotMeta admitted = Objects.requireNonNull(authority, "authority");
    long ceiling = admitted.getBlockNumber();
    if (ceiling < current.baseBlock) {
      throw new ArchivePersistenceException(
          "Hot Archive recovery ceiling precedes the current generation base");
    }
    if (!frozen.isEmpty() && frozen.get(frozen.size() - 1).endBlock > ceiling) {
      throw new ArchivePersistenceException(
          "Hot Archive recovery cannot truncate a frozen generation");
    }
    if (current.publishedBlock > ceiling) {
      throw new ArchivePersistenceException(
          "Hot Archive recovery cannot truncate published history");
    }
    if (current.endBlock < ceiling) {
      return 0;
    }
    requireCurrentIdentity(ceiling, admitted.getBlockHash());
    if (current.endBlock == ceiling) {
      return 0;
    }
    if (current.prepared == null || current.publishedBlock != ceiling) {
      throw new ArchivePersistenceException(
          "Hot Archive recovery ceiling does not bound one prepared tail");
    }
    writer.write(Arrays.asList(
        StateArchiveIndexDatabase.put(META_TRUNCATE_BLOCK, longBytes(ceiling)),
        StateArchiveIndexDatabase.put(META_TRUNCATE_HASH, admitted.getBlockHash())), true);
    faultHook.after(Stage.AFTER_TRUNCATE_INTENT);
    return completeTruncate(new RecoveryCeiling(ceiling, admitted.getBlockHash()));
  }

  /** Returns a self-consistent Hot DB capacity and frozen-backlog snapshot. */
  public synchronized Statistics getStatistics() {
    ensureOpenUnchecked();
    long frozenBlocks = 0;
    long frozenBytes = 0;
    for (GenerationMeta generation : frozen) {
      frozenBlocks = Math.addExact(frozenBlocks, generation.blockCount);
      frozenBytes = Math.addExact(frozenBytes, generation.encodedBytes);
    }
    int frozenCount = frozen.size();
    BacklogLevel level = frozenCount >= redFrozenGenerations ? BacklogLevel.RED
        : frozenCount >= yellowFrozenGenerations ? BacklogLevel.YELLOW : BacklogLevel.GREEN;
    return new Statistics(current.id, current.startBlock, current.endBlock, current.blockCount,
        current.encodedBytes, frozenCount, frozenBlocks, frozenBytes,
        maxFrozenGenerations, yellowFrozenGenerations, redFrozenGenerations, level,
        shouldSealCurrent(), maxBlocks, maxEncodedBytes, current.publishedBlock,
        current.prepared != null);
  }

  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      closed = true;
      if (writer != null) {
        writer.close();
        writer = null;
      }
    }
  }

  private GenerationMeta recoverRotation(GenerationMeta sealed) throws IOException {
    if (sealed.blockCount == 0) {
      throw new ArchivePersistenceException("Hot Archive sealed generation is empty");
    }
    long nextId = Math.addExact(sealed.id, 1);
    GenerationMeta next = createOrValidateNext(sealed, nextId);
    persistCatalog(nextId);
    return next;
  }

  private void resumeTruncateIfPresent() throws IOException {
    byte[] block = writer.get(META_TRUNCATE_BLOCK);
    byte[] hash = writer.get(META_TRUNCATE_HASH);
    if (block == null && hash == null) {
      return;
    }
    if (block == null || block.length != Long.BYTES || hash == null
        || hash.length != HASH_LENGTH) {
      throw new ArchivePersistenceException("Hot Archive truncate intent is corrupt");
    }
    RecoveryCeiling ceiling = new RecoveryCeiling(ByteBuffer.wrap(block).getLong(), hash);
    if (ceiling.blockNumber < current.baseBlock || ceiling.blockNumber >= current.endBlock) {
      throw new ArchivePersistenceException("Hot Archive truncate intent range is invalid");
    }
    if (current.prepared == null || current.publishedBlock != ceiling.blockNumber) {
      throw new ArchivePersistenceException(
          "Hot Archive truncate intent does not bound one prepared tail");
    }
    requireCurrentIdentity(ceiling.blockNumber, ceiling.blockHash);
    completeTruncate(ceiling);
  }

  private long completeTruncate(RecoveryCeiling ceiling) throws IOException {
    GenerationMeta retained = rebuildCurrentThrough(ceiling.blockNumber)
        .published(current.publishedTarget, current.publishedDescriptor);
    long removed = current.endBlock - ceiling.blockNumber;
    for (long block = current.endBlock; block > ceiling.blockNumber; block--) {
      byte[] body = writer.get(BLOCKS_COLUMN, bodyKey(block));
      if (body == null) {
        continue;
      }
      BlockReverseDiff diff = decodeRecord(body);
      if (diff.getMeta().getBlockNumber() != block) {
        throw new ArchivePersistenceException(
            "Hot Archive truncate body identity differs");
      }
      List<StateArchiveIndexDatabase.Mutation> deletes = new ArrayList<>();
      for (DbGroup group : diff.getGroups()) {
        for (Entry entry : group.getEntries()) {
          deletes.add(StateArchiveIndexDatabase.delete(storeColumn(group.getDbName()),
              indexKey(group.getDbName(), entry.getKey(), block)));
        }
      }
      deletes.add(StateArchiveIndexDatabase.delete(BLOCKS_COLUMN, bodyKey(block)));
      writer.write(deletes, true);
      faultHook.after(Stage.AFTER_TRUNCATE_DELETE_BATCH);
    }
    List<StateArchiveIndexDatabase.Mutation> publish = retained.mutableMetadata();
    publish.add(StateArchiveIndexDatabase.delete(META_TRUNCATE_BLOCK));
    publish.add(StateArchiveIndexDatabase.delete(META_TRUNCATE_HASH));
    publish.add(StateArchiveIndexDatabase.delete(META_PREPARED_TARGET));
    publish.add(StateArchiveIndexDatabase.delete(META_PREPARED_DESCRIPTOR));
    writer.write(publish, true);
    current = retained;
    faultHook.after(Stage.AFTER_TRUNCATE_METADATA);
    return removed;
  }

  private GenerationMeta rebuildCurrentThrough(long ceiling) throws IOException {
    GenerationMeta retained = GenerationMeta.empty(current.id, current.baseBlock,
        current.baseHash);
    for (long block = current.baseBlock + 1; block <= ceiling; block++) {
      byte[] record = writer.get(BLOCKS_COLUMN, bodyKey(block));
      if (record == null) {
        throw new ArchivePersistenceException(
            "Hot Archive retained prefix body is missing");
      }
      BlockReverseDiff diff = decodeRecord(record);
      BlockSnapshotMeta meta = diff.getMeta();
      if (meta.getBlockNumber() != block
          || !Arrays.equals(meta.getParentHash(), retained.headHash)) {
        throw new ArchivePersistenceException(
            "Hot Archive retained prefix identity differs");
      }
      retained = retained.appended(meta, record);
    }
    return retained;
  }

  private void requireCurrentIdentity(long blockNumber, byte[] expectedHash) throws IOException {
    byte[] actual;
    if (blockNumber == current.baseBlock) {
      actual = current.baseHash;
    } else {
      byte[] record = writer.get(BLOCKS_COLUMN, bodyKey(blockNumber));
      if (record == null) {
        throw new ArchivePersistenceException(
            "Hot Archive recovery ceiling body is missing");
      }
      actual = decodeRecord(record).getMeta().getBlockHash();
    }
    if (!Arrays.equals(actual, expectedHash)) {
      throw new ArchivePersistenceException(
          "Hot Archive recovery ceiling hash differs");
    }
  }

  private GenerationMeta createOrValidateNext(GenerationMeta parent, long nextId)
      throws IOException {
    Path path = generationPath(nextId);
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      GenerationMeta existing = loadGeneration(nextId);
      existing.requireBase(parent.endBlock, parent.headHash);
      existing.requirePublication(parent.publishedTarget, parent.publishedDescriptor);
      if (existing.sealed || existing.blockCount != 0) {
        throw new ArchivePersistenceException("Hot Archive recovery child is not empty current");
      }
      return existing;
    }
    return createGeneration(nextId, parent.endBlock, parent.headHash, parent.publishedTarget,
        parent.publishedDescriptor);
  }

  private GenerationMeta createGeneration(long id, long baseBlock, byte[] baseHash,
      byte[] publishedTarget, StateArchiveHotBatchDescriptor publishedDescriptor)
      throws IOException {
    Path path = generationPath(id);
    Files.createDirectory(path);
    StateArchiveIndexEngineManifest.openOrCreate(path, engine);
    GenerationMeta meta = GenerationMeta.empty(id, baseBlock, baseHash)
        .published(publishedTarget, publishedDescriptor);
    try (StateArchiveIndexDatabase.Writer created = StateArchiveIndexDatabase.openHotWriter(
        path.resolve(DATABASE), engine, dbSettings)) {
      created.write(meta.createMutations(formatIdentity), true);
    }
    HistorySegmentStore.syncDirectory(generations);
    return meta;
  }

  private void loadAndValidateGenerations(GenerationMeta selected) throws IOException {
    frozen.clear();
    List<Long> ids = generationIds();
    GenerationMeta previous = null;
    boolean foundCurrent = false;
    for (long id : ids) {
      if (id > selected.id) {
        throw new ArchivePersistenceException("Hot Archive has unpublished future generation");
      }
      GenerationMeta meta = id == selected.id ? selected : loadGeneration(id);
      if (previous != null) {
        meta.requireBase(previous.endBlock, previous.headHash);
      }
      if (id == selected.id) {
        if (meta.sealed) {
          throw new ArchivePersistenceException("Hot Archive CURRENT points to sealed generation");
        }
        foundCurrent = true;
      } else {
        if (!meta.sealed || meta.blockCount == 0) {
          throw new ArchivePersistenceException("Hot Archive frozen generation is not sealed");
        }
        frozen.add(meta);
      }
      previous = meta;
    }
    if (!foundCurrent || frozen.size() > maxFrozenGenerations) {
      throw new ArchivePersistenceException("Hot Archive generation catalog is inconsistent");
    }
  }

  private GenerationMeta loadGeneration(long id) throws IOException {
    Path path = generationPath(id);
    StateArchiveIndexEngineManifest.require(path, engine);
    try (StateArchiveIndexDatabase.Reader reader = StateArchiveIndexDatabase.openHotReader(
        path.resolve(DATABASE), engine, dbSettings)) {
      byte[] storedFormat = required(reader, META_FORMAT, HASH_LENGTH);
      if (!Arrays.equals(formatIdentity, storedFormat)) {
        throw new ArchivePersistenceException("Hot Archive generation format differs");
      }
      long storedId = readLong(reader, META_ID);
      if (storedId != id) {
        throw new ArchivePersistenceException("Hot Archive generation id differs");
      }
      long baseBlock = readLong(reader, META_BASE_BLOCK);
      byte[] baseHash = required(reader, META_BASE_HASH, HASH_LENGTH);
      long startBlock = readLong(reader, META_START_BLOCK);
      long endBlock = readLong(reader, META_END_BLOCK);
      byte[] headHash = required(reader, META_HEAD_HASH, HASH_LENGTH);
      long blockCount = readLong(reader, META_BLOCK_COUNT);
      long encodedBytes = readLong(reader, META_ENCODED_BYTES);
      byte[] contentDigest = required(reader, META_CONTENT_DIGEST, HASH_LENGTH);
      byte[] sealed = required(reader, META_SEALED, 1);
      long publishedBlock = readLong(reader, META_PUBLISHED_BLOCK);
      byte[] publishedHash = required(reader, META_PUBLISHED_HASH, HASH_LENGTH);
      byte[] publishedContentDigest = required(reader, META_PUBLISHED_CONTENT_DIGEST,
          HASH_LENGTH);
      byte[] publishedTarget = required(reader, META_PUBLISHED_TARGET, HASH_LENGTH);
      byte[] encodedPublishedDescriptor = reader.get(META_PUBLISHED_DESCRIPTOR);
      StateArchiveHotBatchDescriptor publishedDescriptor = encodedPublishedDescriptor == null
          ? null : descriptorCodec.decode(encodedPublishedDescriptor);
      byte[] preparedTarget = reader.get(META_PREPARED_TARGET);
      byte[] encodedPreparedDescriptor = reader.get(META_PREPARED_DESCRIPTOR);
      if ((preparedTarget == null) != (encodedPreparedDescriptor == null)
          || preparedTarget != null && preparedTarget.length != HASH_LENGTH) {
        throw new ArchivePersistenceException("Hot Archive prepared target is corrupt");
      }
      PreparedTarget prepared = preparedTarget == null ? null
          : new PreparedTarget(preparedTarget,
              descriptorCodec.decode(encodedPreparedDescriptor));
      if (sealed[0] != 0 && sealed[0] != 1) {
        throw new ArchivePersistenceException("Hot Archive sealed flag is invalid");
      }
      GenerationMeta meta = new GenerationMeta(id, baseBlock, baseHash, startBlock, endBlock,
          headHash, blockCount, encodedBytes, contentDigest, sealed[0] == 1,
          publishedBlock, publishedHash, publishedContentDigest, publishedTarget,
          publishedDescriptor, prepared);
      meta.validate();
      if (publishedDescriptor != null && publishedDescriptor.getEngine() != engine) {
        throw new ArchivePersistenceException("Hot Archive published descriptor engine differs");
      }
      if (publishedDescriptor != null && blockCount > 0
          && publishedDescriptor.getFirstBlock().getBlockNumber() >= startBlock) {
        requireExactDescriptor(reader, publishedDescriptor);
      }
      boolean truncateIntent = reader.get(META_TRUNCATE_BLOCK) != null
          || reader.get(META_TRUNCATE_HASH) != null;
      if (prepared != null && !truncateIntent) {
        requireExactDescriptor(reader, prepared.descriptor);
      }
      return meta;
    }
  }

  private void requireExactDescriptor(StateArchiveIndexDatabase.Reader reader,
      StateArchiveHotBatchDescriptor descriptor) throws IOException {
    if (descriptor.getEngine() != engine) {
      throw new ArchivePersistenceException("Hot Archive descriptor engine differs");
    }
    long encodedBytes = 0;
    byte[] contentDigest = descriptor.getParentContentDigest();
    for (StateArchiveHotBatchDescriptor.BlockDigest block : descriptor.getBlocks()) {
      byte[] record = reader.get(BLOCKS_COLUMN, bodyKey(block.getMeta().getBlockNumber()));
      if (record == null) {
        throw new ArchivePersistenceException("Hot Archive descriptor body is missing");
      }
      BlockReverseDiff diff = decodeRecord(record);
      if (!diff.getMeta().equals(block.getMeta())
          || !Arrays.equals(Hashing.sha256().hashBytes(record).asBytes(),
          block.getArchiveRecordDigest())) {
        throw new ArchivePersistenceException("Hot Archive descriptor body identity differs");
      }
      encodedBytes = Math.addExact(encodedBytes, record.length);
      contentDigest = nextContentDigest(contentDigest, diff.getMeta(), record);
    }
    if (encodedBytes != descriptor.getEncodedBytes()
        || !Arrays.equals(contentDigest, descriptor.getResultContentDigest())) {
      throw new ArchivePersistenceException("Hot Archive descriptor batch result differs");
    }
  }

  private List<Long> generationIds() throws IOException {
    try (Stream<Path> paths = Files.list(generations)) {
      List<Long> ids = new ArrayList<>();
      for (Path path : paths.collect(Collectors.toList())) {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
          throw new ArchivePersistenceException("Hot Archive generations contains a non-directory");
        }
        String name = path.getFileName().toString();
        if (!name.matches("[0-9]{20}")) {
          throw new ArchivePersistenceException("Hot Archive generation name is invalid");
        }
        try {
          ids.add(Long.parseLong(name));
        } catch (NumberFormatException invalid) {
          throw new ArchivePersistenceException("Hot Archive generation id overflows", invalid);
        }
      }
      ids.sort(Comparator.naturalOrder());
      return ids;
    }
  }

  private List<GenerationMeta> allGenerations() {
    List<GenerationMeta> result = new ArrayList<>(frozen);
    result.add(current);
    return result;
  }

  private Path generationPath(long id) {
    return generations.resolve(String.format("%020d", id));
  }

  private void persistCatalog(long currentId) throws IOException {
    byte[] encoded = new Catalog(formatIdentity, engine, currentId).encode();
    Path temporary = root.resolve(CURRENT_TEMP);
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(encoded);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
    try {
      Files.move(temporary, root.resolve(CURRENT), StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new ArchivePersistenceException(
          "Hot Archive filesystem does not support atomic CURRENT publication", unsupported);
    }
    HistorySegmentStore.syncDirectory(root);
  }

  private static Catalog loadCatalog(Path path) throws IOException {
    byte[] encoded = Files.readAllBytes(path);
    if (encoded.length != CATALOG_LENGTH) {
      throw new ArchivePersistenceException("Hot Archive CURRENT length is invalid");
    }
    byte[] payload = Arrays.copyOf(encoded, encoded.length - Integer.BYTES);
    int checksum = ByteBuffer.wrap(encoded, payload.length, Integer.BYTES).getInt();
    if (checksum != Hashing.crc32c().hashBytes(payload).asInt()) {
      throw new ArchivePersistenceException("Hot Archive CURRENT checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
      if (input.readInt() != CATALOG_MAGIC || input.readShort() != CATALOG_VERSION) {
        throw new ArchivePersistenceException("Hot Archive CURRENT format is unsupported");
      }
      Engine engine = engine(input.readUnsignedShort());
      long generation = input.readLong();
      byte[] format = new byte[HASH_LENGTH];
      input.readFully(format);
      if (generation < 0 || input.available() != 0) {
        throw new ArchivePersistenceException("Hot Archive CURRENT payload is invalid");
      }
      return new Catalog(format, engine, generation);
    } catch (EOFException truncated) {
      throw new ArchivePersistenceException("Hot Archive CURRENT is truncated", truncated);
    }
  }

  private byte[] encodeRecord(BlockReverseDiff diff) {
    return encodeRecord(diff, historyCodec);
  }

  private static byte[] encodeRecord(BlockReverseDiff diff, BlockHistoryCodec codec) {
    try {
      byte[] history = codec.encode(diff);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(history.length + 16);
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(RECORD_MAGIC);
      output.writeShort(RECORD_VERSION);
      output.writeShort(0);
      output.writeInt(history.length);
      output.write(history);
      output.flush();
      byte[] payload = bytes.toByteArray();
      output.writeInt(Hashing.crc32c().hashBytes(payload).asInt());
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("Unexpected Hot Archive record encoding failure", impossible);
    }
  }

  private BlockReverseDiff decodeRecord(byte[] encoded) throws IOException {
    if (encoded.length < 16) {
      throw new ArchivePersistenceException("Hot Archive record is truncated");
    }
    byte[] payload = Arrays.copyOf(encoded, encoded.length - Integer.BYTES);
    int checksum = ByteBuffer.wrap(encoded, payload.length, Integer.BYTES).getInt();
    if (checksum != Hashing.crc32c().hashBytes(payload).asInt()) {
      throw new ArchivePersistenceException("Hot Archive record checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
      if (input.readInt() != RECORD_MAGIC || input.readShort() != RECORD_VERSION) {
        throw new ArchivePersistenceException("Hot Archive record format is unsupported");
      }
      short flags = input.readShort();
      if (flags != 0) {
        throw new ArchivePersistenceException("Hot Archive record flags are unsupported");
      }
      int historyLength = input.readInt();
      if (historyLength <= 0 || historyLength != input.available()) {
        throw new ArchivePersistenceException("Hot Archive history length is invalid");
      }
      byte[] history = new byte[historyLength];
      input.readFully(history);
      BlockReverseDiff decoded;
      try {
        decoded = historyCodec.decode(history);
      } catch (IllegalArgumentException invalid) {
        throw new ArchivePersistenceException("Hot Archive history is corrupt", invalid);
      }
      return decoded;
    } catch (EOFException truncated) {
      throw new ArchivePersistenceException("Hot Archive record is truncated", truncated);
    }
  }

  private void requireNewKey(String columnFamily, byte[] key, Set<ByteArrayKey> newKeys)
      throws IOException {
    byte[] column = columnFamily.getBytes(StandardCharsets.UTF_8);
    byte[] qualified = ByteBuffer.allocate(Integer.BYTES + column.length + key.length)
        .putInt(column.length).put(column).put(key).array();
    if (!newKeys.add(new ByteArrayKey(qualified)) || writer.get(columnFamily, key) != null) {
      throw new ArchivePersistenceException("Hot Archive append would overwrite existing data");
    }
  }

  private static String storeColumn(String dbName) {
    return StateArchiveIndexDatabase.hotStoreColumnFamily(dbName);
  }

  private static OldValue findExactOldValue(BlockReverseDiff diff, String dbName, byte[] rawKey)
      throws IOException {
    for (DbGroup group : diff.getGroups()) {
      if (!group.getDbName().equals(dbName)) {
        continue;
      }
      for (Entry entry : group.getEntries()) {
        if (Arrays.equals(entry.getKey(), rawKey)) {
          return entry.getOldValue();
        }
      }
    }
    throw new ArchivePersistenceException("Hot Archive index does not match authoritative body");
  }

  private static boolean isIndexCandidate(byte[] key, byte[] prefix) {
    return key.length == prefix.length + Long.BYTES
        && Arrays.equals(prefix, Arrays.copyOf(key, prefix.length));
  }

  private static byte[] bodyKey(long blockNumber) {
    return ByteBuffer.allocate(BODY_PREFIX.length + Long.BYTES).put(BODY_PREFIX)
        .putLong(blockNumber).array();
  }

  private static byte[] indexKey(String dbName, byte[] rawKey, long blockNumber) {
    byte[] prefix = indexPrefix(dbName, rawKey);
    return ByteBuffer.allocate(prefix.length + Long.BYTES).put(prefix).putLong(blockNumber).array();
  }

  private static byte[] indexPrefix(String dbName, byte[] rawKey) {
    byte[] name = dbName.getBytes(StandardCharsets.UTF_8);
    if (name.length == 0 || name.length > 0xffff) {
      throw new IllegalArgumentException("Hot Archive dbName length is invalid");
    }
    return ByteBuffer.allocate(INDEX_PREFIX.length + Short.BYTES + name.length
        + Integer.BYTES + rawKey.length).put(INDEX_PREFIX).putShort((short) name.length)
        .put(name).putInt(rawKey.length).put(rawKey).array();
  }

  private static byte[] nextContentDigest(byte[] previous, BlockSnapshotMeta meta, byte[] record) {
    return Hashing.sha256().newHasher().putBytes(previous).putLong(meta.getBlockNumber())
        .putBytes(meta.getBlockHash()).putBytes(Hashing.sha256().hashBytes(record).asBytes())
        .hash().asBytes();
  }

  private static byte[] required(StateArchiveIndexDatabase.Reader reader, byte[] key, int length)
      throws IOException {
    byte[] value = reader.get(key);
    if (value == null || value.length != length) {
      throw new ArchivePersistenceException("Hot Archive generation metadata is missing");
    }
    return value;
  }

  private static long readLong(StateArchiveIndexDatabase.Reader reader, byte[] key)
      throws IOException {
    return ByteBuffer.wrap(required(reader, key, Long.BYTES)).getLong();
  }

  private static byte[] longBytes(long value) {
    return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }

  private static int positive(int value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  private static long positive(long value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  private static byte[] copyDigest(byte[] value, String name) {
    Objects.requireNonNull(value, name);
    if (value.length != HASH_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return Arrays.copyOf(value, value.length);
  }

  private void ensureOpen() throws IOException {
    if (closed || writer == null) {
      throw new IOException("Hot Archive store is closed or requires recovery");
    }
  }

  private void ensureOpenUnchecked() {
    if (closed || writer == null) {
      throw new IllegalStateException("Hot Archive store is closed or requires recovery");
    }
  }

  public static final class HotLookup {
    private final long blockNumber;
    private final OldValue oldValue;

    private HotLookup(long blockNumber, OldValue oldValue) {
      this.blockNumber = blockNumber;
      this.oldValue = oldValue;
    }

    public long getBlockNumber() {
      return blockNumber;
    }

    public OldValue getOldValue() {
      return oldValue;
    }
  }

  public enum BacklogLevel {
    GREEN,
    YELLOW,
    RED
  }

  public enum HotCheckpointStatus {
    NEEDS_MATERIALIZATION,
    MATERIALIZED,
    PUBLISHED
  }

  /** Immutable values suitable for logging, gauges, or a future management API. */
  public static final class Statistics {
    private final long currentGenerationId;
    private final long currentStartBlock;
    private final long currentEndBlock;
    private final long currentBlocks;
    private final long currentEncodedBytes;
    private final int frozenGenerations;
    private final long frozenBlocks;
    private final long frozenEncodedBytes;
    private final int maxFrozenGenerations;
    private final int yellowFrozenGenerations;
    private final int redFrozenGenerations;
    private final BacklogLevel backlogLevel;
    private final boolean rotationDue;
    private final long maxBlocks;
    private final long maxEncodedBytes;
    private final long publishedBlock;
    private final boolean prepared;

    private Statistics(long currentGenerationId, long currentStartBlock, long currentEndBlock,
        long currentBlocks, long currentEncodedBytes, int frozenGenerations, long frozenBlocks,
        long frozenEncodedBytes, int maxFrozenGenerations, int yellowFrozenGenerations,
        int redFrozenGenerations, BacklogLevel backlogLevel, boolean rotationDue, long maxBlocks,
        long maxEncodedBytes, long publishedBlock, boolean prepared) {
      this.currentGenerationId = currentGenerationId;
      this.currentStartBlock = currentStartBlock;
      this.currentEndBlock = currentEndBlock;
      this.currentBlocks = currentBlocks;
      this.currentEncodedBytes = currentEncodedBytes;
      this.frozenGenerations = frozenGenerations;
      this.frozenBlocks = frozenBlocks;
      this.frozenEncodedBytes = frozenEncodedBytes;
      this.maxFrozenGenerations = maxFrozenGenerations;
      this.yellowFrozenGenerations = yellowFrozenGenerations;
      this.redFrozenGenerations = redFrozenGenerations;
      this.backlogLevel = backlogLevel;
      this.rotationDue = rotationDue;
      this.maxBlocks = maxBlocks;
      this.maxEncodedBytes = maxEncodedBytes;
      this.publishedBlock = publishedBlock;
      this.prepared = prepared;
    }

    public long getCurrentGenerationId() {
      return currentGenerationId;
    }

    public long getCurrentStartBlock() {
      return currentStartBlock;
    }

    public long getCurrentEndBlock() {
      return currentEndBlock;
    }

    public long getCurrentBlocks() {
      return currentBlocks;
    }

    public long getCurrentEncodedBytes() {
      return currentEncodedBytes;
    }

    public int getFrozenGenerations() {
      return frozenGenerations;
    }

    public long getFrozenBlocks() {
      return frozenBlocks;
    }

    public long getFrozenEncodedBytes() {
      return frozenEncodedBytes;
    }

    public int getMaxFrozenGenerations() {
      return maxFrozenGenerations;
    }

    public int getYellowFrozenGenerations() {
      return yellowFrozenGenerations;
    }

    public int getRedFrozenGenerations() {
      return redFrozenGenerations;
    }

    public BacklogLevel getBacklogLevel() {
      return backlogLevel;
    }

    public boolean isRotationDue() {
      return rotationDue;
    }

    public long getMaxBlocks() {
      return maxBlocks;
    }

    public long getMaxEncodedBytes() {
      return maxEncodedBytes;
    }

    public long getPublishedBlock() {
      return publishedBlock;
    }

    public boolean hasPreparedCheckpoint() {
      return prepared;
    }
  }

  enum Stage {
    AFTER_SEAL,
    AFTER_NEW_GENERATION,
    AFTER_CURRENT,
    AFTER_PREPARE,
    AFTER_PUBLISH,
    AFTER_TRUNCATE_INTENT,
    AFTER_TRUNCATE_DELETE_BATCH,
    AFTER_TRUNCATE_METADATA
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage) throws IOException;
  }

  private static final class RecoveryCeiling {
    private final long blockNumber;
    private final byte[] blockHash;

    private RecoveryCeiling(long blockNumber, byte[] blockHash) {
      this.blockNumber = blockNumber;
      this.blockHash = copyDigest(blockHash, "blockHash");
    }
  }

  private static final class PreparedTarget {
    private final byte[] targetDigest;
    private final StateArchiveHotBatchDescriptor descriptor;

    private PreparedTarget(byte[] targetDigest, StateArchiveHotBatchDescriptor descriptor) {
      this.targetDigest = copyDigest(targetDigest, "targetDigest");
      this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }
  }

  private static final class Catalog {
    private final byte[] formatIdentity;
    private final Engine engine;
    private final long currentGeneration;

    private Catalog(byte[] formatIdentity, Engine engine, long currentGeneration) {
      this.formatIdentity = copyDigest(formatIdentity, "formatIdentity");
      this.engine = Objects.requireNonNull(engine, "engine");
      this.currentGeneration = currentGeneration;
    }

    private void require(byte[] expectedFormat, Engine expectedEngine) throws IOException {
      if (!Arrays.equals(formatIdentity, expectedFormat) || engine != expectedEngine) {
        throw new ArchivePersistenceException("Hot Archive CURRENT identity differs");
      }
    }

    private byte[] encode() {
      ByteBuffer payload = ByteBuffer.allocate(CATALOG_LENGTH - Integer.BYTES)
          .putInt(CATALOG_MAGIC).putShort(CATALOG_VERSION).putShort((short) engineTag(engine))
          .putLong(currentGeneration).put(formatIdentity);
      byte[] bytes = payload.array();
      return ByteBuffer.allocate(CATALOG_LENGTH).put(bytes)
          .putInt(Hashing.crc32c().hashBytes(bytes).asInt()).array();
    }
  }

  private static final class GenerationMeta {
    private final long id;
    private final long baseBlock;
    private final byte[] baseHash;
    private final long startBlock;
    private final long endBlock;
    private final byte[] headHash;
    private final long blockCount;
    private final long encodedBytes;
    private final byte[] contentDigest;
    private final boolean sealed;
    private final long publishedBlock;
    private final byte[] publishedHash;
    private final byte[] publishedContentDigest;
    private final byte[] publishedTarget;
    private final StateArchiveHotBatchDescriptor publishedDescriptor;
    private final PreparedTarget prepared;

    private GenerationMeta(long id, long baseBlock, byte[] baseHash, long startBlock,
        long endBlock, byte[] headHash, long blockCount, long encodedBytes,
        byte[] contentDigest, boolean sealed, long publishedBlock, byte[] publishedHash,
        byte[] publishedContentDigest, byte[] publishedTarget,
        StateArchiveHotBatchDescriptor publishedDescriptor, PreparedTarget prepared) {
      this.id = id;
      this.baseBlock = baseBlock;
      this.baseHash = copyDigest(baseHash, "baseHash");
      this.startBlock = startBlock;
      this.endBlock = endBlock;
      this.headHash = copyDigest(headHash, "headHash");
      this.blockCount = blockCount;
      this.encodedBytes = encodedBytes;
      this.contentDigest = copyDigest(contentDigest, "contentDigest");
      this.sealed = sealed;
      this.publishedBlock = publishedBlock;
      this.publishedHash = copyDigest(publishedHash, "publishedHash");
      this.publishedContentDigest = copyDigest(publishedContentDigest,
          "publishedContentDigest");
      this.publishedTarget = copyDigest(publishedTarget, "publishedTarget");
      this.publishedDescriptor = publishedDescriptor;
      this.prepared = prepared;
    }

    private static GenerationMeta empty(long id, long baseBlock, byte[] baseHash) {
      return new GenerationMeta(id, baseBlock, baseHash, -1, baseBlock, baseHash, 0, 0,
          ZERO_DIGEST, false, baseBlock, baseHash, ZERO_DIGEST, ZERO_DIGEST, null, null);
    }

    private GenerationMeta sealed() {
      return new GenerationMeta(id, baseBlock, baseHash, startBlock, endBlock, headHash,
          blockCount, encodedBytes, contentDigest, true, publishedBlock, publishedHash,
          publishedContentDigest, publishedTarget, publishedDescriptor, prepared);
    }

    private GenerationMeta appended(BlockSnapshotMeta meta, byte[] record) {
      long first = blockCount == 0 ? meta.getBlockNumber() : startBlock;
      return new GenerationMeta(id, baseBlock, baseHash, first, meta.getBlockNumber(),
          meta.getBlockHash(), Math.addExact(blockCount, 1),
          Math.addExact(encodedBytes, record.length),
          nextContentDigest(contentDigest, meta, record), false, meta.getBlockNumber(),
          meta.getBlockHash(), nextContentDigest(contentDigest, meta, record), publishedTarget,
          publishedDescriptor, null);
    }

    private GenerationMeta published(byte[] targetDigest,
        StateArchiveHotBatchDescriptor descriptor) {
      return new GenerationMeta(id, baseBlock, baseHash, startBlock, endBlock, headHash,
          blockCount, encodedBytes, contentDigest, sealed, endBlock, headHash, contentDigest,
          targetDigest, descriptor, null);
    }

    private List<StateArchiveIndexDatabase.Mutation> mutableMetadata() {
      List<StateArchiveIndexDatabase.Mutation> mutations = new ArrayList<>();
      mutations.add(StateArchiveIndexDatabase.put(META_START_BLOCK, longBytes(startBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_END_BLOCK, longBytes(endBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_HEAD_HASH, headHash));
      mutations.add(StateArchiveIndexDatabase.put(META_BLOCK_COUNT, longBytes(blockCount)));
      mutations.add(StateArchiveIndexDatabase.put(META_ENCODED_BYTES, longBytes(encodedBytes)));
      mutations.add(StateArchiveIndexDatabase.put(META_CONTENT_DIGEST, contentDigest));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_BLOCK,
          longBytes(publishedBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_HASH, publishedHash));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_CONTENT_DIGEST,
          publishedContentDigest));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_TARGET, publishedTarget));
      mutations.add(publishedDescriptor == null
          ? StateArchiveIndexDatabase.delete(META_PUBLISHED_DESCRIPTOR)
          : StateArchiveIndexDatabase.put(META_PUBLISHED_DESCRIPTOR,
              new StateArchiveHotBatchDescriptorCodec().encode(publishedDescriptor)));
      return mutations;
    }

    private List<StateArchiveIndexDatabase.Mutation> createMutations(byte[] formatIdentity) {
      List<StateArchiveIndexDatabase.Mutation> mutations = new ArrayList<>();
      mutations.add(StateArchiveIndexDatabase.put(META_FORMAT, formatIdentity));
      mutations.add(StateArchiveIndexDatabase.put(META_ID, longBytes(id)));
      mutations.add(StateArchiveIndexDatabase.put(META_BASE_BLOCK, longBytes(baseBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_BASE_HASH, baseHash));
      mutations.add(StateArchiveIndexDatabase.put(META_START_BLOCK, longBytes(startBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_END_BLOCK, longBytes(endBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_HEAD_HASH, headHash));
      mutations.add(StateArchiveIndexDatabase.put(META_BLOCK_COUNT, longBytes(blockCount)));
      mutations.add(StateArchiveIndexDatabase.put(META_ENCODED_BYTES, longBytes(encodedBytes)));
      mutations.add(StateArchiveIndexDatabase.put(META_CONTENT_DIGEST, contentDigest));
      mutations.add(StateArchiveIndexDatabase.put(META_SEALED, new byte[]{0}));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_BLOCK,
          longBytes(publishedBlock)));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_HASH, publishedHash));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_CONTENT_DIGEST,
          publishedContentDigest));
      mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_TARGET, publishedTarget));
      if (publishedDescriptor != null) {
        mutations.add(StateArchiveIndexDatabase.put(META_PUBLISHED_DESCRIPTOR,
            new StateArchiveHotBatchDescriptorCodec().encode(publishedDescriptor)));
      }
      return mutations;
    }

    private void validate() throws IOException {
      if (id < 0 || baseBlock < 0 || blockCount < 0 || encodedBytes < 0
          || endBlock < baseBlock || publishedBlock < baseBlock || publishedBlock > endBlock) {
        throw new ArchivePersistenceException("Hot Archive generation metadata is invalid");
      }
      if (blockCount == 0) {
        if (startBlock != -1 || endBlock != baseBlock || !Arrays.equals(headHash, baseHash)
            || encodedBytes != 0 || !Arrays.equals(contentDigest, ZERO_DIGEST) || sealed) {
          throw new ArchivePersistenceException("Hot Archive empty generation is inconsistent");
        }
      } else if (startBlock != baseBlock + 1 || endBlock - startBlock + 1 != blockCount) {
        throw new ArchivePersistenceException("Hot Archive generation coverage is inconsistent");
      }
      if (publishedBlock == baseBlock
          && (!Arrays.equals(publishedHash, baseHash)
          || !Arrays.equals(publishedContentDigest, ZERO_DIGEST))) {
        throw new ArchivePersistenceException("Hot Archive published base is inconsistent");
      }
      if (publishedBlock == endBlock
          && (!Arrays.equals(publishedHash, headHash)
          || !Arrays.equals(publishedContentDigest, contentDigest))) {
        throw new ArchivePersistenceException("Hot Archive published head is inconsistent");
      }
      if (prepared == null && publishedBlock != endBlock) {
        throw new ArchivePersistenceException("Hot Archive unpublished tail has no target");
      }
      if (prepared != null
          && (sealed
          || prepared.descriptor.getParentPublishedBlock() != publishedBlock
          || !Arrays.equals(prepared.descriptor.getParentPublishedHash(), publishedHash)
          || !Arrays.equals(prepared.descriptor.getParentContentDigest(),
          publishedContentDigest)
          || prepared.descriptor.getFirstBlock().getBlockNumber() != publishedBlock + 1
          || prepared.descriptor.getLastBlock().getBlockNumber() != endBlock
          || !Arrays.equals(prepared.descriptor.getLastBlock().getBlockHash(), headHash)
          || !Arrays.equals(prepared.descriptor.getResultContentDigest(), contentDigest))) {
        throw new ArchivePersistenceException("Hot Archive prepared target is inconsistent");
      }
      boolean initialPublication = Arrays.equals(publishedTarget, ZERO_DIGEST);
      if (initialPublication != (publishedDescriptor == null)) {
        throw new ArchivePersistenceException("Hot Archive published descriptor is missing");
      }
      if (publishedDescriptor != null
          && (publishedDescriptor.getLastBlock().getBlockNumber() != publishedBlock
          || !Arrays.equals(publishedDescriptor.getLastBlock().getBlockHash(), publishedHash))) {
        throw new ArchivePersistenceException("Hot Archive published descriptor is inconsistent");
      }
      if (publishedDescriptor != null && blockCount > 0
          && publishedDescriptor.getFirstBlock().getBlockNumber() >= startBlock
          && !Arrays.equals(publishedDescriptor.getResultContentDigest(),
          publishedContentDigest)) {
        throw new ArchivePersistenceException(
            "Hot Archive published descriptor content differs");
      }
      if (sealed && publishedBlock != endBlock) {
        throw new ArchivePersistenceException("Hot Archive sealed generation is unpublished");
      }
    }

    private void requireBase(long expectedBlock, byte[] expectedHash) throws IOException {
      if (baseBlock != expectedBlock || !Arrays.equals(baseHash, expectedHash)) {
        throw new ArchivePersistenceException("Hot Archive generation parent differs");
      }
    }

    private void requirePublication(byte[] expectedTarget,
        StateArchiveHotBatchDescriptor expectedDescriptor) throws IOException {
      if (!Arrays.equals(publishedTarget, expectedTarget)
          || !Objects.equals(publishedDescriptor, expectedDescriptor)) {
        throw new ArchivePersistenceException(
            "Hot Archive generation publication differs");
      }
    }

  }

  private static final class ByteArrayKey {
    private final byte[] value;

    private ByteArrayKey(byte[] value) {
      this.value = Arrays.copyOf(value, value.length);
    }

    @Override
    public boolean equals(Object object) {
      return object instanceof ByteArrayKey
          && Arrays.equals(value, ((ByteArrayKey) object).value);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(value);
    }
  }

  private static int engineTag(Engine engine) {
    return engine == Engine.LEVELDB ? 1 : 2;
  }

  private static Engine engine(int tag) throws IOException {
    if (tag == 1) {
      return Engine.LEVELDB;
    }
    if (tag == 2) {
      return Engine.ROCKSDB;
    }
    throw new ArchivePersistenceException("Hot Archive engine tag is unsupported");
  }
}
