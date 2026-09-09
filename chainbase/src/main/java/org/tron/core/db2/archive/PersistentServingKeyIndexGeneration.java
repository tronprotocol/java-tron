package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Persistent immutable exact-key serving generation backed by the configured database engine. */
public final class PersistentServingKeyIndexGeneration implements ServingKeyIndex {

  private static final int MAGIC = 0x534b4947; // SKIG
  private static final short VERSION = 4;
  private static final short EXACT_VERSION = 5;
  private static final int MAX_MANIFEST_SIZE = 1024 * 1024;
  private static final byte DATA_PREFIX = 1;
  private static final byte RANGE_DATA_PREFIX = 2;
  private static final byte KEY_META_PREFIX = 3;
  private static final byte KEY_PAGE_PREFIX = 4;
  private static final byte STORE_COVERAGE_PREFIX = 5;
  private static final int INLINE_EPOCH_LIMIT = 4;
  private static final int EPOCHS_PER_PAGE = 512;
  private static final byte INLINE = 1;
  private static final byte PAGED = 2;
  private static final byte[] PRESENT = new byte[]{1};
  private static final String MANIFEST = "generation.meta";
  private static final String MANIFEST_TEMP = "generation.meta.tmp";
  private static final String DATABASE = "keys";
  private static final String ESTIMATED_LIVE_DATA_SIZE =
      "rocksdb.estimate-live-data-size";
  private static final String TOTAL_SST_FILES_SIZE = "rocksdb.total-sst-files-size";
  private static final String PENDING_COMPACTION_BYTES =
      "rocksdb.estimate-pending-compaction-bytes";

  private final Path directory;
  private final Descriptor descriptor;
  private final Engine engine;
  private final StateArchiveIndexDatabase.Reader database;
  private final Runnable release;
  private boolean closed;

  private PersistentServingKeyIndexGeneration(Path directory, Descriptor descriptor,
      Engine engine, boolean validateEngine, Runnable release) throws IOException {
    this.directory = directory;
    this.descriptor = descriptor;
    this.engine = Objects.requireNonNull(engine, "engine");
    this.release = Objects.requireNonNull(release, "release");
    if (validateEngine) {
      StateArchiveIndexEngineManifest.openOrCreateLegacy(directory, engine);
    }
    StateArchiveIndexDatabase.Reader opened = null;
    try {
      opened = StateArchiveIndexDatabase.openImmutableReader(directory.resolve(DATABASE), engine);
      if (descriptor.formatVersion == EXACT_VERSION) {
        validateExactStoreCoverage(opened, descriptor);
      }
      this.database = opened;
    } catch (IOException | RuntimeException failure) {
      if (opened != null) {
        opened.close();
      }
      throw new IOException("Failed to open serving index generation", failure);
    }
  }

  public static PersistentServingKeyIndexGeneration build(Path directory, String generationId,
      long baseEpoch, byte[] baseHash, Iterable<HistoryCommitMarker> committed,
      ServingKeyIndexGeneration.AuthoritativeIndexReader reader,
      List<String> participatingDatabases) throws IOException {
    return build(directory, generationId, baseEpoch, baseHash, committed, reader,
        participatingDatabases, new byte[32]);
  }

  public static PersistentServingKeyIndexGeneration build(Path directory, String generationId,
      long baseEpoch, byte[] baseHash, Iterable<HistoryCommitMarker> committed,
      ServingKeyIndexGeneration.AuthoritativeIndexReader reader,
      List<String> participatingDatabases, byte[] latestSourceIdentityDigest) throws IOException {
    Objects.requireNonNull(directory, "directory");
    Objects.requireNonNull(committed, "committed");
    Objects.requireNonNull(reader, "reader");
    List<String> participants = sortedParticipants(participatingDatabases);
    String scopeIdentity = ArchiveParticipantDescriptor.scopeIdentity(participants);
    if (generationId == null || generationId.isEmpty() || baseEpoch < 0) {
      throw new IllegalArgumentException("Invalid serving generation identity");
    }
    requireHash(baseHash, "baseHash");
    requireHash(latestSourceIdentityDigest, "latestSourceIdentityDigest");
    if (Files.exists(directory)) {
      throw new IllegalArgumentException("Serving generation directory already exists");
    }
    Files.createDirectories(directory);
    Engine engine = configuredEngine();
    StateArchiveIndexEngineManifest.openOrCreate(directory, engine);

    MessageDigest sourceDigest = sha256();
    updateLong(sourceDigest, baseEpoch);
    sourceDigest.update(baseHash);
    updateStringDigest(sourceDigest, scopeIdentity);
    updateParticipantDigest(sourceDigest, participants);
    long previousEpoch = baseEpoch;
    long previousBlock = baseEpoch;
    byte[] previousHash = Arrays.copyOf(baseHash, baseHash.length);
    long keyChanges = 0;
    try (StateArchiveIndexDatabase.Writer target =
        StateArchiveIndexDatabase.openWriter(directory.resolve(DATABASE), engine)) {
      for (HistoryCommitMarker marker : committed) {
        BlockSnapshotMeta meta = marker.getMeta();
        validateNext(marker, previousEpoch, previousBlock, previousHash, participants);
        HistoryIndexRecord record = reader.read(marker.getIndexLocation());
        validateMarker(marker, record, participants);
        List<StateArchiveIndexDatabase.Mutation> mutations = new ArrayList<>();
        for (HistoryIndexRecord.KeyGroup group : record.getGroups()) {
          for (byte[] key : group.getKeys()) {
            mutations.add(StateArchiveIndexDatabase.put(
                dataKey(group.getDbName(), key, meta.getEpoch()), PRESENT));
            mutations.add(StateArchiveIndexDatabase.put(
                rangeDataKey(group.getDbName(), key, meta.getEpoch()), PRESENT));
            keyChanges++;
          }
        }
        target.write(mutations, false);
        updateSourceDigest(sourceDigest, marker);
        previousEpoch = meta.getEpoch();
        previousBlock = meta.getBlockNumber();
        previousHash = meta.getBlockHash();
      }
      target.write(Collections.singletonList(
          StateArchiveIndexDatabase.put(new byte[]{0}, new byte[]{1})), true);
    }

    Descriptor descriptor = new Descriptor(VERSION, scopeIdentity, generationId, baseEpoch,
        previousEpoch,
        previousHash, sourceDigest.digest(), latestSourceIdentityDigest, participants, keyChanges);
    persistDescriptor(directory, descriptor);
    HistorySegmentStore.syncDirectory(directory);
    return open(directory, engine);
  }

  public static PersistentServingKeyIndexGeneration open(Path directory) throws IOException {
    return open(directory, () -> { });
  }

  static PersistentServingKeyIndexGeneration open(Path directory, Runnable release)
      throws IOException {
    return open(directory, configuredEngine(), release);
  }

  static PersistentServingKeyIndexGeneration open(Path directory, Engine engine)
      throws IOException {
    return open(directory, engine, () -> { });
  }

  static PersistentServingKeyIndexGeneration open(Path directory, Engine engine,
      Runnable release) throws IOException {
    return new PersistentServingKeyIndexGeneration(directory, loadDescriptor(directory), engine,
        true, release);
  }

  static PersistentServingKeyIndexGeneration openTrusted(Path directory, Engine engine,
      Runnable release) throws IOException {
    return new PersistentServingKeyIndexGeneration(directory, loadDescriptor(directory), engine,
        false, release);
  }

  /** Creates one approved v5 exact-only generation from a validated logical increment plan. */
  public static PersistentServingKeyIndexGeneration buildExact(Path directory,
      String generationId, ServingIndexIncrementalPlan plan,
      byte[] latestSourceIdentityDigest) throws IOException {
    return buildExact(directory, generationId, plan, latestSourceIdentityDigest, () -> { });
  }

