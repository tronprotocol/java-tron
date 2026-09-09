package org.tron.core.db2.archive;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Engine-neutral native store for Archive indexes and independent hot-history generations. */
final class StateArchiveIndexDatabase {

  private static final Logger logger = LoggerFactory.getLogger("DB");
  static final String DEFAULT_COLUMN_FAMILY = "default";
  static final String HOT_BLOCKS_COLUMN_FAMILY = "blocks";
  private static final Map<Path, SharedLevelDatabase> LEVEL_DATABASES = new HashMap<>();
  private static final Map<Path, SharedRocksDatabase> ROCKS_DATABASES = new HashMap<>();
  private static final Map<Path, SharedRocksDatabase> IMMUTABLE_ROCKS_DATABASES =
      new HashMap<>();
  private static final Map<Path, SharedHotRocksDatabase> HOT_ROCKS_DATABASES = new HashMap<>();
  private static final List<String> HOT_COLUMN_FAMILIES = createHotColumnFamilies();
  private static final Set<String> HOT_COLUMN_FAMILY_SET =
      Collections.unmodifiableSet(new LinkedHashSet<>(HOT_COLUMN_FAMILIES));

  private StateArchiveIndexDatabase() {
  }

  static Reader openReader(Path directory, Engine engine) throws IOException {
    return openReader(directory, engine, configuredOptions());
  }

