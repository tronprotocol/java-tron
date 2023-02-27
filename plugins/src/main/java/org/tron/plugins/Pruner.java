package org.tron.plugins;

import static org.tron.plugins.Constant.DB_DIRECTORY_CONFIG_KEY;
import static org.tron.plugins.Constant.PROPERTIES_CONFIG_KEY;
import static org.tron.plugins.Constant.STATE_GENESIS_META_FILE;
import static org.tron.plugins.Constant.STATE_GENESIS_PATH_KEY;
import static org.tron.plugins.Constant.STATE_TRIE_DB_NAME;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.trie.StoredMerklePatriciaTrie;
import org.hyperledger.besu.storage.KeyValueStorage;
import org.hyperledger.besu.storage.KeyValueStorageTransaction;
import org.hyperledger.besu.storage.RocksDBConfigurationBuilder;
import org.hyperledger.besu.storage.RocksDBKeyValueStorage;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.FileUtils;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DbTool;
import org.tron.protos.Protocol;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j(topic = "prune")
@CommandLine.Command(name = "prune", description = "A helper to prune the archive db.")
public class Pruner implements Callable<Integer> {

  private static final byte[] IN_USE = Bytes.of(1).toArrayUnsafe();

  private static final String BLOCK_DB_NAME = "block";
  private static final String BLOCK_INDEX_DB_NAME = "block-index";
  private static final String TRIE_DB_NAME = "world-state-trie";
  private static final String DEFAULT_STATE_GENESIS_DIRECTORY = "state-genesis";
  private static Path RESERVED_KEY_STORE_PATH;

  private final ReadWriteLock pendingMarksLock = new ReentrantReadWriteLock();
  private final Set<Bytes32> pendingMarks = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private static final int DEFAULT_OPS_PER_TRANSACTION = 10_000;

  private MerkleStorage srcMerkleStorage;
  private KeyValueStorage srcKvStorage;
  private MerkleStorage destMerkleStorage;
  private KeyValueStorage destKvStorage;
  private RocksDB destDb;
  private KeyValueStorage markedKeyStore;

  final ThreadPoolExecutor markingExecutorService =
      new ThreadPoolExecutor(
          0,
          Runtime.getRuntime().availableProcessors() * 2,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingDeque<>(100),
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat(this.getClass().getSimpleName() + "-mark-%d")
              .build(),
          new ThreadPoolExecutor.CallerRunsPolicy()
      );

