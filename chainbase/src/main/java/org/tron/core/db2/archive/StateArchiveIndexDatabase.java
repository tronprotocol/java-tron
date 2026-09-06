package org.tron.core.db2.archive;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Engine-neutral native store for Archive serving indexes. */
final class StateArchiveIndexDatabase {

  private static final Logger logger = LoggerFactory.getLogger("DB");
  private static final Map<Path, SharedLevelDatabase> LEVEL_DATABASES = new HashMap<>();
  private static final Map<Path, SharedRocksDatabase> ROCKS_DATABASES = new HashMap<>();

  private StateArchiveIndexDatabase() {
  }

  static Reader openReader(Path directory, Engine engine) throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = configuredOptions();
    return engine == Engine.LEVELDB ? new LevelReader(acquireLevel(path, false, config))
        : new RocksReader(acquireRocks(path, false, config));
  }

  static Writer openWriter(Path directory, Engine engine) throws IOException {
    Path path = normalize(directory);
    NativeDbConfig config = configuredOptions();
    return engine == Engine.LEVELDB ? new LevelWriter(acquireLevel(path, true, config))
        : new RocksWriter(acquireRocks(path, true, config));
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
    return new Mutation(key, value);
  }

  private static Path normalize(Path directory) {
    return Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
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
      logger.info("Archive serving index opened: directory={}, engine=LEVELDB, blockBytes={}, "
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
            org.rocksdb.RocksDB.open(resources.options, directory.toString()), resources);
      } catch (org.rocksdb.RocksDBException | RuntimeException failure) {
        resources.close();
        throw new IOException("Failed to open RocksDB Archive serving index", failure);
      }
      ROCKS_DATABASES.put(directory, shared);
      logger.info("Archive serving index opened: directory={}, engine=ROCKSDB, blockBytes={}, "
              + "writeBufferBytes={}, cacheBytes={}, maxOpenFiles={}", directory,
          config.getBlockSize(), config.getWriteBufferSize(), config.getCacheSize(),
          config.getMaxOpenFiles());
    }
    shared.references++;
    return shared;
  }

  private static synchronized void releaseRocks(SharedRocksDatabase shared) {
    if (--shared.references != 0) {
      return;
    }
    ROCKS_DATABASES.remove(shared.directory);
    shared.database.close();
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

    byte[] get(byte[] key) throws IOException;

    KeyValue seek(byte[] key) throws IOException;

    Cursor cursor() throws IOException;

    OptionalLong readLongProperty(String name) throws IOException;
  }

  interface Writer extends Closeable {

    byte[] get(byte[] key) throws IOException;

    void write(List<Mutation> mutations) throws IOException;

    void write(List<Mutation> mutations, boolean sync) throws IOException;
  }

  interface Cursor extends Closeable {

    void seek(byte[] key) throws IOException;

    KeyValue next() throws IOException;
  }

  static final class Mutation {
    private final byte[] key;
    private final byte[] value;

    private Mutation(byte[] key, byte[] value) {
      this.key = Arrays.copyOf(Objects.requireNonNull(key, "key"), key.length);
      this.value = Arrays.copyOf(Objects.requireNonNull(value, "value"), value.length);
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
    private int references;

    private SharedRocksDatabase(Path directory, org.rocksdb.RocksDB database,
        RocksResources resources) {
      this.directory = directory;
      this.database = database;
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
    public byte[] get(byte[] key) {
      return shared.database.get(key, reads);
    }

    @Override
    public KeyValue seek(byte[] key) throws IOException {
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
    public byte[] get(byte[] key) {
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
          batch.put(mutation.key, mutation.value);
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
    public byte[] get(byte[] key) throws IOException {
      try {
        return shared.database.get(reads, key);
      } catch (org.rocksdb.RocksDBException failure) {
        throw new IOException("Failed to read RocksDB Archive serving index", failure);
      }
    }

    @Override
    public KeyValue seek(byte[] key) throws IOException {
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
    public byte[] get(byte[] key) throws IOException {
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
          batch.put(mutation.key, mutation.value);
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