  static Reader openReader(Path directory, Engine engine, NativeDbConfig suppliedConfig)
      throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = Objects.requireNonNull(suppliedConfig, "suppliedConfig");
    return engine == Engine.LEVELDB ? new LevelReader(acquireLevel(path, false, config))
        : new RocksReader(acquireRocks(path, false, config));
  }

  static Reader openImmutableReader(Path directory, Engine engine) throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = configuredOptions();
    return engine == Engine.LEVELDB ? new LevelReader(acquireLevel(path, false, config))
        : new RocksReader(acquireImmutableRocks(path, config));
  }

  static Writer openWriter(Path directory, Engine engine) throws IOException {
    return openWriter(directory, engine, configuredOptions());
  }

  static Writer openWriter(Path directory, Engine engine, NativeDbConfig suppliedConfig)
      throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = Objects.requireNonNull(suppliedConfig, "suppliedConfig");
    return engine == Engine.LEVELDB ? new LevelWriter(acquireLevel(path, true, config))
        : new RocksWriter(acquireRocks(path, true, config));
  }

  static Reader openHotReader(Path directory, Engine engine, NativeDbConfig suppliedConfig)
      throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = Objects.requireNonNull(suppliedConfig, "suppliedConfig");
    return engine == Engine.LEVELDB ? new LevelReader(acquireLevel(path, false, config))
        : new HotRocksReader(acquireHotRocks(path, false, config));
  }

  static Writer openHotWriter(Path directory, Engine engine, NativeDbConfig suppliedConfig)
      throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = Objects.requireNonNull(suppliedConfig, "suppliedConfig");
    return engine == Engine.LEVELDB ? new LevelWriter(acquireLevel(path, true, config))
        : new HotRocksWriter(acquireHotRocks(path, true, config));
  }

  static String hotStoreColumnFamily(String dbName) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    return String.format("store-s%04d-%s", storeId, dbName);
  }

  static List<String> hotColumnFamilies() {
    return HOT_COLUMN_FAMILIES;
  }

  private static List<String> createHotColumnFamilies() {
    List<String> stores = new ArrayList<>(
        ArchiveParticipantDescriptor.current().getParticipants());
    stores.sort(Comparator.comparingInt(
        ArchiveParticipantDescriptor.current()::getStoreId));
    List<String> names = new ArrayList<>();
    names.add(DEFAULT_COLUMN_FAMILY);
    names.add(HOT_BLOCKS_COLUMN_FAMILY);
    for (String store : stores) {
      names.add(hotStoreColumnFamily(store));
    }
    return Collections.unmodifiableList(names);
  }

  static void checkpoint(Path source, Path target, Engine engine) throws IOException {
    Path from = normalize(source);
    Path to = normalize(target);
    if (engine == Engine.ROCKSDB) {
      checkpointRocks(from, to);
      return;
    }
    checkpointLevel(from, to);
  }

  static void checkpointImmutable(Path source, Path target, Engine engine) throws IOException {
    Path from = normalize(source);
    Path to = normalize(target);
    if (engine == Engine.ROCKSDB) {
      checkpointFrozenRocks(from, to);
      return;
    }
    checkpointLevel(from, to);
  }

  static synchronized int openReferenceCount(Path directory, Engine engine) {
    Path path = normalize(directory);
    if (engine == Engine.LEVELDB) {
      SharedLevelDatabase shared = LEVEL_DATABASES.get(path);
      return shared == null ? 0 : shared.references;
    }
    SharedRocksDatabase shared = ROCKS_DATABASES.get(path);
    return shared == null ? 0 : shared.references;
  }

  private static void checkpointLevel(Path source, Path target) throws IOException {
    SharedLevelDatabase shared = acquireLevel(source, false, configuredOptions());
    boolean suspended = false;
    try {
      shared.database.suspendCompactions();
      suspended = true;
      Files.createDirectory(target);
      try (java.util.stream.Stream<Path> entries = Files.list(source)) {
        for (Path entry : (Iterable<Path>) entries::iterator) {
          if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
            continue;
          }
          String name = entry.getFileName().toString();
          if ("LOCK".equals(name) || "LOG".equals(name) || "LOG.old".equals(name)) {
            continue;
          }
          Path destination = target.resolve(name);
          if (name.endsWith(".sst") || name.endsWith(".ldb")) {
            Files.createLink(destination, entry);
          } else {
            Files.copy(entry, destination, StandardCopyOption.COPY_ATTRIBUTES);
            try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                destination, java.nio.file.StandardOpenOption.WRITE)) {
              channel.force(true);
            }
          }
        }
      }
      HistorySegmentStore.syncDirectory(target);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while checkpointing LevelDB Archive index", failure);
    } finally {
      if (suspended) {
        shared.database.resumeCompactions();
      }
      releaseLevel(shared);
    }
  }

  static Mutation put(byte[] key, byte[] value) {
    return put(DEFAULT_COLUMN_FAMILY, key, value);
  }

  static Mutation put(String columnFamily, byte[] key, byte[] value) {
    return new Mutation(columnFamily, key, value);
  }

  static Mutation delete(byte[] key) {
    return delete(DEFAULT_COLUMN_FAMILY, key);
  }

  static Mutation delete(String columnFamily, byte[] key) {
    return new Mutation(columnFamily, key, null);
  }

  private static Path normalize(Path directory) {
    return Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
  }

  private static String requireColumnFamily(String columnFamily) {
    String name = Objects.requireNonNull(columnFamily, "columnFamily");
    if (!HOT_COLUMN_FAMILY_SET.contains(name)) {
      throw new IllegalArgumentException("Unknown Hot Archive column family: " + name);
    }
    return name;
  }

  private static void requireDefaultColumnFamily(String columnFamily) {
    if (!DEFAULT_COLUMN_FAMILY.equals(requireColumnFamily(columnFamily))) {
      throw new IllegalArgumentException(
          "Flat Archive database only supports the default column family");
    }
  }

  private static synchronized SharedLevelDatabase acquireLevel(Path directory, boolean create,
      NativeDbConfig config)
      throws IOException {
    SharedLevelDatabase shared = LEVEL_DATABASES.get(directory);
    if (shared == null) {
      org.iq80.leveldb.Options options = new org.iq80.leveldb.Options()
          .createIfMissing(create)
          .paranoidChecks(true)
          .verifyChecksums(true)
          .compressionType(org.iq80.leveldb.CompressionType.SNAPPY)
          .blockSize(config.getBlockSize())
          .writeBufferSize(config.getWriteBufferSize())
          .cacheSize(config.getCacheSize())
          .maxOpenFiles(config.getMaxOpenFiles());
      try {
        shared = new SharedLevelDatabase(directory, factory.open(directory.toFile(), options));
      } catch (IOException | RuntimeException failure) {
        if (failure instanceof IOException) {
          throw (IOException) failure;
        }
        throw failure;
      }
      LEVEL_DATABASES.put(directory, shared);
      logger.info("Archive native database opened: directory={}, engine=LEVELDB, blockBytes={}, "
              + "writeBufferBytes={}, cacheBytes={}, maxOpenFiles={}", directory,
          config.getBlockSize(), config.getWriteBufferSize(), config.getCacheSize(),
          config.getMaxOpenFiles());
    }
    shared.references++;
    return shared;
  }

  private static synchronized SharedRocksDatabase acquireRocks(Path directory, boolean create,
      NativeDbConfig config) throws IOException {
    SharedRocksDatabase shared = ROCKS_DATABASES.get(directory);
    if (shared == null) {
      RocksResources resources = new RocksResources(config, create);
      try {
        shared = new SharedRocksDatabase(directory,
            org.rocksdb.RocksDB.open(resources.options, directory.toString()), resources, false);
      } catch (org.rocksdb.RocksDBException | RuntimeException failure) {
        resources.close();
        throw new IOException("Failed to open RocksDB Archive serving index", failure);
      }
      ROCKS_DATABASES.put(directory, shared);
      logger.info("Archive native database opened: directory={}, engine=ROCKSDB, blockBytes={}, "
              + "writeBufferBytes={}, cacheBytes={}, maxOpenFiles={}", directory,
          config.getBlockSize(), config.getWriteBufferSize(), config.getCacheSize(),
          config.getMaxOpenFiles());
    }
    shared.references++;
    return shared;
  }

  private static synchronized SharedRocksDatabase acquireImmutableRocks(Path directory,
      NativeDbConfig config) throws IOException {
    SharedRocksDatabase shared = IMMUTABLE_ROCKS_DATABASES.get(directory);
    if (shared == null) {
      RocksResources resources = new RocksResources(config, false);
      try {
        shared = new SharedRocksDatabase(directory,
            org.rocksdb.RocksDB.openReadOnly(resources.options, directory.toString()), resources,
            true);
      } catch (org.rocksdb.RocksDBException | RuntimeException failure) {
        resources.close();
        throw new IOException("Failed to open immutable RocksDB Archive serving index", failure);
      }
      IMMUTABLE_ROCKS_DATABASES.put(directory, shared);
      logger.info("Immutable Archive native database opened: directory={}, engine=ROCKSDB, "
              + "blockBytes={}, cacheBytes={}, maxOpenFiles={}", directory,
          config.getBlockSize(), config.getCacheSize(), config.getMaxOpenFiles());
    }
    shared.references++;
    return shared;
  }

  private static synchronized SharedHotRocksDatabase acquireHotRocks(Path directory,
      boolean create, NativeDbConfig config) throws IOException {
    SharedHotRocksDatabase shared = HOT_ROCKS_DATABASES.get(directory);
    if (shared == null) {
      RocksColumnFamilyResources resources = new RocksColumnFamilyResources(config, create);
      try {
        resources.open(directory);
        shared = new SharedHotRocksDatabase(directory, resources);
      } catch (org.rocksdb.RocksDBException | RuntimeException failure) {
        resources.close();
        throw new IOException("Failed to open RocksDB Hot Archive column families", failure);
      }
      HOT_ROCKS_DATABASES.put(directory, shared);
      logger.info("Hot Archive native database opened: directory={}, engine=ROCKSDB, "
              + "columnFamilies={}, blockBytes={}, writeBufferBytes={}, cacheBytes={}, "
              + "maxOpenFiles={}", directory, resources.handles.size(), config.getBlockSize(),
          config.getWriteBufferSize(), config.getCacheSize(), config.getMaxOpenFiles());
    }
    shared.references++;
    return shared;
  }

  private static synchronized void releaseRocks(SharedRocksDatabase shared) {
    if (--shared.references != 0) {
      return;
    }
    if (shared.immutable) {
      IMMUTABLE_ROCKS_DATABASES.remove(shared.directory);
    } else {
      ROCKS_DATABASES.remove(shared.directory);
    }
    shared.database.close();
    shared.resources.close();
  }

  private static synchronized void releaseHotRocks(SharedHotRocksDatabase shared) {
    if (--shared.references != 0) {
      return;
    }
    HOT_ROCKS_DATABASES.remove(shared.directory);
    shared.resources.close();
  }

  private static void checkpointRocks(Path source, Path target) throws IOException {
    SharedRocksDatabase shared = acquireRocks(source, false, configuredOptions());
    try (org.rocksdb.Checkpoint checkpoint = org.rocksdb.Checkpoint.create(shared.database)) {
      checkpoint.createCheckpoint(target.toString());
    } catch (org.rocksdb.RocksDBException failure) {
      throw new IOException("Failed to checkpoint RocksDB Archive serving index", failure);
    } finally {
      releaseRocks(shared);
    }
  }

  private static void checkpointFrozenRocks(Path source, Path target) throws IOException {
    Files.createDirectory(target);
    try (java.util.stream.Stream<Path> entries = Files.list(source)) {
      for (Path entry : (Iterable<Path>) entries::iterator) {
        if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
          continue;
        }
        String name = entry.getFileName().toString();
        if ("LOCK".equals(name) || "LOG".equals(name) || "LOG.old".equals(name)) {
          continue;
        }
        Path destination = target.resolve(name);
        if (name.endsWith(".sst")) {
          Files.createLink(destination, entry);
        } else {
          Files.copy(entry, destination, StandardCopyOption.COPY_ATTRIBUTES);
          try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
              destination, java.nio.file.StandardOpenOption.WRITE)) {
            channel.force(true);
          }
        }
      }
    }
    HistorySegmentStore.syncDirectory(target);
  }

  private static NativeDbConfig configuredOptions() {
    org.tron.core.config.args.Storage storage = CommonParameter.getInstance().getStorage();
    NativeDbConfig config = storage == null ? null
        : storage.getStateArchiveServingIndexDbSettings();
    return config == null ? NativeDbConfig.large() : config;
  }

  private static synchronized void releaseLevel(SharedLevelDatabase shared) throws IOException {
    if (--shared.references != 0) {
      return;
    }
    LEVEL_DATABASES.remove(shared.directory);
    shared.database.close();
  }

  interface Reader extends Closeable {

    default byte[] get(byte[] key) throws IOException {
      return get(DEFAULT_COLUMN_FAMILY, key);
    }

    byte[] get(String columnFamily, byte[] key) throws IOException;

    default KeyValue seek(byte[] key) throws IOException {
      return seek(DEFAULT_COLUMN_FAMILY, key);
    }

    KeyValue seek(String columnFamily, byte[] key) throws IOException;

    Cursor cursor() throws IOException;

    OptionalLong readLongProperty(String name) throws IOException;
  }

  interface Writer extends Closeable {

    default byte[] get(byte[] key) throws IOException {
      return get(DEFAULT_COLUMN_FAMILY, key);
    }

    byte[] get(String columnFamily, byte[] key) throws IOException;

    void write(List<Mutation> mutations) throws IOException;

    void write(List<Mutation> mutations, boolean sync) throws IOException;
  }

  interface Cursor extends Closeable {

    void seek(byte[] key) throws IOException;

    KeyValue next() throws IOException;
  }

  static final class Mutation {
    private final String columnFamily;
    private final byte[] key;
    private final byte[] value;

    private Mutation(String columnFamily, byte[] key, byte[] value) {
      this.columnFamily = requireColumnFamily(columnFamily);
      this.key = Arrays.copyOf(Objects.requireNonNull(key, "key"), key.length);
      this.value = value == null ? null : Arrays.copyOf(value, value.length);
    }
  }

  static final class KeyValue {
    private final byte[] key;
    private final byte[] value;

    private KeyValue(byte[] key, byte[] value) {
      this.key = Arrays.copyOf(key, key.length);
      this.value = Arrays.copyOf(value, value.length);
    }

    byte[] getKey() {
      return Arrays.copyOf(key, key.length);
    }

    byte[] getValue() {
      return Arrays.copyOf(value, value.length);
    }
  }

  private static final class SharedLevelDatabase {
    private final Path directory;
    private final DB database;
    private int references;

    private SharedLevelDatabase(Path directory, DB database) {
      this.directory = directory;
      this.database = database;
    }
  }

  private static final class SharedRocksDatabase {
    private final Path directory;
    private final org.rocksdb.RocksDB database;
    private final RocksResources resources;
    private final boolean immutable;
    private int references;

    private SharedRocksDatabase(Path directory, org.rocksdb.RocksDB database,
        RocksResources resources, boolean immutable) {
      this.directory = directory;
      this.database = database;
      this.resources = resources;
      this.immutable = immutable;
    }
  }

  private static final class SharedHotRocksDatabase {
    private final Path directory;
    private final RocksColumnFamilyResources resources;
    private int references;

    private SharedHotRocksDatabase(Path directory, RocksColumnFamilyResources resources) {
      this.directory = directory;
      this.resources = resources;
    }
  }

  private static final class RocksResources implements Closeable {
    private final org.rocksdb.LRUCache cache;
    private final org.rocksdb.BloomFilter filter;
    private final org.rocksdb.Options options;

    private RocksResources(NativeDbConfig config, boolean create) {
      org.rocksdb.RocksDB.loadLibrary();
      cache = new org.rocksdb.LRUCache(config.getCacheSize());
      filter = new org.rocksdb.BloomFilter(config.getBloomBitsPerKey(), false);
      org.rocksdb.BlockBasedTableConfig table = new org.rocksdb.BlockBasedTableConfig()
          .setBlockSize(config.getBlockSize())
          .setChecksumType(org.rocksdb.ChecksumType.kCRC32c)
          .setBlockCache(cache)
          .setCacheIndexAndFilterBlocks(true)
          .setPinL0FilterAndIndexBlocksInCache(false)
          .setWholeKeyFiltering(true)
          .setFilter(filter);
      options = new org.rocksdb.Options()
          .setCreateIfMissing(create)
          .setParanoidChecks(true)
          .setCompressionType(org.rocksdb.CompressionType.SNAPPY_COMPRESSION)
          .setWriteBufferSize(config.getWriteBufferSize())
          .setMaxWriteBufferNumber(config.getMaxWriteBufferNumber())
          .setMinWriteBufferNumberToMerge(1)
          .setMaxOpenFiles(config.getMaxOpenFiles())
          .setNumLevels(config.getLevelNumber())
          .setLevelCompactionDynamicLevelBytes(true)
          .setLevel0FileNumCompactionTrigger(config.getLevel0FileNumCompactionTrigger())
          .setLevel0SlowdownWritesTrigger(config.getLevel0SlowdownWritesTrigger())
          .setLevel0StopWritesTrigger(config.getLevel0StopWritesTrigger())
          .setMaxBackgroundCompactions(config.getBackgroundCompactions())
          .setMaxBackgroundFlushes(config.getBackgroundFlushes())
          .setTargetFileSizeBase(config.getTargetFileSizeBase())
          .setMaxBytesForLevelBase(config.getMaxBytesForLevelBase())
          .setMaxBytesForLevelMultiplier(config.getMaxBytesForLevelMultiplier())
          .setTableFormatConfig(table);
    }

    @Override
    public void close() {
      options.close();
      filter.close();
      cache.close();
    }
  }

  private static final class RocksColumnFamilyResources implements Closeable {
    private final NativeDbConfig config;
    private final boolean create;
    private final org.rocksdb.DBOptions databaseOptions;
    private final org.rocksdb.LRUCache cache;
    private final List<org.rocksdb.BloomFilter> filters = new ArrayList<>();
    private final List<org.rocksdb.ColumnFamilyOptions> columnOptions = new ArrayList<>();
    private final List<org.rocksdb.ColumnFamilyHandle> handles = new ArrayList<>();
    private final Map<String, org.rocksdb.ColumnFamilyHandle> handlesByName =
        new LinkedHashMap<>();
    private org.rocksdb.RocksDB database;

    private RocksColumnFamilyResources(NativeDbConfig config, boolean create) {
      org.rocksdb.RocksDB.loadLibrary();
      this.config = config;
      this.create = create;
      cache = new org.rocksdb.LRUCache(config.getCacheSize());
      databaseOptions = new org.rocksdb.DBOptions()
          .setCreateIfMissing(create)
          .setCreateMissingColumnFamilies(create)
          .setParanoidChecks(true)
          .setMaxOpenFiles(config.getMaxOpenFiles())
          .setMaxBackgroundCompactions(config.getBackgroundCompactions())
          .setMaxBackgroundFlushes(config.getBackgroundFlushes());
    }

    private void open(Path directory) throws org.rocksdb.RocksDBException, IOException {
      List<String> expected = hotColumnFamilies();
      if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
        Set<String> actual = listColumnFamilies(directory);
        if (!actual.equals(new LinkedHashSet<>(expected))) {
          throw new ArchivePersistenceException(
              "Hot Archive RocksDB column-family layout differs; rebuild/resync is required");
        }
      } else if (!create) {
        throw new ArchivePersistenceException("Hot Archive RocksDB directory is missing");
      }
      List<org.rocksdb.ColumnFamilyDescriptor> descriptors = new ArrayList<>();
      for (String name : expected) {
        org.rocksdb.BloomFilter filter = new org.rocksdb.BloomFilter(
            config.getBloomBitsPerKey(), false);
        filters.add(filter);
        org.rocksdb.BlockBasedTableConfig table = new org.rocksdb.BlockBasedTableConfig()
            .setBlockSize(config.getBlockSize())
            .setChecksumType(org.rocksdb.ChecksumType.kCRC32c)
            .setBlockCache(cache)
            .setCacheIndexAndFilterBlocks(true)
            .setPinL0FilterAndIndexBlocksInCache(false)
            .setWholeKeyFiltering(true)
            .setFilter(filter);
        org.rocksdb.ColumnFamilyOptions options = new org.rocksdb.ColumnFamilyOptions()
            .setCompressionType(org.rocksdb.CompressionType.SNAPPY_COMPRESSION)
            .setWriteBufferSize(config.getWriteBufferSize())
            .setMaxWriteBufferNumber(config.getMaxWriteBufferNumber())
            .setMinWriteBufferNumberToMerge(1)
            .setNumLevels(config.getLevelNumber())
            .setLevelCompactionDynamicLevelBytes(true)
            .setLevel0FileNumCompactionTrigger(config.getLevel0FileNumCompactionTrigger())
            .setLevel0SlowdownWritesTrigger(config.getLevel0SlowdownWritesTrigger())
            .setLevel0StopWritesTrigger(config.getLevel0StopWritesTrigger())
            .setTargetFileSizeBase(config.getTargetFileSizeBase())
            .setMaxBytesForLevelBase(config.getMaxBytesForLevelBase())
            .setMaxBytesForLevelMultiplier(config.getMaxBytesForLevelMultiplier())
            .setTableFormatConfig(table);
        columnOptions.add(options);
        descriptors.add(new org.rocksdb.ColumnFamilyDescriptor(
            name.getBytes(StandardCharsets.UTF_8), options));
      }
      database = org.rocksdb.RocksDB.open(databaseOptions, directory.toString(), descriptors,
          handles);
      if (handles.size() != expected.size()) {
        throw new ArchivePersistenceException("Hot Archive RocksDB did not open every column");
      }
      for (int index = 0; index < expected.size(); index++) {
        handlesByName.put(expected.get(index), handles.get(index));
      }
    }

    private Set<String> listColumnFamilies(Path directory)
        throws org.rocksdb.RocksDBException {
      try (org.rocksdb.Options options = new org.rocksdb.Options()) {
        Set<String> names = new LinkedHashSet<>();
        for (byte[] name : org.rocksdb.RocksDB.listColumnFamilies(
            options, directory.toString())) {
          names.add(new String(name, StandardCharsets.UTF_8));
        }
        return names;
      }
    }

    private org.rocksdb.ColumnFamilyHandle handle(String columnFamily) {
      org.rocksdb.ColumnFamilyHandle handle = handlesByName.get(
          requireColumnFamily(columnFamily));
      if (handle == null) {
        throw new IllegalArgumentException(
            "Hot Archive column family was not opened: " + columnFamily);
      }
      return handle;
    }

    @Override
    public void close() {
      for (org.rocksdb.ColumnFamilyHandle handle : handles) {
        handle.close();
      }
      handles.clear();
      handlesByName.clear();
      if (database != null) {
        database.close();
        database = null;
      }
      for (org.rocksdb.ColumnFamilyOptions options : columnOptions) {
        options.close();
      }
      for (org.rocksdb.BloomFilter filter : filters) {
        filter.close();
      }
      databaseOptions.close();
      cache.close();
    }
  }

  private static final class LevelReader implements Reader {
    private final SharedLevelDatabase shared;
    private final Snapshot snapshot;
    private final ReadOptions reads;
    private boolean closed;

    private LevelReader(SharedLevelDatabase shared) throws IOException {
      this.shared = shared;
      Snapshot openedSnapshot = null;
      ReadOptions openedReads = null;
      try {
        openedSnapshot = shared.database.getSnapshot();
        openedReads = new ReadOptions().fillCache(true).snapshot(openedSnapshot);
      } catch (RuntimeException failure) {
        if (openedSnapshot != null) {
          openedSnapshot.close();
        }
        releaseLevel(shared);
        throw failure;
      }
      this.snapshot = openedSnapshot;
      this.reads = openedReads;
    }

    @Override
    public byte[] get(String columnFamily, byte[] key) {
      requireColumnFamily(columnFamily);
      return shared.database.get(key, reads);
    }

    @Override
    public KeyValue seek(String columnFamily, byte[] key) throws IOException {
      requireColumnFamily(columnFamily);
      try (DBIterator iterator = shared.database.iterator(reads)) {
        iterator.seek(key);
        if (!iterator.hasNext()) {
          return null;
        }
        Map.Entry<byte[], byte[]> entry = iterator.next();
        return new KeyValue(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public Cursor cursor() {
      return new LevelCursor(shared.database.iterator(reads));
    }

    @Override
    public OptionalLong readLongProperty(String name) {
      return OptionalLong.empty();
    }

    @Override
    public void close() throws IOException {
      if (!closed) {
        closed = true;
        snapshot.close();
        releaseLevel(shared);
      }
    }
  }

  private static final class LevelWriter implements Writer {
    private final SharedLevelDatabase shared;
    private boolean closed;

    private LevelWriter(SharedLevelDatabase shared) {
      this.shared = shared;
    }

    @Override
    public byte[] get(String columnFamily, byte[] key) {
      requireColumnFamily(columnFamily);
      return shared.database.get(key);
    }

    @Override
    public void write(List<Mutation> mutations) throws IOException {
      write(mutations, true);
    }

    @Override
    public void write(List<Mutation> mutations, boolean sync) throws IOException {
      try (org.iq80.leveldb.WriteBatch batch = shared.database.createWriteBatch()) {
        for (Mutation mutation : mutations) {
          if (mutation.value == null) {
            batch.delete(mutation.key);
          } else {
            batch.put(mutation.key, mutation.value);
          }
        }
        shared.database.write(batch, new org.iq80.leveldb.WriteOptions().sync(sync));
      }
    }

    @Override
    public void close() throws IOException {
      if (!closed) {
        closed = true;
        releaseLevel(shared);
      }
    }
  }

  private static final class LevelCursor implements Cursor {
    private final DBIterator iterator;

    private LevelCursor(DBIterator iterator) {
      this.iterator = iterator;
    }

    @Override
    public void seek(byte[] key) {
      iterator.seek(key);
    }

    @Override
    public KeyValue next() {
      if (!iterator.hasNext()) {
        return null;
      }
      Map.Entry<byte[], byte[]> entry = iterator.next();
      return new KeyValue(entry.getKey(), entry.getValue());
    }

    @Override
    public void close() throws IOException {
      iterator.close();
    }
  }

  private static final class RocksReader implements Reader {
    private final SharedRocksDatabase shared;
    private final org.rocksdb.Snapshot snapshot;
    private final org.rocksdb.ReadOptions reads;
    private boolean closed;

    private RocksReader(SharedRocksDatabase shared) {
      this.shared = shared;
      snapshot = shared.database.getSnapshot();
      try {
        reads = new org.rocksdb.ReadOptions().setVerifyChecksums(true).setFillCache(true)
            .setSnapshot(snapshot);
      } catch (RuntimeException failure) {
        shared.database.releaseSnapshot(snapshot);
        releaseRocks(shared);
        throw failure;
      }
    }

    @Override
    public byte[] get(String columnFamily, byte[] key) throws IOException {
      requireDefaultColumnFamily(columnFamily);
      try {
        return shared.database.get(reads, key);
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to read RocksDB Archive serving index", failure);
      }
    }

    @Override
    public KeyValue seek(String columnFamily, byte[] key) throws IOException {
      requireDefaultColumnFamily(columnFamily);
      try (org.rocksdb.ReadOptions seekReads = snapshotReads(snapshot);
          org.rocksdb.RocksIterator iterator = shared.database.newIterator(seekReads)) {
        iterator.seek(key);
        if (!iterator.isValid()) {
          iterator.status();
          return null;
        }
        return new KeyValue(iterator.key(), iterator.value());
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to seek RocksDB Archive serving index", failure);
      }
    }

    @Override
    public Cursor cursor() {
      return new RocksCursor(shared.database, snapshotReads(snapshot), true);
    }

    @Override
    public OptionalLong readLongProperty(String name) {
      try {
        return OptionalLong.of(shared.database.getLongProperty(name));
      } catch (org.rocksdb.RocksDBException | IllegalArgumentException failure) {
        return OptionalLong.empty();
      }
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        reads.close();
        shared.database.releaseSnapshot(snapshot);
        releaseRocks(shared);
      }
    }

    private static org.rocksdb.ReadOptions snapshotReads(org.rocksdb.Snapshot snapshot) {
      return new org.rocksdb.ReadOptions().setVerifyChecksums(true).setFillCache(true)
          .setSnapshot(snapshot);
    }
  }

  private static final class RocksWriter implements Writer {
    private final SharedRocksDatabase shared;
    private boolean closed;

    private RocksWriter(SharedRocksDatabase shared) {
      this.shared = shared;
    }

    @Override
    public byte[] get(String columnFamily, byte[] key) throws IOException {
      requireDefaultColumnFamily(columnFamily);
      try {
        return shared.database.get(key);
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to read RocksDB Archive serving index", failure);
      }
    }

    @Override
    public void write(List<Mutation> mutations) throws IOException {
      write(mutations, true);
    }

    @Override
    public void write(List<Mutation> mutations, boolean sync) throws IOException {
      try (org.rocksdb.WriteBatch batch = new org.rocksdb.WriteBatch()) {
        for (Mutation mutation : mutations) {
          requireDefaultColumnFamily(mutation.columnFamily);
          if (mutation.value == null) {
            batch.delete(mutation.key);
          } else {
            batch.put(mutation.key, mutation.value);
          }
        }
        try (org.rocksdb.WriteOptions selected = new org.rocksdb.WriteOptions().setSync(sync)) {
          shared.database.write(selected, batch);
        }
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to write RocksDB Archive serving index", failure);
      }
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        releaseRocks(shared);
      }
    }
  }

  private static final class HotRocksReader implements Reader {
    private final SharedHotRocksDatabase shared;
    private final org.rocksdb.Snapshot snapshot;
    private final org.rocksdb.ReadOptions reads;
    private boolean closed;

    private HotRocksReader(SharedHotRocksDatabase shared) {
      this.shared = shared;
      snapshot = shared.resources.database.getSnapshot();
      try {
        reads = new org.rocksdb.ReadOptions().setVerifyChecksums(true).setFillCache(true)
            .setSnapshot(snapshot);
      } catch (RuntimeException failure) {
        shared.resources.database.releaseSnapshot(snapshot);
        releaseHotRocks(shared);
        throw failure;
      }
    }

    @Override
    public byte[] get(String columnFamily, byte[] key) throws IOException {
      try {
        return shared.resources.database.get(shared.resources.handle(columnFamily), reads, key);
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to read RocksDB Hot Archive column family", failure);
      }
    }

    @Override
    public KeyValue seek(String columnFamily, byte[] key) throws IOException {
      try (org.rocksdb.ReadOptions seekReads = snapshotReads(snapshot);
          org.rocksdb.RocksIterator iterator = shared.resources.database.newIterator(
              shared.resources.handle(columnFamily), seekReads)) {
        iterator.seek(key);
        if (!iterator.isValid()) {
          iterator.status();
          return null;
        }
        return new KeyValue(iterator.key(), iterator.value());
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to seek RocksDB Hot Archive column family", failure);
      }
    }

    @Override
    public Cursor cursor() {
      return new RocksCursor(shared.resources.database,
          shared.resources.handle(DEFAULT_COLUMN_FAMILY), snapshotReads(snapshot), true);
    }

    @Override
    public OptionalLong readLongProperty(String name) {
      try {
        return OptionalLong.of(shared.resources.database.getLongProperty(
            shared.resources.handle(DEFAULT_COLUMN_FAMILY), name));
      } catch (org.rocksdb.RocksDBException | IllegalArgumentException failure) {
        return OptionalLong.empty();
      }
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        reads.close();
        shared.resources.database.releaseSnapshot(snapshot);
        releaseHotRocks(shared);
      }
    }

    private static org.rocksdb.ReadOptions snapshotReads(org.rocksdb.Snapshot snapshot) {
      return new org.rocksdb.ReadOptions().setVerifyChecksums(true).setFillCache(true)
          .setSnapshot(snapshot);
    }
  }

  private static final class HotRocksWriter implements Writer {
    private final SharedHotRocksDatabase shared;
    private boolean closed;

    private HotRocksWriter(SharedHotRocksDatabase shared) {
      this.shared = shared;
    }

    @Override
    public byte[] get(String columnFamily, byte[] key) throws IOException {
      try {
        return shared.resources.database.get(shared.resources.handle(columnFamily), key);
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to read RocksDB Hot Archive column family", failure);
      }
    }

    @Override
    public void write(List<Mutation> mutations) throws IOException {
      write(mutations, true);
    }

    @Override
    public void write(List<Mutation> mutations, boolean sync) throws IOException {
      try (org.rocksdb.WriteBatch batch = new org.rocksdb.WriteBatch()) {
        for (Mutation mutation : mutations) {
          org.rocksdb.ColumnFamilyHandle handle = shared.resources.handle(mutation.columnFamily);
          if (mutation.value == null) {
            batch.delete(handle, mutation.key);
          } else {
            batch.put(handle, mutation.key, mutation.value);
          }
        }
        try (org.rocksdb.WriteOptions selected = new org.rocksdb.WriteOptions().setSync(sync)) {
          shared.resources.database.write(selected, batch);
        }
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to write RocksDB Hot Archive column families", failure);
      }
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        releaseHotRocks(shared);
      }
    }
  }

  private static final class RocksCursor implements Cursor {
    private final org.rocksdb.ReadOptions reads;
    private final org.rocksdb.RocksIterator iterator;
    private final boolean ownsReadOptions;

    private RocksCursor(org.rocksdb.RocksDB database, org.rocksdb.ReadOptions reads,
        boolean ownsReadOptions) {
      this.reads = reads;
      this.iterator = database.newIterator(reads);
      this.ownsReadOptions = ownsReadOptions;
    }

    private RocksCursor(org.rocksdb.RocksDB database,
        org.rocksdb.ColumnFamilyHandle columnFamily, org.rocksdb.ReadOptions reads,
        boolean ownsReadOptions) {
      this.reads = reads;
      this.iterator = database.newIterator(columnFamily, reads);
      this.ownsReadOptions = ownsReadOptions;
    }

    @Override
    public void seek(byte[] key) {
      iterator.seek(key);
    }

    @Override
    public KeyValue next() throws IOException {
      if (!iterator.isValid()) {
        try {
          iterator.status();
        } catch (org.rocksdb.RocksDBException failure) {
          throw new IOException("Failed to scan RocksDB Archive serving index", failure);
        }
        return null;
      }
      KeyValue value = new KeyValue(iterator.key(), iterator.value());
      iterator.next();
      return value;
    }

    @Override
    public void close() {
      iterator.close();
      if (ownsReadOptions) {
        reads.close();
      }
    }
  }
}
