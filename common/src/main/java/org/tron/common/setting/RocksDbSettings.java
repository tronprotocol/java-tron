package org.tron.common.setting;

import java.util.Arrays;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.Statistics;
import org.tron.common.utils.MarketOrderPriceComparatorForRocksDB;
import org.tron.core.Constant;

@Slf4j
public class RocksDbSettings {

  private static RocksDbSettings rocksDbSettings;

  @Getter
  private int levelNumber;
  @Getter
  private int maxOpenFiles;
  @Getter
  private int compactThreads;
  @Getter
  private long blockSize;
  @Getter
  private long maxBytesForLevelBase;
  @Getter
  private double maxBytesForLevelMultiplier;
  @Getter
  private int level0FileNumCompactionTrigger;
  @Getter
  private long targetFileSizeBase;
  @Getter
  private int targetFileSizeMultiplier;
  @Getter
  private boolean enableStatistics;

  static {
    RocksDB.loadLibrary();
  }

  private static final LRUCache cache = new LRUCache(1 * 1024 * 1024 * 1024L);

  private static final String[] CI_ENVIRONMENT_VARIABLES = {
      "CI",
      "JENKINS_URL",
      "TRAVIS",
      "CIRCLECI",
      "GITHUB_ACTIONS",
      "GITLAB_CI"
  };

  private RocksDbSettings() {

  }

  public static RocksDbSettings getDefaultSettings() {
    RocksDbSettings defaultSettings = new RocksDbSettings();
    return defaultSettings.withLevelNumber(7).withBlockSize(64).withCompactThreads(32)
        .withTargetFileSizeBase(256).withMaxBytesForLevelMultiplier(10)
        .withTargetFileSizeMultiplier(1)
        .withMaxBytesForLevelBase(256).withMaxOpenFiles(5000).withEnableStatistics(false);
  }

  public static RocksDbSettings getSettings() {
    return rocksDbSettings == null ? getDefaultSettings() : rocksDbSettings;
  }

  public static RocksDbSettings initCustomSettings(int levelNumber, int compactThreads,
      int blockSize, long maxBytesForLevelBase,
      double maxBytesForLevelMultiplier, int level0FileNumCompactionTrigger,
      long targetFileSizeBase,
      int targetFileSizeMultiplier, int maxOpenFiles) {
    rocksDbSettings = new RocksDbSettings()
        .withMaxOpenFiles(maxOpenFiles)
        .withEnableStatistics(false)
        .withLevelNumber(levelNumber)
        .withCompactThreads(compactThreads)
        .withBlockSize(blockSize)
        .withMaxBytesForLevelBase(maxBytesForLevelBase)
        .withMaxBytesForLevelMultiplier(maxBytesForLevelMultiplier)
        .withLevel0FileNumCompactionTrigger(level0FileNumCompactionTrigger)
        .withTargetFileSizeBase(targetFileSizeBase)
        .withTargetFileSizeMultiplier(targetFileSizeMultiplier);
    return rocksDbSettings;
  }

  public static void loggingSettings() {
    logger.info(
        "level number: {}, CompactThreads: {}, Blocksize:{}, maxBytesForLevelBase: {},"
            + " withMaxBytesForLevelMultiplier: {}, level0FileNumCompactionTrigger: {}, "
            + "withTargetFileSizeBase: {}, withTargetFileSizeMultiplier: {}, maxOpenFiles: {}",
        rocksDbSettings.getLevelNumber(),
        rocksDbSettings.getCompactThreads(), rocksDbSettings.getBlockSize(),
        rocksDbSettings.getMaxBytesForLevelBase(),
        rocksDbSettings.getMaxBytesForLevelMultiplier(),
        rocksDbSettings.getLevel0FileNumCompactionTrigger(),
        rocksDbSettings.getTargetFileSizeBase(), rocksDbSettings.getTargetFileSizeMultiplier(),
        rocksDbSettings.getMaxOpenFiles());
  }

