package org.tron.core.config.args;

import static org.tron.common.math.Maths.max;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
  private BackupConfig backup = new BackupConfig();
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

  // LevelDB per-database option overrides (default, defaultM, defaultL)
  // These are not bean-bound because they are optional nested objects with
  // dynamic usage. Read manually in fromConfig().
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private Config rawStorageConfig;

  public Config getRawStorageConfig() {
    return rawStorageConfig;
  }

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
    private int compactThreads = Runtime.getRuntime().availableProcessors();
    private int blocksize = 16;
    private long maxBytesForLevelBase = 256;
    private double maxBytesForLevelMultiplier = 10;
    private int level0FileNumCompactionTrigger = 2;
    private long targetFileSizeBase = 64;
    private int targetFileSizeMultiplier = 1;
    private int maxOpenFiles = 5000;
  }

  @Getter
  @Setter
  public static class BackupConfig {
    private boolean enable = false;
    private String propPath = "prop.properties";
    private String bak1path = "bak1/database/";
    private String bak2path = "bak2/database/";
    private int frequency = 10000;
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
  }

  @Getter
  @Setter
  public static class TxCacheConfig {
    private int estimatedTransactions = 1000;
    private boolean initOptimization = false;
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
    private int cacheSize = 10485760;
    private int maxOpenFiles = 100;
  }

  private static final Config DEFAULTS;

  static {
    int cpus = (int) max(Runtime.getRuntime().availableProcessors(), 1, true);
    DEFAULTS = ConfigFactory.parseString(
        "db { engine = LEVELDB, sync = false, directory = database }\n"
            + "index { directory = index, switch = on }\n"
            + "transHistory { switch = on }\n"
            + "needToUpdateAsset = true\n"
            + "dbSettings { levelNumber = 7, compactThreads = " + cpus
            + ", blocksize = 16, maxBytesForLevelBase = 256,"
            + " maxBytesForLevelMultiplier = 10, level0FileNumCompactionTrigger = 2,"
            + " targetFileSizeBase = 64, targetFileSizeMultiplier = 1, maxOpenFiles = 5000 }\n"
            + "backup { enable = false, propPath = \"prop.properties\","
            + " bak1path = \"bak1/database/\", bak2path = \"bak2/database/\","
            + " frequency = 10000 }\n"
            + "balance { history { lookup = false } }\n"
            + "checkpoint { version = 1, sync = true }\n"
            + "snapshot { maxFlushCount = 1 }\n"
            + "txCache { estimatedTransactions = 1000, initOptimization = false }\n"
            + "properties = []\n"
            + "merkleRoot = {}\n"
    );
  }

  public static StorageConfig fromConfig(Config config) {
    Config section = config.hasPath("storage")
        ? config.getConfig("storage").withFallback(DEFAULTS)
        : DEFAULTS;

    StorageConfig sc = ConfigBeanFactory.create(section, StorageConfig.class);
    // Keep raw config for legacy LevelDB per-database option overrides (default, defaultM, defaultL)
    sc.rawStorageConfig = section;
    return sc;
  }
}
