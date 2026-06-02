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
  private TransHistoryConfig transHistory = new TransHistoryConfig();
  private boolean needToUpdateAsset = true;
  private DbSettingsConfig dbSettings = new DbSettingsConfig();
  private BalanceConfig balance = new BalanceConfig();
  private CheckpointConfig checkpoint = new CheckpointConfig();
  private SnapshotConfig snapshot = new SnapshotConfig();
  private TxCacheConfig txCache = new TxCacheConfig();
  // ConfigBeanFactory requires all bean fields present per item, so we parse manually.
  @Setter(lombok.AccessLevel.NONE)
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
  public static class TransHistoryConfig {

    // "switch" is a reserved Java keyword; ConfigBeanFactory calls setSwitch() which works fine
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

  // A named database entry: name/path plus the optional LevelDB option overrides
  // inherited from DbOptionOverride (boxed types, null = "inherit per-tier defaults").
  @Getter
  @Setter
  public static class PropertyConfig extends DbOptionOverride {

    private String name = "";
    private String path = "";
  }

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  public static StorageConfig fromConfig(Config config) {
    Config section = config.getConfig("storage");

    StorageConfig sc = ConfigBeanFactory.create(section, StorageConfig.class);
    sc.rawStorageConfig = section;
    sc.properties = readProperties(section);

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

    private Integer blockSize;
    private Integer writeBufferSize;
    private Long cacheSize;
    private Integer maxOpenFiles;
  }

  // Shared LevelDB option parser used by both readDbOption and readProperties.
  // Fills the given target (boxed fields, null means "not specified by user") so the
  // same parser can populate a plain DbOptionOverride or a PropertyConfig (which extends it).
  private static void readLevelDbOptions(ConfigObject conf, DbOptionOverride o) {
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
  }

  // Read optional LevelDB option override for default/defaultM/defaultL keys.
  private static DbOptionOverride readDbOption(Config section, String key) {
    if (!section.hasPath(key)) {
      return null;
    }
    DbOptionOverride o = new DbOptionOverride();
    readLevelDbOptions(section.getObject(key), o);
    return o;
  }

  // Parse storage.properties list manually: ConfigBeanFactory requires every bean field to be
  // present in each list item, but name+path-only entries (all LevelDB opts commented out) are
  // valid — missing fields fall back to PropertyConfig Java defaults.
  private static List<PropertyConfig> readProperties(Config section) {
    if (!section.hasPath("properties")) {
      return new ArrayList<>();
    }
    List<? extends ConfigObject> items = section.getObjectList("properties");
    List<PropertyConfig> result = new ArrayList<>(items.size());
    for (ConfigObject obj : items) {
      PropertyConfig p = new PropertyConfig();
      if (obj.containsKey("name")) {
        p.setName(obj.get("name").unwrapped().toString());
      }
      if (obj.containsKey("path")) {
        p.setPath(obj.get("path").unwrapped().toString());
      }
      // Boxed nullable fields: unset options stay null so they inherit the per-tier
      // defaults applied by newDefaultDbOptions instead of resetting them.
      readLevelDbOptions(obj, p);
      result.add(p);
    }
    return result;
  }

  private static void throwIllegalArgumentException(String param, Class<?> type, String actual) {
    throw new IllegalArgumentException(
        String.format("[storage.properties] %s must be %s type, actual: %s.",
            param, type.getSimpleName(), actual));
  }
}