  static PersistentServingKeyIndexGeneration buildExact(Path directory, String generationId,
      ServingIndexIncrementalPlan plan, byte[] latestSourceIdentityDigest,
      ExactWriteFaultHook faultHook) throws IOException {
    Objects.requireNonNull(directory, "directory");
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(faultHook, "faultHook");
    validateExactIdentity(generationId, plan, latestSourceIdentityDigest);
    if (Files.exists(directory)) {
      throw new IllegalArgumentException("Serving generation directory already exists");
    }
    Files.createDirectories(directory);
    Engine engine = configuredEngine();
    StateArchiveIndexEngineManifest.openOrCreate(directory, engine);
    byte[] sourceDigest = rollSourceDigest(plan.getSourceSeedDigest(),
        plan.getSourceStepDigests());
    long keyChanges;
    try (StateArchiveIndexDatabase.Writer target =
        StateArchiveIndexDatabase.openWriter(directory.resolve(DATABASE), engine)) {
      keyChanges = applyExactPlan(target, generationId, plan, plan.getIndexedFrom(),
          sourceDigest, faultHook);
    }
    Descriptor descriptor = new Descriptor(EXACT_VERSION,
        ArchiveParticipantDescriptor.FORMAT_ID, generationId, plan.getIndexedFrom(),
        plan.getIndexedThrough(), plan.getHeadHash(), sourceDigest, latestSourceIdentityDigest,
        plan.getParticipatingDatabases(), keyChanges);
    persistDescriptor(directory, descriptor);
    HistorySegmentStore.syncDirectory(directory);
    return open(directory, engine);
  }

  /** Checkpoints this immutable v5 generation and applies only the validated {@code (I,H]} plan. */
  public synchronized PersistentServingKeyIndexGeneration extendExact(Path directory,
      String generationId, ServingIndexIncrementalPlan plan,
      byte[] latestSourceIdentityDigest) throws IOException {
    return extendExact(directory, generationId, plan, latestSourceIdentityDigest, () -> { });
  }

  synchronized PersistentServingKeyIndexGeneration extendExact(Path directory,
      String generationId, ServingIndexIncrementalPlan plan,
      byte[] latestSourceIdentityDigest, ExactWriteFaultHook faultHook) throws IOException {
    ensureOpen();
    Objects.requireNonNull(directory, "directory");
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(faultHook, "faultHook");
    validateExactIdentity(generationId, plan, latestSourceIdentityDigest);
    if (descriptor.formatVersion != EXACT_VERSION
        || plan.getIndexedFrom() != descriptor.indexedThrough
        || !Arrays.equals(plan.getIndexedFromHash(), descriptor.headHash)
        || !plan.getParticipatingDatabases().equals(descriptor.participants)) {
      throw new IllegalArgumentException("Exact serving increment does not extend current I");
    }
    if (Files.exists(directory)) {
      throw new IllegalArgumentException("Serving generation directory already exists");
    }
    Files.createDirectories(directory);
    StateArchiveIndexEngineManifest.openOrCreate(directory, engine);
    StateArchiveIndexDatabase.checkpointImmutable(this.directory.resolve(DATABASE),
        directory.resolve(DATABASE), engine);
    byte[] sourceDigest = rollSourceDigest(descriptor.sourceDigest,
        plan.getSourceStepDigests());
    long added;
    try (StateArchiveIndexDatabase.Writer target =
        StateArchiveIndexDatabase.openWriter(directory.resolve(DATABASE), engine)) {
      added = applyExactPlan(target, generationId, plan, descriptor.indexedFrom,
          sourceDigest, faultHook);
    }
    Descriptor replacement = new Descriptor(EXACT_VERSION, descriptor.scopeIdentity,
        generationId, descriptor.indexedFrom, plan.getIndexedThrough(), plan.getHeadHash(),
        sourceDigest, latestSourceIdentityDigest, descriptor.participants,
        descriptor.keyChanges + added);
    persistDescriptor(directory, replacement);
    HistorySegmentStore.syncDirectory(directory);
    return open(directory, engine);
  }

  /**
   * Creates a process-owned mutable copy used to publish immutable generations without reopening
   * the write database for every checkpoint target.
   */
  synchronized RuntimeBuilder openRuntimeBuilder(Path builderDirectory) throws IOException {
    ensureOpen();
    if (descriptor.formatVersion != EXACT_VERSION) {
      throw new IllegalStateException("Serving runtime builder requires exact-only format");
    }
    return RuntimeBuilder.open(builderDirectory, this);
  }

  @Override
  public synchronized OptionalLong firstChangeAfter(String dbName, byte[] rawKey,
      long targetBlock, long upperBound) throws IOException {
    ensureOpen();
    Objects.requireNonNull(dbName, "dbName");
    Objects.requireNonNull(rawKey, "rawKey");
    validateCoverage(dbName, targetBlock, upperBound);
    if (targetBlock == Long.MAX_VALUE) {
      return OptionalLong.empty();
    }
    if (descriptor.formatVersion == EXACT_VERSION) {
      return firstExactChangeAfter(dbName, rawKey, targetBlock, upperBound);
    }
    byte[] prefix = dataPrefix(dbName, rawKey);
    byte[] seek = ByteBuffer.allocate(prefix.length + Long.BYTES).put(prefix)
        .putLong(targetBlock + 1).array();
    try (StateArchiveIndexDatabase.Cursor iterator = database.cursor()) {
      iterator.seek(seek);
      StateArchiveIndexDatabase.KeyValue entry = iterator.next();
      if (entry == null) {
        return OptionalLong.empty();
      }
      byte[] found = entry.getKey();
      if (found.length != prefix.length + Long.BYTES || !startsWith(found, prefix)) {
        return OptionalLong.empty();
      }
      long epoch = ByteBuffer.wrap(found, prefix.length, Long.BYTES).getLong();
      return epoch <= upperBound ? OptionalLong.of(epoch) : OptionalLong.empty();
    }
  }

