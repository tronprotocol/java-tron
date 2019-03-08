package org.tron.common.storage;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.CompressionType;

@Slf4j
public class DBSettings {

  private static DBSettings settings;

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
  private List<CompressionType> compressionTypeList;
  @Getter
  private long targetFileSizeBase;
  @Getter
  private int targetFileSizeMultiplier;
  @Getter
  private boolean enableStatistics;

  private DBSettings() {

  }

  public static DBSettings getDefaultSettings() {
    DBSettings defaultSettings = new DBSettings();
    return defaultSettings.withLevelNumber(7).withBlockSize(64).withCompactThreads(32)
        .withCompressionTypeList("no:no:no:lz4:lz4:zstd:zstd").withTargetFileSizeBase(256)
        .withMaxBytesForLevelMultiplier(10).withTargetFileSizeMultiplier(1).withMaxBytesForLevelBase(256).withMaxOpenFiles(-1)
        .withEnableStatistics(false);
  }

  public static DBSettings getSettings() {
    if (settings == null) {
      return getDefaultSettings();
    }
    return settings;
  }

  public static DBSettings initCustomSettings(int levelNumber, int compactThreads, int blocksize,
      long maxBytesForLevelBase,
      double maxBytesForLevelMultiplier, String compressionStr,
      int level0FileNumCompactionTrigger, long targetFileSizeBase, int targetFileSizeMultiplier) {
    settings = new DBSettings()
        .withMaxOpenFiles(-1)
        .withEnableStatistics(false)
        .withLevelNumber(levelNumber)
        .withCompactThreads(compactThreads)
        .withBlockSize(blocksize)
        .withMaxBytesForLevelBase(maxBytesForLevelBase)
        .withMaxBytesForLevelMultiplier(maxBytesForLevelMultiplier)
        .withCompressionTypeList(compressionStr)
        .withLevel0FileNumCompactionTrigger(level0FileNumCompactionTrigger)
        .withTargetFileSizeBase(targetFileSizeBase)
        .withTargetFileSizeMultiplier(targetFileSizeMultiplier);
    return settings;
  }


  public DBSettings withMaxOpenFiles(int maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  public DBSettings withCompactThreads(int compactThreads) {
    this.compactThreads = compactThreads;
    return this;
  }

  public DBSettings withBlockSize(long blockSize) {
    this.blockSize = blockSize * 1024;
    return this;
  }

  public DBSettings withMaxBytesForLevelBase(long maxBytesForLevelBase) {
    this.maxBytesForLevelBase = maxBytesForLevelBase * 1024 * 1024;
    return this;
  }

  public DBSettings withMaxBytesForLevelMultiplier(double maxBytesForLevelMultiplier) {
    this.maxBytesForLevelMultiplier = maxBytesForLevelMultiplier;
    return this;
  }

  public DBSettings withLevel0FileNumCompactionTrigger(int level0FileNumCompactionTrigger) {
    this.level0FileNumCompactionTrigger = level0FileNumCompactionTrigger;
    return this;
  }

  public DBSettings withCompressionTypeList(String compressionTypeListString) {
    List<String> compressionTypeStringList = Arrays.asList(compressionTypeListString.split(":"));
    List<CompressionType> compressionTypeList = new ArrayList<>();
    for (String str : compressionTypeStringList) {
      if (str.equals("no")) {
        compressionTypeList.add(CompressionType.NO_COMPRESSION);
      } else if (str.equals("lz4")) {
        compressionTypeList.add(CompressionType.LZ4_COMPRESSION);
      } else if (str.equals("zstd")) {
        compressionTypeList.add(CompressionType.ZSTD_COMPRESSION);
      } else if (str.equals("zlib")) {
        compressionTypeList.add(CompressionType.ZLIB_COMPRESSION);
      }
    }
    this.compressionTypeList = compressionTypeList;
    return this;
  }

  public DBSettings withEnableStatistics(boolean enable) {
    this.enableStatistics = enable;
    return this;
  }

  public DBSettings withLevelNumber(int levelNumber) {
    this.levelNumber = levelNumber;
    return this;
  }


  public DBSettings withTargetFileSizeBase(long targetFileSizeBase) {
    this.targetFileSizeBase = targetFileSizeBase * 1024 * 1024;
    return this;
  }

  public DBSettings withTargetFileSizeMultiplier(int targetFileSizeMultiplier) {
    this.targetFileSizeMultiplier = targetFileSizeMultiplier;
    return this;
  }

  public static void loggingSettings() {
    logger.info(String.format(
        "level number: %d, CompactThreads: %d, Blocksize: %d, maxBytesForLevelBase: %d, withMaxBytesForLevelMultiplier: %f, level0FileNumCompactionTrigger: %d, withTargetFileSizeBase: %d, withTargetFileSizeMultiplier: %d",
        settings.getLevelNumber(),
        settings.getCompactThreads(), settings.getBlockSize(), settings.getMaxBytesForLevelBase(),
        settings.getMaxBytesForLevelMultiplier(), settings.getLevel0FileNumCompactionTrigger(),
        settings.getTargetFileSizeBase(), settings.getTargetFileSizeMultiplier()));
  }
}
