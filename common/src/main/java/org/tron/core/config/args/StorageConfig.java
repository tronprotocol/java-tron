package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.math.StrictMathWrapper;

/**
 * Storage configuration bean.
 * Field names match config.conf keys under the "storage" section.
 * Covers db, index, properties, dbSettings, backup, checkpoint, txCache, etc.
 */
@Slf4j
@Getter
@Setter
public class StorageConfig {

  private DbConfig db = new DbConfig();
  private IndexConfig index = new IndexConfig();
  private TransHistoryConfig transHistory = new TransHistoryConfig();
  private boolean needToUpdateAsset = true;
  private DbSettingsConfig dbSettings = new DbSettingsConfig();
  private BalanceConfig balance = new BalanceConfig();
  private CheckpointConfig checkpoint = new CheckpointConfig();
  private SnapshotConfig snapshot = new SnapshotConfig();
  private TxCacheConfig txCache = new TxCacheConfig();
  private List<PropertyConfig> properties = new ArrayList<>();

  // merkleRoot is a nested object (e.g. { reward-vi = "hash..." }) not a string.
  // Excluded from auto-binding, handled by Storage class directly.
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private Object merkleRoot;

  // Raw storage config sub-tree, kept for setCacheStrategies/setDbRoots which
  // have dynamic keys that ConfigBeanFactory cannot bind.
  @Setter(lombok.AccessLevel.NONE)
  private Config rawStorageConfig;

  // LevelDB per-database option overrides (default, defaultM, defaultL).
  // @Setter(NONE): optional keys commented out in reference.conf; ConfigBeanFactory
  // would throw if it required them. Values are assigned in fromConfig().
  @Setter(lombok.AccessLevel.NONE)
  private DbOptionOverride defaultDbOption;
  @Setter(lombok.AccessLevel.NONE)
  private DbOptionOverride defaultMDbOption;
  @Setter(lombok.AccessLevel.NONE)
  private DbOptionOverride defaultLDbOption;

  @Getter
  @Setter
  public static class DbConfig {
    private String engine = "LEVELDB";
    private boolean sync = false;
    private String directory = "database";
  }

  @Getter
  @Setter
  public static class IndexConfig {
    private String directory = "index";
    // "switch" is a Java keyword, but HOCON key is "index.switch"
    // ConfigBeanFactory would look for setSwitch which works fine in Java
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private String switchValue = "on";

    public String getSwitch() {
      return switchValue;
    }

    public void setSwitch(String v) {
      this.switchValue = v;
    }
  }

  @Getter
  @Setter
  public static class TransHistoryConfig {
    // "switch" is a Java keyword — same handling as IndexConfig
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private String switchValue = "on";

    public String getSwitch() {
      return switchValue;
    }

    public void setSwitch(String v) {
      this.switchValue = v;
    }
  }

  @Getter
  @Setter
  public static class DbSettingsConfig {
    private int levelNumber = 7;
    private int compactThreads = 0; // 0 = auto: max(availableProcessors, 1)
    private int blocksize = 16;
    private long maxBytesForLevelBase = 256;
    private double maxBytesForLevelMultiplier = 10;
    private int level0FileNumCompactionTrigger = 2;
    private long targetFileSizeBase = 64;
    private int targetFileSizeMultiplier = 1;
    private int maxOpenFiles = 5000;

    // Expand 0 → auto-detected processor count. Mirrors develop Args.java:1609-1611.
    void postProcess() {
      if (compactThreads == 0) {
        compactThreads = StrictMathWrapper.max(Runtime.getRuntime().availableProcessors(), 1);
      }
    }
  }

  @Getter
  @Setter
  public static class BalanceConfig {
    private HistoryConfig history = new HistoryConfig();

    @Getter
    @Setter
    public static class HistoryConfig {
      private boolean lookup = false;
    }
  }

  @Getter
  @Setter
  public static class CheckpointConfig {
    private int version = 1;
    private boolean sync = true;
  }

  @Getter
  @Setter
  public static class SnapshotConfig {
    private int maxFlushCount = 1;

    // Reject out-of-range values. Mirrors develop Storage.getSnapshotMaxFlushCountFromConfig.
    void postProcess() {
      if (maxFlushCount <= 0) {
        throw new IllegalArgumentException("MaxFlushCount value can not be negative or zero!");
      }
      if (maxFlushCount > 500) {
        throw new IllegalArgumentException("MaxFlushCount value must not exceed 500!");
      }
    }
  }

