package org.tron.common.setting;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RocksDbSettings {

  @Setter
  @Getter
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

  private RocksDbSettings() {

  }

  public static RocksDbSettings getDefaultSettings() {
    RocksDbSettings defaultSettings = new RocksDbSettings();
    return defaultSettings.withLevelNumber(7).withBlockSize(64).withCompactThreads(32)
        .withTargetFileSizeBase(256).withMaxBytesForLevelMultiplier(10)
        .withTargetFileSizeMultiplier(1)
        .withMaxBytesForLevelBase(256).withMaxOpenFiles(-1).withEnableStatistics(false);
  }

  public static RocksDbSettings getSettings() {
    return rocksDbSettings == null ? getDefaultSettings() : rocksDbSettings;
  }

  public static RocksDbSettings initCustomSettings(int levelNumber, int compactThreads,
      int blockSize, long maxBytesForLevelBase,
      double maxBytesForLevelMultiplier, int level0FileNumCompactionTrigger,
      long targetFileSizeBase,
      int targetFileSizeMultiplier) {
    rocksDbSettings = new RocksDbSettings()
        .withMaxOpenFiles(-1)
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
    logger.info(String.format(
        "level number: %d, CompactThreads: %d, Blocksize: %d, maxBytesForLevelBase: %d,"
            + " withMaxBytesForLevelMultiplier: %f, level0FileNumCompactionTrigger: %d, "
            + "withTargetFileSizeBase: %d, withTargetFileSizeMultiplier: %d",
        rocksDbSettings.getLevelNumber(),
        rocksDbSettings.getCompactThreads(), rocksDbSettings.getBlockSize(),
        rocksDbSettings.getMaxBytesForLevelBase(),
        rocksDbSettings.getMaxBytesForLevelMultiplier(),
        rocksDbSettings.getLevel0FileNumCompactionTrigger(),
        rocksDbSettings.getTargetFileSizeBase(), rocksDbSettings.getTargetFileSizeMultiplier()));
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
}
