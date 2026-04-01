/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.config.args;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.tron.common.cache.CacheStrategies;
import org.tron.common.cache.CacheType;
import org.tron.common.utils.DbOptionalsUtils;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Property;
import org.tron.common.utils.Sha256Hash;

/**
 * Custom storage configurations
 *
 * @author haoyouqiang
 * @version 1.0
 * @since 2018/5/25
 */
@Slf4j(topic = "db")
public class Storage {

  private static final String DEFAULT_INDEX_SWITCH = "on";
  private Config storage;

  /**
   * Database storage directory: /path/to/{dbDirectory}
   */
  @Getter
  @Setter
  private String dbDirectory;

  @Getter
  @Setter
  private String dbEngine;

  @Getter
  @Setter
  private boolean dbSync;

  @Getter
  @Setter
  private int maxFlushCount;

  /**
   * Index storage directory: /path/to/{indexDirectory}
   */
  @Getter
  @Setter
  private String indexDirectory;

  @Getter
  @Setter
  private String indexSwitch;

  @Getter
  @Setter
  private boolean contractParseSwitch;

  @Getter
  @Setter
  private String transactionHistorySwitch;

  @Getter
  @Setter
  private int checkpointVersion;

  @Getter
  @Setter
  private boolean checkpointSync;

  private Options defaultDbOptions;

  @Getter
  @Setter
  private int estimatedBlockTransactions;

  @Getter
  @Setter
  private boolean txCacheInitOptimization = false;

  // second cache
  private final Map<CacheType, String> cacheStrategies = Maps.newConcurrentMap();

  @Getter
  private final List<String> cacheDbs = CacheStrategies.CACHE_DBS;
  // second cache

  /**
   * Key: dbName, Value: Property object of that database
   */
  @Getter
  private Map<String, Property> propertyMap;

  // db root
  private final Map<String, Sha256Hash> dbRoots = Maps.newConcurrentMap();

  /**
   * All getXxxFromConfig methods now read from StorageConfig bean instead of
   * manual string constants. Signatures preserved for backward compatibility.
   */