  @Getter
  @Setter
  public static class TxCacheConfig {
    private int estimatedTransactions = 1000;
    private boolean initOptimization = false;

    // Clamp to [100, 10000]. Mirrors develop Storage.getEstimatedTransactionsFromConfig.
    void postProcess() {
      if (estimatedTransactions > 10000) {
        estimatedTransactions = 10000;
      } else if (estimatedTransactions < 100) {
        estimatedTransactions = 100;
      }
    }
  }

  @Getter
  @Setter
  public static class PropertyConfig {
    private String name = "";
    private String path = "";
    private boolean createIfMissing = true;
    private boolean paranoidChecks = true;
    private boolean verifyChecksums = true;
    private int compressionType = 1;
    private int blockSize = 4096;
    private int writeBufferSize = 10485760;
    private long cacheSize = 10485760;
    private int maxOpenFiles = 100;
  }

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  public static StorageConfig fromConfig(Config config) {
    Config section = config.getConfig("storage");

    StorageConfig sc = ConfigBeanFactory.create(section, StorageConfig.class);
    sc.rawStorageConfig = section;

    // Read optional LevelDB option overrides (default, defaultM, defaultL).
    sc.defaultDbOption = readDbOption(section, "default");
    sc.defaultMDbOption = readDbOption(section, "defaultM");
    sc.defaultLDbOption = readDbOption(section, "defaultL");

    sc.dbSettings.postProcess();
    sc.snapshot.postProcess();
    sc.txCache.postProcess();
    return sc;
  }

  // Partial LevelDB option override for default/defaultM/defaultL.
  // Uses boxed types so null means "not set by user, keep existing value".
  @Getter
  @Setter
  public static class DbOptionOverride {
    private Boolean createIfMissing;
    private Boolean paranoidChecks;
    private Boolean verifyChecksums;
    private Integer compressionType;
    private Integer blockSize;
    private Integer writeBufferSize;
    private Long cacheSize;
    private Integer maxOpenFiles;
  }

  // Read optional LevelDB option override (default/defaultM/defaultL).
  // Not bean-bound: users may only set a subset of keys (e.g. just maxOpenFiles),
  // ConfigBeanFactory requires all fields present so partial overrides would fail.
  private static DbOptionOverride readDbOption(Config section, String key) {
    if (!section.hasPath(key)) {
      return null;
    }
    ConfigObject conf = section.getObject(key);
    DbOptionOverride o = new DbOptionOverride();
    if (conf.containsKey("createIfMissing")) {
      o.setCreateIfMissing(
          Boolean.parseBoolean(conf.get("createIfMissing").unwrapped().toString()));
    }
    if (conf.containsKey("paranoidChecks")) {
      o.setParanoidChecks(
          Boolean.parseBoolean(conf.get("paranoidChecks").unwrapped().toString()));
    }
    if (conf.containsKey("verifyChecksums")) {
      o.setVerifyChecksums(
          Boolean.parseBoolean(conf.get("verifyChecksums").unwrapped().toString()));
    }
    if (conf.containsKey("compressionType")) {
      String param = conf.get("compressionType").unwrapped().toString();
      try {
        o.setCompressionType(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException("compressionType", Integer.class, param);
      }
    }
    if (conf.containsKey("blockSize")) {
      String param = conf.get("blockSize").unwrapped().toString();
      try {
        o.setBlockSize(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException("blockSize", Integer.class, param);
      }
    }
    if (conf.containsKey("writeBufferSize")) {
      String param = conf.get("writeBufferSize").unwrapped().toString();
      try {
        o.setWriteBufferSize(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException("writeBufferSize", Integer.class, param);
      }
    }
    if (conf.containsKey("cacheSize")) {
      String param = conf.get("cacheSize").unwrapped().toString();
      try {
        o.setCacheSize(Long.parseLong(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException("cacheSize", Long.class, param);
      }
    }
    if (conf.containsKey("maxOpenFiles")) {
      String param = conf.get("maxOpenFiles").unwrapped().toString();
      try {
        o.setMaxOpenFiles(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException("maxOpenFiles", Integer.class, param);
      }
    }
    return o;
  }

  private static void throwIllegalArgumentException(String param, Class<?> type, String actual) {
    throw new IllegalArgumentException(
        String.format("[storage.properties] %s must be %s type, actual: %s.",
            param, type.getSimpleName(), actual));
  }
}