  @Override
  public synchronized List<ServingKeyIndexGeneration.ChangedKey> changesInRange(String dbName,
      byte[] lowerInclusive, byte[] upperExclusive, long targetBlock, long upperBound,
      int maxChangedKeys) throws IOException {
    ensureOpen();
    Objects.requireNonNull(dbName, "dbName");
    Objects.requireNonNull(lowerInclusive, "lowerInclusive");
    if (maxChangedKeys <= 0) {
      throw new IllegalArgumentException("maxChangedKeys must be positive");
    }
    if (upperExclusive != null
        && BlockReverseDiff.compareUnsigned(lowerInclusive, upperExclusive) > 0) {
      throw new IllegalArgumentException("lowerInclusive must not exceed upperExclusive");
    }
    if (!supportsRangeQueries()) {
      throw new ArchivePersistenceException(
          "Serving index generation must be upgraded before range queries");
    }
    validateCoverage(dbName, targetBlock, upperBound);
    if (targetBlock == Long.MAX_VALUE) {
      return Collections.emptyList();
    }

    byte[] databasePrefix = rangeDatabasePrefix(dbName);
    List<ServingKeyIndexGeneration.ChangedKey> result = new ArrayList<>();
    try (StateArchiveIndexDatabase.Cursor iterator = database.cursor()) {
      iterator.seek(concat(databasePrefix, encodeRangeRawKey(lowerInclusive)));
      StateArchiveIndexDatabase.KeyValue entry;
      while ((entry = iterator.next()) != null) {
        RangeDataKey found = decodeRangeDataKey(entry.getKey(), databasePrefix);
        if (found == null) {
          break;
        }
        if (upperExclusive != null
            && BlockReverseDiff.compareUnsigned(found.rawKey, upperExclusive) >= 0) {
          break;
        }
        if (found.epoch <= targetBlock) {
          iterator.seek(rangeDataKey(dbName, found.rawKey, targetBlock + 1));
          entry = iterator.next();
          if (entry == null) {
            break;
          }
          RangeDataKey candidate = decodeRangeDataKey(entry.getKey(), databasePrefix);
          if (candidate == null || !Arrays.equals(candidate.rawKey, found.rawKey)) {
            continue;
          }
          found = candidate;
        }
        if (found.epoch <= upperBound) {
          if (result.size() == maxChangedKeys) {
            throw new ArchiveQueryLimitExceededException("changed-key budget exceeded");
          }
          result.add(new ServingKeyIndexGeneration.ChangedKey(found.rawKey, found.epoch));
        }
        iterator.seek(rangeAfterRawKey(databasePrefix, found.rawKey));
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public String getGenerationId() {
    return descriptor.generationId;
  }

  @Override
  public long getIndexedFrom() {
    return descriptor.indexedFrom;
  }

  @Override
  public long getIndexedThrough() {
    return descriptor.indexedThrough;
  }

  @Override
  public byte[] getHeadHash() {
    return Arrays.copyOf(descriptor.headHash, descriptor.headHash.length);
  }

  @Override
  public byte[] getAuthoritativePrefixDigest() {
    return Arrays.copyOf(descriptor.sourceDigest, descriptor.sourceDigest.length);
  }

  public List<String> getParticipatingDatabases() {
    return descriptor.participants;
  }

  public String getScopeIdentity() {
    return descriptor.scopeIdentity;
  }

  public long getKeyChangeCount() {
    return descriptor.keyChanges;
  }

  public byte[] getLatestSourceIdentityDigest() {
    return Arrays.copyOf(descriptor.latestSourceIdentityDigest,
        descriptor.latestSourceIdentityDigest.length);
  }

  public boolean isLatestSourceIdentityBound() {
    for (byte value : descriptor.latestSourceIdentityDigest) {
      if (value != 0) {
        return true;
      }
    }
    return false;
  }

  public boolean supportsRangeQueries() {
    return descriptor.formatVersion == VERSION;
  }

  public boolean isExactOnlyFormat() {
    return descriptor.formatVersion == EXACT_VERSION;
  }

  public PersistentStoreCoverage getPersistentStoreCoverage(String dbName) throws IOException {
    ensureOpen();
    if (!isExactOnlyFormat()) {
      throw new ArchivePersistenceException("Serving generation has no durable Store coverage");
    }
    byte[] encoded = database.get(storeCoverageKey(dbName));
    PersistentStoreCoverage coverage = decodeCoverage(encoded);
    if (!coverage.dbName.equals(dbName)
        || coverage.indexedFrom != descriptor.indexedFrom
        || coverage.indexedThrough != descriptor.indexedThrough
        || !Arrays.equals(coverage.headHash, descriptor.headHash)
        || !Arrays.equals(coverage.sourceDigest, descriptor.sourceDigest)
        || !coverage.generationId.equals(descriptor.generationId)) {
      throw new ArchivePersistenceException("Serving Store coverage identity mismatch");
    }
    return coverage;
  }

  /** Performs an explicit read-only scan of v5 metadata for measured observability. */
  public synchronized GenerationStatistics inspectStatistics() throws IOException {
    return inspectStatistics(this::readLongProperty);
  }

  synchronized GenerationStatistics inspectStatistics(RocksPropertyReader propertyReader)
      throws IOException {
    ensureOpen();
    Objects.requireNonNull(propertyReader, "propertyReader");
    if (!isExactOnlyFormat()) {
      throw new ArchivePersistenceException("Serving statistics require exact-only format");
    }
    Map<String, StoreStatistics> stores = new LinkedHashMap<>();
    for (String participant : descriptor.participants) {
      stores.put(participant, inspectStore(participant));
    }
    long measuredChanges = stores.values().stream()
        .mapToLong(StoreStatistics::getChangeEntryCount).sum();
    if (measuredChanges != descriptor.keyChanges) {
      throw new ArchivePersistenceException(
          "Serving statistics differ from generation change count");
    }
    FileSizeMeasurement files = measureGenerationFiles(directory);
    EngineStatistics engine = new EngineStatistics(
        readProperty(propertyReader, ESTIMATED_LIVE_DATA_SIZE),
        readProperty(propertyReader, TOTAL_SST_FILES_SIZE),
        readProperty(propertyReader, PENDING_COMPACTION_BYTES));
    return new GenerationStatistics(descriptor.generationId, descriptor.indexedFrom,
        descriptor.indexedThrough, stores, files.apparentBytes, files.allocatedBytes,
        files.allocatedExact, engine);
  }

  Path getDirectory() {
    return directory;
  }

  Engine getEngine() {
    return engine;
  }

  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      closed = true;
      database.close();
      release.run();
    }
  }

  private void validateCoverage(String dbName, long targetBlock, long upperBound) {
    if (Collections.binarySearch(descriptor.participants, dbName) < 0) {
      throw new IllegalArgumentException("Database is outside serving index coverage: " + dbName);
    }
    if (targetBlock < descriptor.indexedFrom || targetBlock > upperBound
        || upperBound > descriptor.indexedThrough) {
      throw new IllegalArgumentException("Query range is outside serving index coverage");
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("Serving index generation is closed");
    }
  }

  private StoreStatistics inspectStore(String dbName) throws IOException {
    byte[] metaPrefix = exactPartitionPrefix(KEY_META_PREFIX, dbName);
    byte[] pagePrefix = exactPartitionPrefix(KEY_PAGE_PREFIX, dbName);
    long keyMetadata = 0;
    long inlineKeys = 0;
    long pagedKeys = 0;
    long changeEntries = 0;
    long expectedPagedEntries = 0;
    long pages = 0;
    long pagedEntries = 0;
    long logicalBytes = 0;
    try (StateArchiveIndexDatabase.Cursor iterator = database.cursor()) {
      iterator.seek(metaPrefix);
      StateArchiveIndexDatabase.KeyValue entry;
      while ((entry = iterator.next()) != null && startsWith(entry.getKey(), metaPrefix)) {
        byte[] key = entry.getKey();
        byte[] value = entry.getValue();
        KeyMeta meta = decodeKeyMeta(value);
        keyMetadata++;
        changeEntries += meta.count;
        logicalBytes += key.length + value.length;
        if (meta.mode == INLINE) {
          inlineKeys++;
        } else {
          pagedKeys++;
          expectedPagedEntries += meta.count;
        }
      }
    }
    try (StateArchiveIndexDatabase.Cursor iterator = database.cursor()) {
      iterator.seek(pagePrefix);
      StateArchiveIndexDatabase.KeyValue entry;
      while ((entry = iterator.next()) != null && startsWith(entry.getKey(), pagePrefix)) {
        byte[] key = entry.getKey();
        byte[] value = entry.getValue();
        pages++;
        pagedEntries += decodeEpochPage(value).length;
        logicalBytes += key.length + value.length;
      }
    }
    if (pagedEntries != expectedPagedEntries) {
      throw new ArchivePersistenceException(
          "Serving statistics found inconsistent paged entry totals: " + dbName);
    }
    byte[] coverageKey = storeCoverageKey(dbName);
    byte[] coverageValue = database.get(coverageKey);
    decodeCoverage(coverageValue);
    logicalBytes += coverageKey.length + coverageValue.length;
    return new StoreStatistics(dbName, keyMetadata, inlineKeys, pagedKeys, pages,
        changeEntries, logicalBytes);
  }

  private static byte[] exactPartitionPrefix(byte prefix, String dbName) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    return ByteBuffer.allocate(1 + Short.BYTES).put(prefix).putShort((short) storeId).array();
  }

  private static FileSizeMeasurement measureGenerationFiles(Path root) throws IOException {
    long apparent = 0;
    long allocated = 0;
    boolean exact = true;
    try (Stream<Path> paths = Files.walk(root)) {
      Iterator<Path> iterator = paths.filter(Files::isRegularFile).iterator();
      while (iterator.hasNext()) {
        Path file = iterator.next();
        apparent += Files.size(file);
        if (exact) {
          try {
            Number blocks = (Number) Files.getAttribute(file, "unix:blocks",
                LinkOption.NOFOLLOW_LINKS);
            allocated += blocks.longValue() * 512L;
          } catch (UnsupportedOperationException | IllegalArgumentException failure) {
            exact = false;
          }
        }
      }
    }
    return new FileSizeMeasurement(apparent, exact ? allocated : apparent, exact);
  }

  private OptionalLong readLongProperty(String name) {
    try {
      return database.readLongProperty(name);
    } catch (IOException | IllegalArgumentException failure) {
      return OptionalLong.empty();
    }
  }