  public static String getDbEngineFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getDb().getEngine();
  }

  public static Boolean getDbVersionSyncFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getDb().isSync();
  }

  public static int getSnapshotMaxFlushCountFromConfig(final Config config) {
    int maxFlushCountConfig = StorageConfig.fromConfig(config)
        .getSnapshot().getMaxFlushCount();
    if (maxFlushCountConfig <= 0) {
      throw new IllegalArgumentException("MaxFlushCount value can not be negative or zero!");
    }
    if (maxFlushCountConfig > 500) {
      throw new IllegalArgumentException("MaxFlushCount value must not exceed 500!");
    }
    return maxFlushCountConfig;
  }

  public static Boolean getContractParseSwitchFromConfig(final Config config) {
    // contractParse is under event.subscribe, not storage — read from EventConfig
    EventConfig ec = EventConfig.fromConfig(config);
    return ec.isContractParse();
  }

  public static String getDbDirectoryFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getDb().getDirectory();
  }

  public static String getIndexDirectoryFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getIndex().getDirectory();
  }

  public static String getIndexSwitchFromConfig(final Config config) {
    String val = StorageConfig.fromConfig(config).getIndex().getSwitch();
    return StringUtils.isNotEmpty(val) ? val : DEFAULT_INDEX_SWITCH;
  }

  public static String getTransactionHistorySwitchFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getTransHistory().getSwitch();
  }

  public static int getCheckpointVersionFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getCheckpoint().getVersion();
  }

  public static boolean getCheckpointSyncFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getCheckpoint().isSync();
  }

  public static int getEstimatedTransactionsFromConfig(final Config config) {
    int estimatedTransactions = StorageConfig.fromConfig(config)
        .getTxCache().getEstimatedTransactions();
    if (estimatedTransactions > 10000) {
      estimatedTransactions = 10000;
    } else if (estimatedTransactions < 100) {
      estimatedTransactions = 100;
    }
    return estimatedTransactions;
  }

  public static boolean getTxCacheInitOptimizationFromConfig(final Config config) {
    return StorageConfig.fromConfig(config).getTxCache().isInitOptimization();
  }


  /**
   * Accepts raw storage Config sub-tree because cache.strategies has dynamic keys
   * (CacheType enum names) that ConfigBeanFactory cannot bind to fixed bean fields.
   */
  public void setCacheStrategies(Config storageSection) {
    if (storageSection.hasPath("cache.strategies")) {
      storageSection.getConfig("cache.strategies").resolve().entrySet().forEach(c ->
          this.cacheStrategies.put(CacheType.valueOf(c.getKey()),
              c.getValue().unwrapped().toString()));
    }
  }

  public String getCacheStrategy(CacheType dbName) {
    return this.cacheStrategies.getOrDefault(dbName, CacheStrategies.getCacheStrategy(dbName));
  }

  public Sha256Hash getDbRoot(String dbName, Sha256Hash defaultV) {
    return this.dbRoots.getOrDefault(dbName, defaultV);
  }

  /**
   * Accepts raw storage Config sub-tree because merkleRoot has dynamic keys
   * (database names) that ConfigBeanFactory cannot bind to fixed bean fields.
   */
  public void setDbRoots(Config storageSection) {
    if (storageSection.hasPath("merkleRoot")) {
      storageSection.getConfig("merkleRoot").resolve().entrySet().forEach(c ->
          this.dbRoots.put(c.getKey(), Sha256Hash.wrap(
              ByteString.fromHex(c.getValue().unwrapped().toString()))));
    }
  }

  /**
   * Create Property from StorageConfig.PropertyConfig bean.
   */
  private Property createPropertyFromBean(StorageConfig.PropertyConfig pc) {
    Property property = new Property();

    if (pc.getName().isEmpty()) {
      throw new IllegalArgumentException("[storage.properties] database name must be set.");
    }
    property.setName(pc.getName());

    if (!pc.getPath().isEmpty()) {
      String path = pc.getPath();
      File file = new File(path);
      if (!file.exists() && !file.mkdirs()) {
        throw new IllegalArgumentException(
            String.format("[storage.properties] can not create storage path: %s", path));
      }
      if (!file.canWrite()) {
        throw new IllegalArgumentException(
            String.format("[storage.properties] permission denied to write to: %s ", path));
      }
      property.setPath(path);
    }

    Options dbOptions = newDefaultDbOptions(property.getName());
    applyPropertyOptions(pc, dbOptions);
    property.setDbOptions(dbOptions);
    return property;
  }

  /**
   * Apply LevelDB options from PropertyConfig bean values.
   */
  private static void applyPropertyOptions(StorageConfig.PropertyConfig pc, Options dbOptions) {
    dbOptions.createIfMissing(pc.isCreateIfMissing());
    dbOptions.paranoidChecks(pc.isParanoidChecks());
    dbOptions.verifyChecksums(pc.isVerifyChecksums());
    dbOptions.compressionType(
        CompressionType.getCompressionTypeByPersistentId(pc.getCompressionType()));
    dbOptions.blockSize(pc.getBlockSize());
    dbOptions.writeBufferSize(pc.getWriteBufferSize());
    dbOptions.cacheSize(pc.getCacheSize());
    dbOptions.maxOpenFiles(pc.getMaxOpenFiles());
  }

  // Keep old createProperty and setIfNeeded for setDefaultDbOptions which still
  // uses ConfigObject for dynamic default/defaultM/defaultL overrides
  private static void setIfNeeded(ConfigObject conf, Options dbOptions) {
    if (conf.containsKey("createIfMissing")) {
      dbOptions.createIfMissing(
          Boolean.parseBoolean(conf.get("createIfMissing").unwrapped().toString()));
    }
    if (conf.containsKey("paranoidChecks")) {
      dbOptions.paranoidChecks(
          Boolean.parseBoolean(conf.get("paranoidChecks").unwrapped().toString()));
    }
    if (conf.containsKey("verifyChecksums")) {
      dbOptions.verifyChecksums(
          Boolean.parseBoolean(conf.get("verifyChecksums").unwrapped().toString()));
    }
    if (conf.containsKey("compressionType")) {
      dbOptions.compressionType(CompressionType.getCompressionTypeByPersistentId(
          Integer.parseInt(conf.get("compressionType").unwrapped().toString())));
    }
    if (conf.containsKey("blockSize")) {
      dbOptions.blockSize(
          Integer.parseInt(conf.get("blockSize").unwrapped().toString()));
    }
    if (conf.containsKey("writeBufferSize")) {
      dbOptions.writeBufferSize(
          Integer.parseInt(conf.get("writeBufferSize").unwrapped().toString()));
    }
    if (conf.containsKey("cacheSize")) {
      dbOptions.cacheSize(
          Long.parseLong(conf.get("cacheSize").unwrapped().toString()));
    }
    if (conf.containsKey("maxOpenFiles")) {
      dbOptions.maxOpenFiles(
          Integer.parseInt(conf.get("maxOpenFiles").unwrapped().toString()));
    }
  }

  /**
   * Set propertyMap of Storage object from Config via StorageConfig bean.
   */
  /**
   * Set propertyMap from StorageConfig bean list. No Config parameter needed.
   */
  public void setPropertyMapFromBean(List<StorageConfig.PropertyConfig> props) {
    if (props != null && !props.isEmpty()) {
      propertyMap = props.stream()
          .map(this::createPropertyFromBean)
          .collect(Collectors.toMap(Property::getName, p -> p));
    }
  }

  /**
   * Only for unit test on db
   */
  public void deleteAllStoragePaths() {
    if (propertyMap == null) {
      return;
    }

    for (Property property : propertyMap.values()) {
      String path = property.getPath();
      if (path != null) {
        FileUtil.recursiveDelete(path);
      }
    }
  }

  /**
   * Accepts raw storage Config sub-tree because default/defaultM/defaultL are
   * optional nested objects with dynamic LevelDB Option fields that
   * ConfigBeanFactory cannot bind to fixed bean fields.
   */
  public void setDefaultDbOptions(final Config storageSection) {
    this.defaultDbOptions = DbOptionalsUtils.createDefaultDbOptions();
    storage = storageSection;
  }

  public Options newDefaultDbOptions(String name) {
    Options options = DbOptionalsUtils.newDefaultDbOptions(name, this.defaultDbOptions);

    if (storage.hasPath("default")) {
      setIfNeeded(storage.getObject("default"), options);
    }
    if (storage.hasPath("defaultM") && DbOptionalsUtils.DB_M.contains(name)) {
      setIfNeeded(storage.getObject("defaultM"), options);
    }
    if (storage.hasPath("defaultL") && DbOptionalsUtils.DB_L.contains(name)) {
      setIfNeeded(storage.getObject("defaultL"), options);
    }

    return options;
  }
}