  final ThreadPoolExecutor checkExecutorService =
      new ThreadPoolExecutor(
          0,
          Runtime.getRuntime().availableProcessors() * 2,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingDeque<>(100),
          new ThreadFactoryBuilder()
              .setDaemon(true)
              // .setPriority(Thread.MIN_PRIORITY)
              .setNameFormat(this.getClass().getSimpleName() + "-check-%d")
              .build(),
          new ThreadPoolExecutor.CallerRunsPolicy()
      );

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-d", "--output-directory"},
      required = true,
      defaultValue = "output-directory",
      converter = DbMove.PathConverter.class,
      order = 1,
      description = "source output directory. Default: ${DEFAULT-VALUE}")
  static Path srcDirectory;

  @Option(names = {"-p", "--state-directory-pruned"},
      required = true,
      defaultValue = "state-genesis-pruned",
      order = 2,
      description = "pruned state directory. Default: ${DEFAULT-VALUE}")
  private String prunedDir;

  @CommandLine.Option(names = {"-c", "--config"},
      required = true,
      defaultValue = "config.conf",
      converter = ConfigConverter.class,
      order = 0,
      description = "config file. Default: ${DEFAULT-VALUE}")
  static Config config;

  @CommandLine.Option(
      names = {"-n", "--number-reserved"},
      required = true,
      order = 3,
      description = "the number of block state data need to be reserved.")
  private long reserveNumber;

  @CommandLine.Option(
      names = {"-k", "--check"},
      order = 4,
      description = "check trie data integrity")
  private boolean check;

  @Option(names = {"-h", "--help"})
  static boolean help;


  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    if (!checkPrunedDir(prunedDir)) {
      return 300;
    }

    String msg;
    Path databasePath = Paths.get(srcDirectory.toString(),
        config.getString(DB_DIRECTORY_CONFIG_KEY));
    Path prunedPath = Paths.get(prunedDir);
    Path statePath;
    if (config.hasPath(STATE_GENESIS_PATH_KEY)) {
      statePath = Paths.get(srcDirectory.toString(), config.getString(STATE_GENESIS_PATH_KEY));
    } else {
      statePath = Paths.get(srcDirectory.toString(), DEFAULT_STATE_GENESIS_DIRECTORY);
    }

    // check reserve number
    Map<String, Long> result = checkAndGetBlockNumberRange(
        databasePath.toString(), statePath.toString(), reserveNumber);
    if (result == null || result.size() == 0) {
      return 404;
    }

    // init persistant storage, only for once
    initKeyValueStorage(statePath.toString());
    initReservedKeyStore(prunedPath);

    // mark the trie data
    long startIndex = result.get("end") - reserveNumber + 1;
    long endIndex = result.get("end");
    print(String.format("Prune begin, start number: %d, end number: %d",
        startIndex, endIndex), false);
    long startTime = System.currentTimeMillis();
    for (long index = startIndex; index <= endIndex; index++) {
      Bytes32 root = Bytes32.wrap(
          getBlockByNumber(index).getBlockHeader().getArchiveRoot().toByteArray());
      MerklePatriciaTrie<Bytes, Bytes> srcTrie = getTrie(srcMerkleStorage, root);
      print("marking block number: " + index + ", root: " + root, false);

      srcTrie.visitAll((node) -> {
        markNodes(node.getHash());
      }, markingExecutorService).join();
    }

    // wait markingExecutorService task finished
    awaitTask(markingExecutorService);

    // flush the final task data
    flushPendingMarks();
    print(String.format("mark trie finish, cost: %d ms",
        System.currentTimeMillis() - startTime), false);

    // copy the marked data to dest merkle store
    copy();

    // check trie data correction
    if (check) {
      destMerkleStorage = new KeyValueMerkleStorage(getDestKvStorage(prunedDir));
      for (long index = startIndex; index <= endIndex; index++) {
        Bytes32 root = Bytes32.wrap(
            getBlockByNumber(index).getBlockHeader().getArchiveRoot().toByteArray());
        startTime = System.currentTimeMillis();
        MerklePatriciaTrie<Bytes, Bytes> destTrie = getTrie(
            destMerkleStorage, root);
        destTrie.visitAll(node -> {
          node.getHash();
          node.getValue();
        }, checkExecutorService).join();
      }
      awaitTask(checkExecutorService);
      msg = String.format("check trie data correction, cost: %d",
          System.currentTimeMillis() - startTime);
      print(msg, false);
      destKvStorage.close();
    }

    // copy state genesis data
    if (!copyStateGenesis(statePath, prunedPath)) {
      return 501;
    }

    // generate state meta file
    Protocol.Block stateGenesisBlock = getBlockByNumber(startIndex);
    if (!generateMetaFile(prunedPath, stateGenesisBlock)) {
      return 501;
    }

    // release resource
    releaseResource();

    msg = "prune success!";
    print(msg, false);

    return 0;
  }

  private void copy() throws RocksDBException {
    print("copy marked data start.", false);
    // init destDb
    initDestDb(prunedDir);
    AtomicLong count = new AtomicLong();
    long start = System.currentTimeMillis();
    markedKeyStore.streamKeys().parallel().forEach(key -> {
      Optional<byte[]> v =
          Optional.ofNullable(srcKvStorage.get(key)).orElse(Optional.empty());
      try {
        if (v.isPresent()) {
          destDb.put(key, v.get());
        }
      } catch (RocksDBException e) {
        print(String.format("copy marked data failed, err: %s", e.getMessage()), true);
        throw new RuntimeException(e);
      }
      count.incrementAndGet();
    });
    destDb.close();
    print(String.format("copy marked data finish, record number: %d, cost: %d",
        count.get(), System.currentTimeMillis() - start), false);
  }

  private void markNodes(final Bytes32 hash) {
    markThenMaybeFlush(() -> pendingMarks.add(hash), 1);
  }

  private void markThenMaybeFlush(final Runnable nodeMarker, final int numberOfNodes) {
    // We use the read lock here because pendingMarks is threadsafe and we want to allow all the
    // marking threads access simultaneously.
    final Lock markLock = pendingMarksLock.readLock();
    markLock.lock();
    try {
      nodeMarker.run();
    } finally {
      markLock.unlock();
    }

    // However, when the size of pendingMarks grows too large, we want all the threads to stop
    // adding because we're going to clear the set.
    // Therefore, we need to take out a write lock.
    if (pendingMarks.size() >= DEFAULT_OPS_PER_TRANSACTION) {
      final Lock flushLock = pendingMarksLock.writeLock();
      flushLock.lock();
      try {
        // Check once again that the condition holds. If it doesn't, that means another thread
        // already flushed them.
        if (pendingMarks.size() >= DEFAULT_OPS_PER_TRANSACTION) {
          flushPendingMarks();
        }
      } finally {
        flushLock.unlock();
      }
    }
  }

  private void flushPendingMarks() {
    final KeyValueStorageTransaction transaction = markedKeyStore.startTransaction();
    pendingMarks.forEach(node -> transaction.put(node.toArrayUnsafe(), IN_USE));
    transaction.commit();
    pendingMarks.clear();
  }

  private void awaitTask(ThreadPoolExecutor executor) throws InterruptedException {
    Thread.sleep(10000);
    while (executor.getActiveCount() > 0 || executor.getQueue().size() > 0) {
      Thread.sleep(10000);
    }
    executor.shutdown();
  }

  private boolean copyStateGenesis(Path statePath, Path prunedPath) {
    List<DbCopy.Copier> services = new ArrayList<>();
    Arrays.stream(Objects.requireNonNull(statePath.toFile().listFiles()))
        .filter(File::isDirectory)
        .filter(f -> !STATE_TRIE_DB_NAME.equals(f.getName()))
        .forEach(f -> services.add(
            new DbCopy.DbCopier(
                statePath.toFile().getPath(), prunedPath.toFile().getPath(), f.getName())));
    List<String> fails = ProgressBar
        .wrap(services.stream(), "copy task")
        .parallel()
        .map(
            dbCopier -> {
              try {
                return dbCopier.doCopy() ? null : dbCopier.name();
              } catch (Exception e) {
                print(String.format("copy state genesis failed, db: %s, err: %s",
                    dbCopier.name(), e.getMessage()), true);
                return dbCopier.name();
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return fails.isEmpty();
  }

  private boolean generateMetaFile(Path prunedPath, Protocol.Block block) {
    String propertyfile = Paths.get(prunedPath.toString(),
        STATE_GENESIS_META_FILE).toString();
    if (!FileUtils.createFileIfNotExists(propertyfile)) {
      print("Create properties file failed.", true);
      return false;
    }

    Map<String, String> properties = new HashMap<>();
    properties.put("height",
        String.valueOf(block.getBlockHeader().getRawData().getNumber()));
    properties.put("hash",
        Bytes32.wrap(block.getBlockHeader().getArchiveRoot().toByteArray()).toHexString());
    properties.put("time",
        String.valueOf(block.getBlockHeader().getRawData().getTimestamp()));

    if (!FileUtils.writeProperties(propertyfile, properties)) {
      print("Write properties file failed.", true);
      return false;
    }
    return true;
  }

  private void initKeyValueStorage(String stateDir) {
    if (srcKvStorage == null) {
      Path triePath = Paths.get(stateDir, TRIE_DB_NAME);
      srcKvStorage = new RocksDBKeyValueStorage(
          new RocksDBConfigurationBuilder()
              .databaseDir(triePath)
              .build());
      srcMerkleStorage = new KeyValueMerkleStorage(srcKvStorage);
    }
  }

  private RocksDB initDestDb(String prunedDirectory) throws RocksDBException {
    Path triePath = Paths.get(prunedDirectory, TRIE_DB_NAME);
    destDb = RocksDB.open(triePath.toString());
    return destDb;
  }

  private KeyValueStorage getDestKvStorage(String prunedDirectory) {
    Path triePath = Paths.get(prunedDirectory, TRIE_DB_NAME);
    destKvStorage = new RocksDBKeyValueStorage(
        new RocksDBConfigurationBuilder().databaseDir(triePath).build());
    return destKvStorage;
  }

  private MerklePatriciaTrie<Bytes, Bytes> getTrie(
      MerkleStorage merkleStorage, Bytes32 root) {
    return new StoredMerklePatriciaTrie<>(
        merkleStorage::get, root,
        Function.identity(),
        Function.identity());
  }

  private Protocol.Block getBlockByNumber(long blockNumber) throws IOException {
    try {
      DBInterface blockDb = getDb(BLOCK_DB_NAME);
      DBInterface blockIndexDb = getDb(BLOCK_INDEX_DB_NAME);
      byte[] value = blockDb.get(blockIndexDb.get(ByteArray.fromLong(blockNumber)));
      if (value == null || value.length == 0) {
        throw new IOException("can not find block, number: " + blockNumber);
      }
      return Protocol.Block.parseFrom(value);
    } catch (IOException | RocksDBException e) {
      throw new IOException("get block number failed, " + e.getMessage());
    }
  }

  /**
   * get db object (can not get state db)
   */
  private DBInterface getDb(String dbName) throws IOException, RocksDBException {
    File dbDir = getAndCheckDbPath(srcDirectory.toString(), dbName, config).toFile();
    Path dbParentPath = dbDir.toPath().getParent();
    return DbTool.getDB(dbParentPath.toString(), dbName);
  }

  private Path getDbPath(String outputDir, String dbName, Config config) {
    String confPath = String.format("%s.%s", PROPERTIES_CONFIG_KEY, dbName);
    if (config.hasPath(confPath)) {
      return Paths.get(config.getString(confPath));
    } else {
      return Paths.get(outputDir, config.getString(DB_DIRECTORY_CONFIG_KEY), dbName);
    }
  }

  private Path getAndCheckDbPath(String outputDir, String dbName, Config config)
      throws IOException {
    File file = getDbPath(outputDir, dbName, config).toFile();
    if (!file.exists() || !file.isDirectory()) {
      String errMsg = String.format("%s database does not exist.", BLOCK_DB_NAME);
      print(errMsg, true);
      throw new IOException(errMsg);
    }
    return file.toPath();
  }

  private Map<String, Long> checkAndGetBlockNumberRange(
      String databaseDir, String stateDir, long reserveNumber) {
    Map<String, Long> result = Maps.newHashMap();
    String errMsg;
    if (reserveNumber < 1) {
      errMsg = "reserveNumber must bigger than 0";
      print(errMsg, true);
      return null;
    }
    try {
      long latestBlockNumber = new DbLite().getLatestBlockHeaderNum(databaseDir);
      long stateInitNumber = getStateInitNumber(stateDir);
      if (stateInitNumber == -1) {
        return null;
      }
      if (reserveNumber > Math.subtractExact(latestBlockNumber, stateInitNumber)) {
        errMsg = String.format("reserveNumber is bigger than the block gap. "
                + "reserveNumber: %d, latestBlockNumber: %d, earliestBlockNumber: %d",
            reserveNumber, latestBlockNumber, stateInitNumber);
        print(errMsg, true);
        return null;
      }
      result.put("start", stateInitNumber);
      result.put("end", latestBlockNumber);
    } catch (IOException | RocksDBException e) {
      errMsg = String.format("checkReserveNumber failed, err: %s", e.getMessage());
      print(errMsg, true);
      return null;
    }
    return result;
  }

  private long getStateInitNumber(String stateDir) {
    File f = Paths.get(stateDir, STATE_GENESIS_META_FILE).toFile();
    if (!f.exists()) {
      String err = "state genesis meta file not exist.";
      print(err, true);
      return -1;
    }
    return Long.parseLong(FileUtils.readProperty(f.getPath(), "height"));
  }

  private void initReservedKeyStore(Path path) {
    RESERVED_KEY_STORE_PATH = Paths.get(path.toString(),
        String.format(".reserved-keys-%s", System.currentTimeMillis()));
    markedKeyStore = new RocksDBKeyValueStorage(
        new RocksDBConfigurationBuilder().databaseDir(RESERVED_KEY_STORE_PATH).build());
  }

  private void destoryPrunedKeyStore() throws IOException {
    markedKeyStore.close();
    FileUtils.deleteDir(RESERVED_KEY_STORE_PATH.toFile());
  }

  private void releaseResource() throws IOException {
    DbTool.close();
    destoryPrunedKeyStore();
    srcKvStorage.close();
    destDb.close();
  }

  private void print(String msg, boolean err) {
    if (err) {
      spec.commandLine().getErr().println(
          spec.commandLine().getColorScheme().errorText(msg));
    } else {
      spec.commandLine().getOut().println(msg);
    }
  }

  private boolean checkPrunedDir(String prunedDir) {
    File file  = Paths.get(prunedDir).toFile();
    if (file.exists()) {
      print("Pruned output path is already exist!", true);
      return false;
    } else if (!file.mkdirs()) {
      print("Pruned output path create failed!", true);
      return false;
    }
    return true;
  }

  static class ConfigConverter implements CommandLine.ITypeConverter<Config> {
    ConfigConverter() {
    }

    public Config convert(String value) throws Exception {
      if (help) {
        return null;
      }
      File file  = Paths.get(value).toFile();
      if (file.exists() && file.isFile()) {
        return ConfigFactory.parseFile(Paths.get(value).toFile());
      } else {
        throw new IOException("Parse Config [" + value + "] failed!");
      }
    }
  }

}