  private static LongPropertyMeasurement readProperty(RocksPropertyReader reader, String name)
      throws IOException {
    OptionalLong value = Objects.requireNonNull(reader.read(name), "property value");
    if (value.isPresent() && value.getAsLong() < 0) {
      throw new ArchivePersistenceException("Negative RocksDB property: " + name);
    }
    return value.isPresent()
        ? LongPropertyMeasurement.available(value.getAsLong())
        : LongPropertyMeasurement.unavailable();
  }

  private OptionalLong firstExactChangeAfter(String dbName, byte[] rawKey, long targetBlock,
      long upperBound) throws IOException {
    KeyMeta meta;
    byte[] encoded = database.get(keyMetaKey(dbName, rawKey));
    if (encoded == null) {
      return OptionalLong.empty();
    }
    meta = decodeKeyMeta(encoded);
    if (meta.lastEpoch <= targetBlock || meta.firstEpoch > upperBound) {
      return OptionalLong.empty();
    }
    if (meta.mode == INLINE) {
      return firstChange(meta.inlineEpochs, targetBlock, upperBound);
    }
    int pageCount = pageCount(meta.count);
    int low = 0;
    int high = pageCount;
    while (low < high) {
      int middle = (low + high) >>> 1;
      long[] page = readPage(dbName, rawKey, middle);
      if (page[page.length - 1] <= targetBlock) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    if (low == pageCount) {
      return OptionalLong.empty();
    }
    return firstChange(readPage(dbName, rawKey, low), targetBlock, upperBound);
  }

  private long[] readPage(String dbName, byte[] rawKey, int pageIndex) throws IOException {
    byte[] encoded = database.get(keyPageKey(dbName, rawKey, pageIndex));
    if (encoded == null) {
      throw new ArchivePersistenceException("Exact serving epoch page is missing");
    }
    return decodeEpochPage(encoded);
  }

  private static long applyExactPlan(StateArchiveIndexDatabase.Writer target,
      String generationId,
      ServingIndexIncrementalPlan plan, long coverageFrom, byte[] sourceDigest,
      ExactWriteFaultHook faultHook) throws IOException {
    Map<ExactKey, List<Long>> changes = new LinkedHashMap<>();
    for (Map.Entry<String, List<ServingIndexIncrementalPlan.KeyChange>> database
        : plan.getChangesByDatabase().entrySet()) {
      for (ServingIndexIncrementalPlan.KeyChange change : database.getValue()) {
        ExactKey key = new ExactKey(database.getKey(), change.getRawKey());
        changes.computeIfAbsent(key, ignored -> new ArrayList<>()).add(change.getEpoch());
      }
    }
    List<StateArchiveIndexDatabase.Mutation> mutations = new ArrayList<>();
    for (Map.Entry<ExactKey, List<Long>> entry : changes.entrySet()) {
      appendExactChanges(target, mutations, entry.getKey(), entry.getValue());
    }
    for (String database : plan.getParticipatingDatabases()) {
      PersistentStoreCoverage coverage = new PersistentStoreCoverage(database,
          coverageFrom, plan.getIndexedThrough(), plan.getHeadHash(), sourceDigest,
          generationId, comparatorId(database));
      mutations.add(StateArchiveIndexDatabase.put(storeCoverageKey(database),
          encodeCoverage(coverage)));
    }
    faultHook.beforeWrite();
    target.write(mutations, true);
    return changes.values().stream().mapToLong(List::size).sum();
  }

  static final class RuntimeBuilder implements java.io.Closeable {

    private final Path directory;
    private final Engine engine;
    private final StateArchiveIndexDatabase.Writer writer;
    private Descriptor descriptor;
    private boolean closed;

    private RuntimeBuilder(Path directory, Engine engine,
        StateArchiveIndexDatabase.Writer writer, Descriptor descriptor) {
      this.directory = directory;
      this.engine = engine;
      this.writer = writer;
      this.descriptor = descriptor;
    }

    private static RuntimeBuilder open(Path directory,
        PersistentServingKeyIndexGeneration source) throws IOException {
      Objects.requireNonNull(directory, "directory");
      requireReplaceableBuilderDirectory(directory);
      deleteBuilderDirectory(directory);
      Files.createDirectories(directory);
      StateArchiveIndexEngineManifest.openOrCreate(directory, source.engine);
      StateArchiveIndexDatabase.Writer writer = null;
      try {
        StateArchiveIndexDatabase.checkpointImmutable(source.directory.resolve(DATABASE),
            directory.resolve(DATABASE), source.engine);
        writer = StateArchiveIndexDatabase.openWriter(directory.resolve(DATABASE), source.engine);
        return new RuntimeBuilder(directory, source.engine, writer, source.descriptor);
      } catch (IOException | RuntimeException failure) {
        if (writer != null) {
          try {
            writer.close();
          } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
          }
        }
        throw failure;
      }
    }

    synchronized PersistentServingKeyIndexGeneration extendExact(Path shadow,
        String generationId, ServingIndexIncrementalPlan plan,
        byte[] latestSourceIdentityDigest) throws IOException {
      ensureOpen();
      validateExactIdentity(generationId, plan, latestSourceIdentityDigest);
      if (plan.getIndexedFrom() != descriptor.indexedThrough
          || !Arrays.equals(plan.getIndexedFromHash(), descriptor.headHash)
          || !plan.getParticipatingDatabases().equals(descriptor.participants)) {
        throw new IllegalArgumentException("Exact serving increment does not extend runtime I");
      }
      if (Files.exists(shadow, LinkOption.NOFOLLOW_LINKS)) {
        throw new IllegalArgumentException("Serving generation directory already exists");
      }
      byte[] sourceDigest = rollSourceDigest(descriptor.sourceDigest,
          plan.getSourceStepDigests());
      long added = applyExactPlan(writer, generationId, plan, descriptor.indexedFrom,
          sourceDigest, () -> { });
      Descriptor replacement = new Descriptor(EXACT_VERSION, descriptor.scopeIdentity,
          generationId, descriptor.indexedFrom, plan.getIndexedThrough(), plan.getHeadHash(),
          sourceDigest, latestSourceIdentityDigest, descriptor.participants,
          descriptor.keyChanges + added);
      descriptor = replacement;

      Files.createDirectories(shadow);
      StateArchiveIndexEngineManifest.openOrCreate(shadow, engine);
      StateArchiveIndexDatabase.checkpoint(directory.resolve(DATABASE),
          shadow.resolve(DATABASE), engine);
      persistDescriptor(shadow, replacement);
      HistorySegmentStore.syncDirectory(shadow);
      return PersistentServingKeyIndexGeneration.open(shadow, engine);
    }

    @Override
    public synchronized void close() throws IOException {
      if (!closed) {
        closed = true;
        writer.close();
      }
    }

    private void ensureOpen() {
      if (closed) {
        throw new IllegalStateException("Serving runtime builder is closed");
      }
    }

    private static void requireReplaceableBuilderDirectory(Path directory) throws IOException {
      if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
        return;
      }
      if (Files.isSymbolicLink(directory)
          || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
        throw new ArchivePersistenceException(
            "Serving runtime builder path is not a direct directory");
      }
    }

