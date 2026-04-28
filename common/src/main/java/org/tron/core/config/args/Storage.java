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
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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

  // Optional per-tier LevelDB option overrides, read from StorageConfig bean
  private StorageConfig.DbOptionOverride defaultDbOption;
  private StorageConfig.DbOptionOverride defaultMDbOption;
  private StorageConfig.DbOptionOverride defaultLDbOption;

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
   * Initialize default LevelDB options and store optional per-tier overrides
   * from StorageConfig bean (no raw Config needed).
   */
  public void setDefaultDbOptions(StorageConfig sc) {
    this.defaultDbOptions = DbOptionalsUtils.createDefaultDbOptions();
    this.defaultDbOption = sc.getDefaultDbOption();
    this.defaultMDbOption = sc.getDefaultMDbOption();
    this.defaultLDbOption = sc.getDefaultLDbOption();
  }

  public Options newDefaultDbOptions(String name) {
    Options options = DbOptionalsUtils.newDefaultDbOptions(name, this.defaultDbOptions);

    if (defaultDbOption != null) {
      applyDbOptionOverride(defaultDbOption, options);
    }
    if (defaultMDbOption != null && DbOptionalsUtils.DB_M.contains(name)) {
      applyDbOptionOverride(defaultMDbOption, options);
    }
    if (defaultLDbOption != null && DbOptionalsUtils.DB_L.contains(name)) {
      applyDbOptionOverride(defaultLDbOption, options);
    }

    return options;
  }

  // Apply only user-specified overrides (non-null fields) to LevelDB Options.
  private static void applyDbOptionOverride(
      StorageConfig.DbOptionOverride o, Options dbOptions) {
    if (o.getCreateIfMissing() != null) {
      dbOptions.createIfMissing(o.getCreateIfMissing());
    }
    if (o.getParanoidChecks() != null) {
      dbOptions.paranoidChecks(o.getParanoidChecks());
    }
    if (o.getVerifyChecksums() != null) {
      dbOptions.verifyChecksums(o.getVerifyChecksums());
    }
    if (o.getCompressionType() != null) {
      dbOptions.compressionType(
          CompressionType.getCompressionTypeByPersistentId(o.getCompressionType()));
    }
    if (o.getBlockSize() != null) {
      dbOptions.blockSize(o.getBlockSize());
    }
    if (o.getWriteBufferSize() != null) {
      dbOptions.writeBufferSize(o.getWriteBufferSize());
    }
    if (o.getCacheSize() != null) {
      dbOptions.cacheSize(o.getCacheSize());
    }
    if (o.getMaxOpenFiles() != null) {
      dbOptions.maxOpenFiles(o.getMaxOpenFiles());
    }
  }
}