  public RocksDbSettings withMaxOpenFiles(int maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  public RocksDbSettings withCompactThreads(int compactThreads) {
    this.compactThreads = compactThreads;
    return this;
  }

  public RocksDbSettings withBlockSize(long blockSize) {
    this.blockSize = blockSize * 1024;
    return this;
  }

  public RocksDbSettings withMaxBytesForLevelBase(long maxBytesForLevelBase) {
    this.maxBytesForLevelBase = maxBytesForLevelBase * 1024 * 1024;
    return this;
  }

  public RocksDbSettings withMaxBytesForLevelMultiplier(double maxBytesForLevelMultiplier) {
    this.maxBytesForLevelMultiplier = maxBytesForLevelMultiplier;
    return this;
  }

  public RocksDbSettings withLevel0FileNumCompactionTrigger(int level0FileNumCompactionTrigger) {
    this.level0FileNumCompactionTrigger = level0FileNumCompactionTrigger;
    return this;
  }

  public RocksDbSettings withEnableStatistics(boolean enable) {
    this.enableStatistics = enable;
    return this;
  }

  public RocksDbSettings withLevelNumber(int levelNumber) {
    this.levelNumber = levelNumber;
    return this;
  }

  public RocksDbSettings withTargetFileSizeBase(long targetFileSizeBase) {
    this.targetFileSizeBase = targetFileSizeBase * 1024 * 1024;
    return this;
  }

  public RocksDbSettings withTargetFileSizeMultiplier(int targetFileSizeMultiplier) {
    this.targetFileSizeMultiplier = targetFileSizeMultiplier;
    return this;
  }
  public static LRUCache getCache() {
    return cache;
  }

  public static Options getOptionsByDbName(String dbName) {
    RocksDbSettings settings = getSettings();

    Options options = new Options();

    // most of these options are suggested by https://github.com/facebook/rocksdb/wiki/Set-Up-Options

    // general options
    if (settings.isEnableStatistics()) {
      options.setStatistics(new Statistics());
      options.setStatsDumpPeriodSec(60);
    }
    options.setCreateIfMissing(true);
    options.setIncreaseParallelism(1);
    options.setLevelCompactionDynamicLevelBytes(true);
    options.setMaxOpenFiles(settings.getMaxOpenFiles());

    // general options supported user config
    options.setNumLevels(settings.getLevelNumber());
    options.setMaxBytesForLevelMultiplier(settings.getMaxBytesForLevelMultiplier());
    options.setMaxBytesForLevelBase(settings.getMaxBytesForLevelBase());
    options.setMaxBackgroundCompactions(settings.getCompactThreads());
    options.setLevel0FileNumCompactionTrigger(settings.getLevel0FileNumCompactionTrigger());
    options.setTargetFileSizeMultiplier(settings.getTargetFileSizeMultiplier());
    options.setTargetFileSizeBase(settings.getTargetFileSizeBase());

    // table options
    final BlockBasedTableConfig tableCfg;
    options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
    tableCfg.setBlockSize(settings.getBlockSize());
    tableCfg.setBlockCache(RocksDbSettings.getCache());
    tableCfg.setCacheIndexAndFilterBlocks(true);
    tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
    tableCfg.setFilter(new BloomFilter(10, false));
    if (Constant.MARKET_PAIR_PRICE_TO_ORDER.equals(dbName)) {
      ComparatorOptions comparatorOptions = new ComparatorOptions();
      options.setComparator(new MarketOrderPriceComparatorForRocksDB(comparatorOptions));
    }

    if (isRunningInCI()) {
      // Disable fallocate calls  to avoid issues with disk space
      options.setAllowFAllocate(false);
      // Set WAL size limits to avoid excessive disk
      options.setMaxTotalWalSize(2 * 1024 * 1024);
      // Set recycle log file
      options.setRecycleLogFileNum(1);
      // Enable creation of missing column families
      options.setCreateMissingColumnFamilies(true);
      // Set max background flushes to 1 to reduce resource usage
      options.setMaxBackgroundFlushes(1);
    }

    return options;
  }

  private static boolean isRunningInCI() {
    return Arrays.stream(CI_ENVIRONMENT_VARIABLES).anyMatch(System.getenv()::containsKey);
  }
}