    private static void deleteBuilderDirectory(Path directory) throws IOException {
      if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
        return;
      }
      List<Path> paths = new ArrayList<>();
      try (Stream<Path> walk = Files.walk(directory)) {
        walk.sorted(java.util.Comparator.reverseOrder()).forEach(paths::add);
      }
      for (Path path : paths) {
        if (Files.isSymbolicLink(path)) {
          throw new ArchivePersistenceException(
              "Serving runtime builder contains a symbolic link");
        }
        Files.delete(path);
      }
    }
  }

  private static void appendExactChanges(StateArchiveIndexDatabase.Writer target,
      List<StateArchiveIndexDatabase.Mutation> batch, ExactKey key,
      List<Long> appended) throws IOException {
    byte[] metaKey = keyMetaKey(key.dbName, key.rawKey);
    byte[] existing = target.get(metaKey);
    KeyMeta meta = existing == null ? null : decodeKeyMeta(existing);
    if (meta == null) {
      requireStrictEpochs(appended, Long.MIN_VALUE);
      if (appended.size() <= INLINE_EPOCH_LIMIT) {
        batch.add(StateArchiveIndexDatabase.put(metaKey,
            encodeKeyMeta(KeyMeta.inline(toArray(appended)))));
        return;
      }
      writeAllPages(batch, key, appended, 0);
      batch.add(StateArchiveIndexDatabase.put(metaKey,
          encodeKeyMeta(KeyMeta.paged(appended.size(), appended.get(0),
              appended.get(appended.size() - 1)))));
      return;
    }
    requireStrictEpochs(appended, meta.lastEpoch);
    if (meta.mode == INLINE && meta.count + appended.size() <= INLINE_EPOCH_LIMIT) {
      List<Long> combined = asList(meta.inlineEpochs);
      combined.addAll(appended);
      batch.add(StateArchiveIndexDatabase.put(metaKey,
          encodeKeyMeta(KeyMeta.inline(toArray(combined)))));
      return;
    }
    if (meta.mode == INLINE) {
      List<Long> combined = asList(meta.inlineEpochs);
      combined.addAll(appended);
      writeAllPages(batch, key, combined, 0);
    } else {
      int lastPageIndex = pageCount(meta.count) - 1;
      long[] lastPage = decodeEpochPage(target.get(
          keyPageKey(key.dbName, key.rawKey, lastPageIndex)));
      List<Long> combined = asList(lastPage);
      combined.addAll(appended);
      writeAllPages(batch, key, combined, lastPageIndex);
    }
    batch.add(StateArchiveIndexDatabase.put(metaKey,
        encodeKeyMeta(KeyMeta.paged(meta.count + appended.size(),
            meta.firstEpoch, appended.get(appended.size() - 1)))));
  }

  private static void writeAllPages(List<StateArchiveIndexDatabase.Mutation> batch,
      ExactKey key, List<Long> epochs, int firstPageIndex) {
    for (int start = 0, page = firstPageIndex; start < epochs.size();
        start += EPOCHS_PER_PAGE, page++) {
      int end = Math.min(start + EPOCHS_PER_PAGE, epochs.size());
      batch.add(StateArchiveIndexDatabase.put(keyPageKey(key.dbName, key.rawKey, page),
          encodeEpochPage(toArray(epochs.subList(start, end)))));
    }
  }

  private static byte[] keyMetaKey(String dbName, byte[] rawKey) {
    return exactKey(KEY_META_PREFIX, dbName, rawKey, null);
  }

  private static byte[] keyPageKey(String dbName, byte[] rawKey, int pageIndex) {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Serving page index must not be negative");
    }
    return exactKey(KEY_PAGE_PREFIX, dbName, rawKey, pageIndex);
  }

  private static byte[] exactKey(byte prefix, String dbName, byte[] rawKey, Integer pageIndex) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    byte[] encodedKey = encodeRangeRawKey(rawKey);
    int suffix = pageIndex == null ? 0 : Integer.BYTES;
    ByteBuffer key = ByteBuffer.allocate(1 + Short.BYTES + encodedKey.length + 2 + suffix)
        .put(prefix).putShort((short) storeId).put(encodedKey).put((byte) 0).put((byte) 0);
    if (pageIndex != null) {
      key.putInt(pageIndex);
    }
    return key.array();
  }

  private static byte[] storeCoverageKey(String dbName) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    return ByteBuffer.allocate(1 + Short.BYTES).put(STORE_COVERAGE_PREFIX)
        .putShort((short) storeId).array();
  }

  private static byte[] encodeKeyMeta(KeyMeta meta) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeByte(meta.mode);
      output.writeLong(meta.count);
      output.writeLong(meta.firstEpoch);
      output.writeLong(meta.lastEpoch);
      if (meta.mode == INLINE) {
        output.writeInt(meta.inlineEpochs.length);
        for (long epoch : meta.inlineEpochs) {
          output.writeLong(epoch);
        }
      }
      output.flush();
      return withChecksum(bytes.toByteArray());
    } catch (IOException impossible) {
      throw new IllegalStateException("Unexpected exact serving metadata encoding failure",
          impossible);
    }
  }

  private static KeyMeta decodeKeyMeta(byte[] encoded) {
    byte[] payload = checkedPayload(encoded, "exact serving key metadata");
    try {
      DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
      byte mode = input.readByte();
      long count = input.readLong();
      long first = input.readLong();
      long last = input.readLong();
      if (count <= 0 || first < 0 || last < first || mode != INLINE && mode != PAGED) {
        throw new ArchivePersistenceException("Invalid exact serving key metadata");
      }
      long[] inline = null;
      if (mode == INLINE) {
        int size = input.readInt();
        if (size != count || size <= 0 || size > INLINE_EPOCH_LIMIT) {
          throw new ArchivePersistenceException("Invalid inline serving metadata");
        }
        inline = new long[size];
        for (int i = 0; i < size; i++) {
          inline[i] = input.readLong();
        }
        requireStrictEpochs(asList(inline), Long.MIN_VALUE);
      }
      if (input.available() != 0) {
        throw new ArchivePersistenceException("Exact serving metadata payload mismatch");
      }
      return new KeyMeta(mode, count, first, last, inline);
    } catch (IOException failure) {
      throw new ArchivePersistenceException("Exact serving metadata is truncated", failure);
    }
  }

  private static byte[] encodeEpochPage(long[] epochs) {
    ByteBuffer payload = ByteBuffer.allocate(Integer.BYTES + epochs.length * Long.BYTES)
        .putInt(epochs.length);
    for (long epoch : epochs) {
      payload.putLong(epoch);
    }
    return withChecksum(payload.array());
  }

  private static long[] decodeEpochPage(byte[] encoded) {
    byte[] payload = checkedPayload(encoded, "exact serving epoch page");
    ByteBuffer input = ByteBuffer.wrap(payload);
    if (input.remaining() < Integer.BYTES) {
      throw new ArchivePersistenceException("Exact serving epoch page is truncated");
    }
    int count = input.getInt();
    if (count <= 0 || count > EPOCHS_PER_PAGE
        || input.remaining() != count * Long.BYTES) {
      throw new ArchivePersistenceException("Invalid exact serving epoch page");
    }
    long[] epochs = new long[count];
    for (int i = 0; i < count; i++) {
      epochs[i] = input.getLong();
    }
    requireStrictEpochs(asList(epochs), Long.MIN_VALUE);
    return epochs;
  }

  private static byte[] encodeCoverage(PersistentStoreCoverage coverage) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeUTF(coverage.dbName);
      output.writeLong(coverage.indexedFrom);
      output.writeLong(coverage.indexedThrough);
      output.write(coverage.headHash);
      output.write(coverage.sourceDigest);
      output.writeUTF(coverage.generationId);
      output.writeUTF(coverage.comparatorId);
      output.flush();
      return withChecksum(bytes.toByteArray());
    } catch (IOException impossible) {
      throw new IllegalStateException("Unexpected Store coverage encoding failure", impossible);
    }
  }

  private static PersistentStoreCoverage decodeCoverage(byte[] encoded) {
    byte[] payload = checkedPayload(encoded, "serving Store coverage");
    try {
      DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
      String database = input.readUTF();
      long from = input.readLong();
      long through = input.readLong();
      byte[] headHash = new byte[32];
      byte[] sourceDigest = new byte[32];
      input.readFully(headHash);
      input.readFully(sourceDigest);
      String generationId = input.readUTF();
      String comparatorId = input.readUTF();
      if (from < 0 || through < from || generationId.isEmpty() || comparatorId.isEmpty()
          || input.available() != 0) {
        throw new ArchivePersistenceException("Invalid serving Store coverage");
      }
      return new PersistentStoreCoverage(database, from, through, headHash, sourceDigest,
          generationId, comparatorId);
    } catch (IOException failure) {
      throw new ArchivePersistenceException("Serving Store coverage is truncated", failure);
    }
  }

  private static void validateExactStoreCoverage(StateArchiveIndexDatabase.Reader database,
      Descriptor descriptor) throws IOException {
    for (String participant : descriptor.participants) {
      PersistentStoreCoverage coverage = decodeCoverage(database.get(
          storeCoverageKey(participant)));
      if (!participant.equals(coverage.dbName)
          || coverage.indexedFrom != descriptor.indexedFrom
          || coverage.indexedThrough != descriptor.indexedThrough
          || !Arrays.equals(coverage.headHash, descriptor.headHash)
          || !Arrays.equals(coverage.sourceDigest, descriptor.sourceDigest)
          || !coverage.generationId.equals(descriptor.generationId)
          || !coverage.comparatorId.equals(comparatorId(participant))) {
        throw new ArchivePersistenceException("Serving Store coverage identity mismatch");
      }
    }
  }

  private static byte[] withChecksum(byte[] payload) {
    return ByteBuffer.allocate(payload.length + Integer.BYTES).put(payload)
        .putInt(Hashing.crc32c().hashBytes(payload).asInt()).array();
  }

  private static byte[] checkedPayload(byte[] encoded, String name) {
    if (encoded == null || encoded.length <= Integer.BYTES) {
      throw new ArchivePersistenceException(name + " is missing or truncated");
    }
    byte[] payload = Arrays.copyOf(encoded, encoded.length - Integer.BYTES);
    int checksum = ByteBuffer.wrap(encoded, payload.length, Integer.BYTES).getInt();
    if (checksum != Hashing.crc32c().hashBytes(payload).asInt()) {
      throw new ArchivePersistenceException(name + " checksum mismatch");
    }
    return payload;
  }

  private static void requireStrictEpochs(List<Long> epochs, long previous) {
    for (long epoch : epochs) {
      if (epoch < 0 || epoch <= previous) {
        throw new ArchivePersistenceException("Serving epochs are not strictly increasing");
      }
      previous = epoch;
    }
  }

  private static int pageCount(long count) {
    return (int) ((count + EPOCHS_PER_PAGE - 1) / EPOCHS_PER_PAGE);
  }

  private static long[] toArray(List<Long> epochs) {
    long[] result = new long[epochs.size()];
    for (int i = 0; i < epochs.size(); i++) {
      result[i] = epochs.get(i);
    }
    return result;
  }

  private static List<Long> asList(long[] epochs) {
    List<Long> result = new ArrayList<>(epochs.length);
    for (long epoch : epochs) {
      result.add(epoch);
    }
    return result;
  }

  private static OptionalLong firstChange(long[] epochs, long target, long upperBound) {
    int low = 0;
    int high = epochs.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (epochs[middle] <= target) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return low < epochs.length && epochs[low] <= upperBound
        ? OptionalLong.of(epochs[low]) : OptionalLong.empty();
  }

  private static byte[] rollingDigest(byte[] previous, byte[] delta) {
    MessageDigest digest = sha256();
    digest.update(previous);
    digest.update(delta);
    return digest.digest();
  }

  private static byte[] rollSourceDigest(byte[] seed, List<byte[]> steps) {
    byte[] result = Arrays.copyOf(seed, seed.length);
    for (byte[] step : steps) {
      result = rollingDigest(result, step);
    }
    return result;
  }

  static byte[] sourceDigestForRebuild(ServingIndexIncrementalPlan plan) {
    Objects.requireNonNull(plan, "plan");
    return rollSourceDigest(plan.getSourceSeedDigest(), plan.getSourceStepDigests());
  }

  private static String comparatorId(String dbName) {
    return "market_pair_price_to_order".equals(dbName)
        ? "MARKET_PRICE_V1" : "UNSIGNED_RAW_V1";
  }

  private static Engine configuredEngine() {
    return StateArchiveCheckpointServingIndex.configuredEngine();
  }

  private static void validateExactIdentity(String generationId,
      ServingIndexIncrementalPlan plan, byte[] latestSourceIdentityDigest) {
    if (generationId == null || generationId.isEmpty()) {
      throw new IllegalArgumentException("generationId must not be empty");
    }
    requireHash(latestSourceIdentityDigest, "latestSourceIdentityDigest");
    ArchiveParticipantDescriptor.current().requireExactParticipants(
        plan.getParticipatingDatabases());
  }

  private static byte[] dataKey(String dbName, byte[] rawKey, long epoch) {
    if (epoch < 0) {
      throw new IllegalArgumentException("Serving index epoch must not be negative");
    }
    byte[] prefix = dataPrefix(dbName, rawKey);
    return ByteBuffer.allocate(prefix.length + Long.BYTES).put(prefix).putLong(epoch).array();
  }

  private static byte[] dataPrefix(String dbName, byte[] rawKey) {
    byte[] database = dbName.getBytes(StandardCharsets.UTF_8);
    return ByteBuffer.allocate(1 + Integer.BYTES + database.length + Integer.BYTES + rawKey.length)
        .put(DATA_PREFIX).putInt(database.length).put(database).putInt(rawKey.length).put(rawKey)
        .array();
  }

  private static byte[] rangeDataKey(String dbName, byte[] rawKey, long epoch) {
    if (epoch < 0) {
      throw new IllegalArgumentException("Serving index epoch must not be negative");
    }
    byte[] databasePrefix = rangeDatabasePrefix(dbName);
    byte[] encodedKey = encodeRangeRawKey(rawKey);
    return ByteBuffer.allocate(databasePrefix.length + encodedKey.length + 2 + Long.BYTES)
        .put(databasePrefix).put(encodedKey).put((byte) 0).put((byte) 0).putLong(epoch).array();
  }

  private static byte[] rangeDatabasePrefix(String dbName) {
    byte[] name = dbName.getBytes(StandardCharsets.UTF_8);
    return ByteBuffer.allocate(1 + Integer.BYTES + name.length)
        .put(RANGE_DATA_PREFIX).putInt(name.length).put(name).array();
  }

  private static byte[] encodeRangeRawKey(byte[] rawKey) {
    ByteArrayOutputStream encoded = new ByteArrayOutputStream(rawKey.length);
    for (byte value : rawKey) {
      encoded.write(value);
      if (value == 0) {
        encoded.write(0xff);
      }
    }
    return encoded.toByteArray();
  }

  private static byte[] rangeAfterRawKey(byte[] databasePrefix, byte[] rawKey) {
    byte[] encoded = encodeRangeRawKey(rawKey);
    return ByteBuffer.allocate(databasePrefix.length + encoded.length + 2)
        .put(databasePrefix).put(encoded).put((byte) 0).put((byte) 1).array();
  }

  private static RangeDataKey decodeRangeDataKey(byte[] key, byte[] databasePrefix) {
    if (!startsWith(key, databasePrefix) || key.length < databasePrefix.length + 2 + Long.BYTES) {
      return null;
    }
    ByteArrayOutputStream rawKey = new ByteArrayOutputStream();
    int cursor = databasePrefix.length;
    int epochOffset = key.length - Long.BYTES;
    while (cursor < epochOffset) {
      byte value = key[cursor++];
      if (value != 0) {
        rawKey.write(value);
      } else if (cursor < epochOffset && key[cursor] == (byte) 0xff) {
        rawKey.write(0);
        cursor++;
      } else if (cursor < epochOffset && key[cursor] == 0) {
        cursor++;
        if (cursor != epochOffset) {
          return null;
        }
        return new RangeDataKey(rawKey.toByteArray(),
            ByteBuffer.wrap(key, epochOffset, Long.BYTES).getLong());
      } else {
        return null;
      }
    }
    return null;
  }

  private static byte[] concat(byte[] left, byte[] right) {
    return ByteBuffer.allocate(left.length + right.length).put(left).put(right).array();
  }

  private static final class RangeDataKey {
    private final byte[] rawKey;
    private final long epoch;

    private RangeDataKey(byte[] rawKey, long epoch) {
      this.rawKey = rawKey;
      this.epoch = epoch;
    }
  }

  private static boolean startsWith(byte[] value, byte[] prefix) {
    if (value.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (value[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private static void validateNext(HistoryCommitMarker marker, long previousEpoch,
      long previousBlock, byte[] previousHash, List<String> participants) {
    BlockSnapshotMeta meta = marker.getMeta();
    if (marker.getPreviousEpoch() != previousEpoch || meta.getEpoch() != previousEpoch + 1
        || meta.getBlockNumber() != previousBlock + 1
        || !Arrays.equals(meta.getParentHash(), previousHash)
        || !participants.equals(marker.getDatabases())) {
      throw new IllegalArgumentException("Serving index source commit prefix is inconsistent");
    }
  }

  private static void validateMarker(HistoryCommitMarker marker, HistoryIndexRecord record,
      List<String> participants) {
    if (record == null || !marker.getMeta().equals(record.getMeta())
        || !same(marker.getHistoryLocation(), record.getHistoryLocation())) {
      throw new IllegalArgumentException(
          "Commit marker does not match authoritative history index record");
    }
    for (HistoryIndexRecord.KeyGroup group : record.getGroups()) {
      if (Collections.binarySearch(participants, group.getDbName()) < 0) {
        throw new IllegalArgumentException("History index contains an unknown database");
      }
    }
  }

  private static boolean same(HistoryLocation left, HistoryLocation right) {
    return left.getSegmentId() == right.getSegmentId()
        && left.getOffset() == right.getOffset()
        && left.getRecordLength() == right.getRecordLength()
        && left.getBodyChecksum() == right.getBodyChecksum()
        && Arrays.equals(left.getBodyDigest(), right.getBodyDigest());
  }

  private static List<String> sortedParticipants(List<String> databases) {
    List<String> result = new ArrayList<>(Objects.requireNonNull(databases, "databases"));
    Collections.sort(result);
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Serving index participant set must not be empty");
    }
    String previous = null;
    for (String database : result) {
      if (database == null || database.isEmpty() || database.equals(previous)) {
        throw new IllegalArgumentException("Serving index participant set is invalid");
      }
      previous = database;
    }
    return Collections.unmodifiableList(result);
  }

  private static void updateSourceDigest(MessageDigest digest, HistoryCommitMarker marker) {
    updateLong(digest, marker.getMeta().getEpoch());
    updateLong(digest, marker.getMeta().getBlockNumber());
    digest.update(marker.getMeta().getBlockHash());
    digest.update(marker.getMeta().getParentHash());
    updateLong(digest, marker.getIndexLocation().getOffset());
    updateLong(digest, marker.getIndexLocation().getRecordLength());
    digest.update(marker.getIndexLocation().getDigest());
    digest.update(marker.getHistoryLocation().getBodyDigest());
  }

  private static void updateParticipantDigest(MessageDigest digest, List<String> databases) {
    updateLong(digest, databases.size());
    for (String database : databases) {
      byte[] encoded = database.getBytes(StandardCharsets.UTF_8);
      updateLong(digest, encoded.length);
      digest.update(encoded);
    }
  }

  private static void updateStringDigest(MessageDigest digest, String value) {
    byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
    updateLong(digest, encoded.length);
    digest.update(encoded);
  }

  private static void updateLong(MessageDigest digest, long value) {
    digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  private static void requireHash(byte[] hash, String name) {
    if (hash == null || hash.length != 32) {
      throw new IllegalArgumentException(name + " must be exactly 32 bytes");
    }
  }

  private static void persistDescriptor(Path directory, Descriptor descriptor) throws IOException {
    byte[] encoded = encodeDescriptor(descriptor);
    Path temporary = directory.resolve(MANIFEST_TEMP);
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(encoded);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
    try {
      Files.move(temporary, directory.resolve(MANIFEST), StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new ArchivePersistenceException(
          "Serving index filesystem does not support atomic manifests", unsupported);
    }
  }

  private static Descriptor loadDescriptor(Path directory) throws IOException {
    Path manifest = directory.resolve(MANIFEST);
    if (!Files.isRegularFile(manifest)) {
      throw new ArchivePersistenceException("Serving index generation manifest is missing");
    }
    try {
      return decodeDescriptor(Files.readAllBytes(manifest));
    } catch (IllegalArgumentException invalid) {
      throw new ArchivePersistenceException("Serving index generation manifest is corrupt",
          invalid);
    }
  }

  private static byte[] encodeDescriptor(Descriptor descriptor) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(MAGIC);
      output.writeShort(descriptor.formatVersion);
      output.writeShort(0);
      output.writeUTF(descriptor.scopeIdentity);
      output.writeUTF(descriptor.generationId);
      output.writeLong(descriptor.indexedFrom);
      output.writeLong(descriptor.indexedThrough);
      output.write(descriptor.headHash);
      output.write(descriptor.sourceDigest);
      output.write(descriptor.latestSourceIdentityDigest);
      output.writeLong(descriptor.keyChanges);
      output.writeInt(descriptor.participants.size());
      for (String participant : descriptor.participants) {
        output.writeUTF(participant);
      }
      output.flush();
      byte[] payload = bytes.toByteArray();
      output.writeInt(Hashing.crc32c().hashBytes(payload).asInt());
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("Unexpected serving manifest encoding failure", impossible);
    }
  }

  private static Descriptor decodeDescriptor(byte[] encoded) {
    if (encoded == null || encoded.length < 96 || encoded.length > MAX_MANIFEST_SIZE) {
      throw new IllegalArgumentException("Serving index manifest length is invalid");
    }
    byte[] payload = Arrays.copyOf(encoded, encoded.length - Integer.BYTES);
    int checksum = ByteBuffer.wrap(encoded, payload.length, Integer.BYTES).getInt();
    if (checksum != Hashing.crc32c().hashBytes(payload).asInt()) {
      throw new IllegalArgumentException("Serving index manifest checksum mismatch");
    }
    try {
      DataInputStream input = new DataInputStream(new ByteArrayInputStream(encoded));
      if (input.readInt() != MAGIC) {
        throw new IllegalArgumentException("Unsupported serving index manifest");
      }
      short version = input.readShort();
      if ((version != EXACT_VERSION && version != VERSION && version != VERSION - 1)
          || input.readShort() != 0) {
        throw new IllegalArgumentException("Unsupported serving index manifest");
      }
      String scopeIdentity = input.readUTF();
      String generationId = input.readUTF();
      long from = input.readLong();
      long through = input.readLong();
      byte[] headHash = new byte[32];
      byte[] sourceDigest = new byte[32];
      input.readFully(headHash);
      input.readFully(sourceDigest);
      byte[] latestSourceIdentityDigest = new byte[32];
      input.readFully(latestSourceIdentityDigest);
      long keyChanges = input.readLong();
      int count = input.readInt();
      if (generationId.isEmpty() || from < 0 || through < from || keyChanges < 0
          || count <= 0 || count > ArchiveStoreScope.getStateDatabases().size() + 16) {
        throw new IllegalArgumentException("Invalid serving index manifest fields");
      }
      List<String> participants = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        participants.add(input.readUTF());
      }
      if (input.available() != Integer.BYTES) {
        throw new IllegalArgumentException("Serving index manifest payload mismatch");
      }
      List<String> sorted = sortedParticipants(participants);
      if (!scopeIdentity.equals(ArchiveParticipantDescriptor.scopeIdentity(sorted))) {
        throw new IllegalArgumentException("Serving index manifest scope identity mismatch");
      }
      return new Descriptor(version, scopeIdentity, generationId, from, through, headHash,
          sourceDigest, latestSourceIdentityDigest, sorted, keyChanges);
    } catch (IOException invalid) {
      throw new IllegalArgumentException("Serving index manifest is truncated", invalid);
    }
  }

  private static final class Descriptor {
    private final short formatVersion;
    private final String scopeIdentity;
    private final String generationId;
    private final long indexedFrom;
    private final long indexedThrough;
    private final byte[] headHash;
    private final byte[] sourceDigest;
    private final byte[] latestSourceIdentityDigest;
    private final List<String> participants;
    private final long keyChanges;

    private Descriptor(short formatVersion, String scopeIdentity, String generationId,
        long indexedFrom,
        long indexedThrough,
        byte[] headHash, byte[] sourceDigest, byte[] latestSourceIdentityDigest,
        List<String> participants, long keyChanges) {
      this.formatVersion = formatVersion;
      this.scopeIdentity = scopeIdentity;
      this.generationId = generationId;
      this.indexedFrom = indexedFrom;
      this.indexedThrough = indexedThrough;
      this.headHash = Arrays.copyOf(headHash, headHash.length);
      this.sourceDigest = Arrays.copyOf(sourceDigest, sourceDigest.length);
      this.latestSourceIdentityDigest = Arrays.copyOf(latestSourceIdentityDigest,
          latestSourceIdentityDigest.length);
      this.participants = participants;
      this.keyChanges = keyChanges;
    }
  }

  @FunctionalInterface
  interface ExactWriteFaultHook {
    void beforeWrite() throws IOException;
  }

  /** Durable completeness identity for one exact-27 Store partition. */
  public static final class PersistentStoreCoverage {
    private final String dbName;
    private final long indexedFrom;
    private final long indexedThrough;
    private final byte[] headHash;
    private final byte[] sourceDigest;
    private final String generationId;
    private final String comparatorId;

    private PersistentStoreCoverage(String dbName, long indexedFrom, long indexedThrough,
        byte[] headHash, byte[] sourceDigest, String generationId, String comparatorId) {
      this.dbName = dbName;
      this.indexedFrom = indexedFrom;
      this.indexedThrough = indexedThrough;
      this.headHash = Arrays.copyOf(headHash, headHash.length);
      this.sourceDigest = Arrays.copyOf(sourceDigest, sourceDigest.length);
      this.generationId = generationId;
      this.comparatorId = comparatorId;
    }

    public String getDbName() {
      return dbName;
    }

    public long getIndexedFrom() {
      return indexedFrom;
    }

    public long getIndexedThrough() {
      return indexedThrough;
    }

    public byte[] getHeadHash() {
      return Arrays.copyOf(headHash, headHash.length);
    }

    public byte[] getSourceDigest() {
      return Arrays.copyOf(sourceDigest, sourceDigest.length);
    }

    public String getGenerationId() {
      return generationId;
    }

    public String getComparatorId() {
      return comparatorId;
    }
  }

  /** Read-only measured statistics for one immutable v5 generation. */
  public static final class GenerationStatistics {
    private final String generationId;
    private final long indexedFrom;
    private final long indexedThrough;
    private final Map<String, StoreStatistics> stores;
    private final long apparentBytes;
    private final long allocatedBytes;
    private final boolean allocatedBytesExact;
    private final EngineStatistics engine;

    private GenerationStatistics(String generationId, long indexedFrom, long indexedThrough,
        Map<String, StoreStatistics> stores, long apparentBytes, long allocatedBytes,
        boolean allocatedBytesExact, EngineStatistics engine) {
      this.generationId = generationId;
      this.indexedFrom = indexedFrom;
      this.indexedThrough = indexedThrough;
      this.stores = Collections.unmodifiableMap(new LinkedHashMap<>(stores));
      this.apparentBytes = apparentBytes;
      this.allocatedBytes = allocatedBytes;
      this.allocatedBytesExact = allocatedBytesExact;
      this.engine = engine;
    }

    public String getGenerationId() {
      return generationId;
    }

    public long getIndexedFrom() {
      return indexedFrom;
    }

    public long getIndexedThrough() {
      return indexedThrough;
    }

    public Map<String, StoreStatistics> getStores() {
      return stores;
    }

    public long getApparentBytes() {
      return apparentBytes;
    }

    public long getAllocatedBytes() {
      return allocatedBytes;
    }

    public boolean isAllocatedBytesExact() {
      return allocatedBytesExact;
    }

    public EngineStatistics getEngine() {
      return engine;
    }
  }

  /** RocksDB properties sampled from the pinned immutable generation. */
  public static final class EngineStatistics {
    private final LongPropertyMeasurement estimatedLiveDataBytes;
    private final LongPropertyMeasurement totalSstBytes;
    private final LongPropertyMeasurement pendingCompactionBytes;

    private EngineStatistics(LongPropertyMeasurement estimatedLiveDataBytes,
        LongPropertyMeasurement totalSstBytes,
        LongPropertyMeasurement pendingCompactionBytes) {
      this.estimatedLiveDataBytes = estimatedLiveDataBytes;
      this.totalSstBytes = totalSstBytes;
      this.pendingCompactionBytes = pendingCompactionBytes;
    }

    public LongPropertyMeasurement getEstimatedLiveDataBytes() {
      return estimatedLiveDataBytes;
    }

    public LongPropertyMeasurement getTotalSstBytes() {
      return totalSstBytes;
    }

    public LongPropertyMeasurement getPendingCompactionBytes() {
      return pendingCompactionBytes;
    }
  }

  /** One property value with an explicit unsupported/unavailable state. */
  public static final class LongPropertyMeasurement {
    private final boolean available;
    private final long value;

    private LongPropertyMeasurement(boolean available, long value) {
      this.available = available;
      this.value = value;
    }

    private static LongPropertyMeasurement available(long value) {
      return new LongPropertyMeasurement(true, value);
    }

    private static LongPropertyMeasurement unavailable() {
      return new LongPropertyMeasurement(false, 0);
    }

    public boolean isAvailable() {
      return available;
    }

    public long getValue() {
      if (!available) {
        throw new IllegalStateException("RocksDB property is unavailable");
      }
      return value;
    }
  }

  /** Logical RocksDB entry statistics for one exact Store partition. */
  public static final class StoreStatistics {
    private final String dbName;
    private final long keyMetadataCount;
    private final long inlineKeyCount;
    private final long pagedKeyCount;
    private final long pageCount;
    private final long changeEntryCount;
    private final long logicalBytes;

    private StoreStatistics(String dbName, long keyMetadataCount, long inlineKeyCount,
        long pagedKeyCount, long pageCount, long changeEntryCount, long logicalBytes) {
      this.dbName = dbName;
      this.keyMetadataCount = keyMetadataCount;
      this.inlineKeyCount = inlineKeyCount;
      this.pagedKeyCount = pagedKeyCount;
      this.pageCount = pageCount;
      this.changeEntryCount = changeEntryCount;
      this.logicalBytes = logicalBytes;
    }

    public String getDbName() {
      return dbName;
    }

    public long getKeyMetadataCount() {
      return keyMetadataCount;
    }

    public long getInlineKeyCount() {
      return inlineKeyCount;
    }

    public long getPagedKeyCount() {
      return pagedKeyCount;
    }

    public long getPageCount() {
      return pageCount;
    }

    public long getChangeEntryCount() {
      return changeEntryCount;
    }

    public long getLogicalBytes() {
      return logicalBytes;
    }
  }

  private static final class FileSizeMeasurement {
    private final long apparentBytes;
    private final long allocatedBytes;
    private final boolean allocatedExact;

    private FileSizeMeasurement(long apparentBytes, long allocatedBytes,
        boolean allocatedExact) {
      this.apparentBytes = apparentBytes;
      this.allocatedBytes = allocatedBytes;
      this.allocatedExact = allocatedExact;
    }
  }

  @FunctionalInterface
  interface RocksPropertyReader {
    OptionalLong read(String name) throws IOException;
  }

  private static final class KeyMeta {
    private final byte mode;
    private final long count;
    private final long firstEpoch;
    private final long lastEpoch;
    private final long[] inlineEpochs;

    private KeyMeta(byte mode, long count, long firstEpoch, long lastEpoch,
        long[] inlineEpochs) {
      this.mode = mode;
      this.count = count;
      this.firstEpoch = firstEpoch;
      this.lastEpoch = lastEpoch;
      this.inlineEpochs = inlineEpochs;
    }

    private static KeyMeta inline(long[] epochs) {
      return new KeyMeta(INLINE, epochs.length, epochs[0], epochs[epochs.length - 1], epochs);
    }

    private static KeyMeta paged(long count, long firstEpoch, long lastEpoch) {
      return new KeyMeta(PAGED, count, firstEpoch, lastEpoch, null);
    }
  }

  private static final class ExactKey {
    private final String dbName;
    private final byte[] rawKey;

    private ExactKey(String dbName, byte[] rawKey) {
      this.dbName = dbName;
      this.rawKey = Arrays.copyOf(rawKey, rawKey.length);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ExactKey)) {
        return false;
      }
      ExactKey that = (ExactKey) other;
      return dbName.equals(that.dbName) && Arrays.equals(rawKey, that.rawKey);
    }

    @Override
    public int hashCode() {
      return 31 * dbName.hashCode() + Arrays.hashCode(rawKey);
    }
  }
}
