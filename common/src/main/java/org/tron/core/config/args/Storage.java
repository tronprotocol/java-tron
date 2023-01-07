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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.tron.common.cache.CacheStrategies;
import org.tron.common.cache.CacheType;
import org.tron.common.utils.DbOptionalsUtils;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Property;

/**
 * Custom storage configurations
 *
 * @author haoyouqiang
 * @version 1.0
 * @since 2018/5/25
 */
public class Storage {

  /**
   * Keys (names) of database config
   */
  private static final String DB_DIRECTORY_CONFIG_KEY = "storage.db.directory";
  private static final String DB_ENGINE_CONFIG_KEY = "storage.db.engine";
  private static final String DB_SYNC_CONFIG_KEY = "storage.db.sync";
  private static final String INDEX_DIRECTORY_CONFIG_KEY = "storage.index.directory";
  private static final String INDEX_SWITCH_CONFIG_KEY = "storage.index.switch";
  private static final String TRANSACTIONHISTORY_SWITCH_CONFIG_KEY = "storage.transHistory.switch";
  private static final String ESTIMATED_TRANSACTIONS_CONFIG_KEY =
      "storage.txCache.estimatedTransactions";
  private static final String SNAPSHOT_MAX_FLUSH_COUNT_CONFIG_KEY = "storage.snapshot.maxFlushCount";
  private static final String PROPERTIES_CONFIG_KEY = "storage.properties";
  private static final String PROPERTIES_CONFIG_DB_KEY = "storage";
  private static final String PROPERTIES_CONFIG_DEFAULT_KEY = "default";
  private static final String PROPERTIES_CONFIG_DEFAULT_M_KEY = "defaultM";
  private static final String PROPERTIES_CONFIG_DEFAULT_L_KEY = "defaultL";
  private static final String DEFAULT_TRANSACTIONHISTORY_SWITCH = "on";

  private static final String NAME_CONFIG_KEY = "name";
  private static final String PATH_CONFIG_KEY = "path";
  private static final String CREATE_IF_MISSING_CONFIG_KEY = "createIfMissing";
  private static final String PARANOID_CHECKS_CONFIG_KEY = "paranoidChecks";
  private static final String VERITY_CHECK_SUMS_CONFIG_KEY = "verifyChecksums";
  private static final String COMPRESSION_TYPE_CONFIG_KEY = "compressionType";
  private static final String BLOCK_SIZE_CONFIG_KEY = "blockSize";
  private static final String WRITE_BUFFER_SIZE_CONFIG_KEY = "writeBufferSize";
  private static final String CACHE_SIZE_CONFIG_KEY = "cacheSize";
  private static final String MAX_OPEN_FILES_CONFIG_KEY = "maxOpenFiles";
  private static final String EVENT_SUBSCRIBE_CONTRACT_PARSE = "event.subscribe.contractParse";

  private static final String CHECKPOINT_VERSION_KEY = "storage.checkpoint.version";
  private static final String CHECKPOINT_SYNC_KEY = "storage.checkpoint.sync";

  private static final String CACHE_STRATEGIES = "storage.cache.strategies";

  /**
   * Default values of directory
   */
  private static final String DEFAULT_DB_ENGINE = "LEVELDB";
  private static final boolean DEFAULT_DB_SYNC = false;
  private static final boolean DEFAULT_EVENT_SUBSCRIBE_CONTRACT_PARSE = true;
  private static final String DEFAULT_DB_DIRECTORY = "database";
  private static final String DEFAULT_INDEX_DIRECTORY = "index";
  private static final String DEFAULT_INDEX_SWITCH = "on";
  private static final int DEFAULT_CHECKPOINT_VERSION = 1;
  private static final boolean DEFAULT_CHECKPOINT_SYNC = true;
  private static final int DEFAULT_ESTIMATED_TRANSACTIONS = 1000;
  private static final int DEFAULT_SNAPSHOT_MAX_FLUSH_COUNT = 1;
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

  public static String getDbEngineFromConfig(final Config config) {
    return config.hasPath(DB_ENGINE_CONFIG_KEY)
        ? config.getString(DB_ENGINE_CONFIG_KEY) : DEFAULT_DB_ENGINE;
  }

  public static Boolean getDbVersionSyncFromConfig(final Config config) {
    return config.hasPath(DB_SYNC_CONFIG_KEY)
        ? config.getBoolean(DB_SYNC_CONFIG_KEY) : DEFAULT_DB_SYNC;
  }

  public static int getSnapshotMaxFlushCountFromConfig(final Config config) {
    if (!config.hasPath(SNAPSHOT_MAX_FLUSH_COUNT_CONFIG_KEY)) {
      return DEFAULT_SNAPSHOT_MAX_FLUSH_COUNT;
    }
    int maxFlushCountConfig = config.getInt(SNAPSHOT_MAX_FLUSH_COUNT_CONFIG_KEY);
    if (maxFlushCountConfig <= 0) {
      throw new IllegalArgumentException("MaxFlushCount value can not be negative or zero!");
    }
    if (maxFlushCountConfig > 500) {
      throw new IllegalArgumentException("MaxFlushCount value must not exceed 500!");
    }
    return maxFlushCountConfig;
  }

  public static Boolean getContractParseSwitchFromConfig(final Config config) {
    return config.hasPath(EVENT_SUBSCRIBE_CONTRACT_PARSE)
        ? config.getBoolean(EVENT_SUBSCRIBE_CONTRACT_PARSE)
        : DEFAULT_EVENT_SUBSCRIBE_CONTRACT_PARSE;
  }

  public static String getDbDirectoryFromConfig(final Config config) {
    return config.hasPath(DB_DIRECTORY_CONFIG_KEY)
        ? config.getString(DB_DIRECTORY_CONFIG_KEY) : DEFAULT_DB_DIRECTORY;
  }

  public static String getIndexDirectoryFromConfig(final Config config) {
    return config.hasPath(INDEX_DIRECTORY_CONFIG_KEY)
        ? config.getString(INDEX_DIRECTORY_CONFIG_KEY) : DEFAULT_INDEX_DIRECTORY;
  }

  public static String getIndexSwitchFromConfig(final Config config) {
    return config.hasPath(INDEX_SWITCH_CONFIG_KEY)
        && StringUtils.isNotEmpty(config.getString(INDEX_SWITCH_CONFIG_KEY))
        ? config.getString(INDEX_SWITCH_CONFIG_KEY) : DEFAULT_INDEX_SWITCH;
  }

  public static String getTransactionHistorySwitchFromConfig(final Config config) {
    return config.hasPath(TRANSACTIONHISTORY_SWITCH_CONFIG_KEY)
        ? config.getString(TRANSACTIONHISTORY_SWITCH_CONFIG_KEY)
        : DEFAULT_TRANSACTIONHISTORY_SWITCH;
  }

  public static int getCheckpointVersionFromConfig(final Config config) {
    return config.hasPath(CHECKPOINT_VERSION_KEY)
        ? config.getInt(CHECKPOINT_VERSION_KEY)
        : DEFAULT_CHECKPOINT_VERSION;
  }

  public static boolean getCheckpointSyncFromConfig(final Config config) {
    return config.hasPath(CHECKPOINT_SYNC_KEY)
        ? config.getBoolean(CHECKPOINT_SYNC_KEY)
        : DEFAULT_CHECKPOINT_SYNC;
  }

  public static int getEstimatedTransactionsFromConfig(final Config config) {
    if (!config.hasPath(ESTIMATED_TRANSACTIONS_CONFIG_KEY)) {
      return DEFAULT_ESTIMATED_TRANSACTIONS;
    }
    int estimatedTransactions = config.getInt(ESTIMATED_TRANSACTIONS_CONFIG_KEY);
    if (estimatedTransactions > 10000) {
      estimatedTransactions = 10000;
    } else if (estimatedTransactions < 100) {
      estimatedTransactions = 100;
    }
    return estimatedTransactions;
  }


  public  void setCacheStrategies(Config config) {
    if (config.hasPath(CACHE_STRATEGIES)) {
      config.getConfig(CACHE_STRATEGIES).resolve().entrySet().forEach(c ->
          this.cacheStrategies.put(CacheType.valueOf(c.getKey()),
              c.getValue().unwrapped().toString()));
    }
  }

  public String getCacheStrategy(CacheType dbName) {
    return this.cacheStrategies.getOrDefault(dbName, CacheStrategies.getCacheStrategy(dbName));
  }

  private  Property createProperty(final ConfigObject conf) {

    Property property = new Property();

    // Database name must be set
    if (!conf.containsKey(NAME_CONFIG_KEY)) {
      throw new IllegalArgumentException("[storage.properties] database name must be set.");
    }
    property.setName(conf.get(NAME_CONFIG_KEY).unwrapped().toString());

    // Check writable permission of path
    if (conf.containsKey(PATH_CONFIG_KEY)) {
      String path = conf.get(PATH_CONFIG_KEY).unwrapped().toString();

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

    // Check, get and set fields of Options
    Options dbOptions = newDefaultDbOptions(property.getName());

    setIfNeeded(conf, dbOptions);

    property.setDbOptions(dbOptions);
    return property;
  }

  private static void setIfNeeded(ConfigObject conf, Options dbOptions) {
    if (conf.containsKey(CREATE_IF_MISSING_CONFIG_KEY)) {
      dbOptions.createIfMissing(
          Boolean.parseBoolean(
              conf.get(CREATE_IF_MISSING_CONFIG_KEY).unwrapped().toString()
          )
      );
    }

    if (conf.containsKey(PARANOID_CHECKS_CONFIG_KEY)) {
      dbOptions.paranoidChecks(
          Boolean.parseBoolean(
              conf.get(PARANOID_CHECKS_CONFIG_KEY).unwrapped().toString()
          )
      );
    }

    if (conf.containsKey(VERITY_CHECK_SUMS_CONFIG_KEY)) {
      dbOptions.verifyChecksums(
          Boolean.parseBoolean(
              conf.get(VERITY_CHECK_SUMS_CONFIG_KEY).unwrapped().toString()
          )
      );
    }

    if (conf.containsKey(COMPRESSION_TYPE_CONFIG_KEY)) {
      String param = conf.get(COMPRESSION_TYPE_CONFIG_KEY).unwrapped().toString();
      try {
        dbOptions.compressionType(
            CompressionType.getCompressionTypeByPersistentId(Integer.parseInt(param)));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException(COMPRESSION_TYPE_CONFIG_KEY, Integer.class, param);
      }
    }

    if (conf.containsKey(BLOCK_SIZE_CONFIG_KEY)) {
      String param = conf.get(BLOCK_SIZE_CONFIG_KEY).unwrapped().toString();
      try {
        dbOptions.blockSize(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException(BLOCK_SIZE_CONFIG_KEY, Integer.class, param);
      }
    }

    if (conf.containsKey(WRITE_BUFFER_SIZE_CONFIG_KEY)) {
      String param = conf.get(WRITE_BUFFER_SIZE_CONFIG_KEY).unwrapped().toString();
      try {
        dbOptions.writeBufferSize(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException(WRITE_BUFFER_SIZE_CONFIG_KEY, Integer.class, param);
      }
    }

    if (conf.containsKey(CACHE_SIZE_CONFIG_KEY)) {
      String param = conf.get(CACHE_SIZE_CONFIG_KEY).unwrapped().toString();
      try {
        dbOptions.cacheSize(Long.parseLong(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException(CACHE_SIZE_CONFIG_KEY, Long.class, param);
      }
    }

    if (conf.containsKey(MAX_OPEN_FILES_CONFIG_KEY)) {
      String param = conf.get(MAX_OPEN_FILES_CONFIG_KEY).unwrapped().toString();
      try {
        dbOptions.maxOpenFiles(Integer.parseInt(param));
      } catch (NumberFormatException e) {
        throwIllegalArgumentException(MAX_OPEN_FILES_CONFIG_KEY, Integer.class, param);
      }
    }
  }

  private static void throwIllegalArgumentException(String param, Class type, String actual) {
    throw new IllegalArgumentException(
        String.format("[storage.properties] %s must be %s type, actual: %s.",
            param, type.getSimpleName(), actual));
  }

  /**
   * Set propertyMap of Storage object from Config
   *
   * @param config Config object from "config.conf" file
   */
  public void setPropertyMapFromConfig(final Config config) {
    if (config.hasPath(PROPERTIES_CONFIG_KEY)) {
      propertyMap = config.getObjectList(PROPERTIES_CONFIG_KEY).stream()
          .map(this::createProperty)
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

  public void setDefaultDbOptions(final Config config) {
    this.defaultDbOptions = DbOptionalsUtils.createDefaultDbOptions();
    storage = config.getConfig(PROPERTIES_CONFIG_DB_KEY);
  }

  public Options newDefaultDbOptions(String name ) {
    // first fetch origin default
    Options options =  DbOptionalsUtils.newDefaultDbOptions(name, this.defaultDbOptions);

    // then fetch from config for default
    if (storage.hasPath(PROPERTIES_CONFIG_DEFAULT_KEY)) {
      setIfNeeded(storage.getObject(PROPERTIES_CONFIG_DEFAULT_KEY), options);
    }

    // check if has middle config
    if (storage.hasPath(PROPERTIES_CONFIG_DEFAULT_M_KEY) && DbOptionalsUtils.DB_M.contains(name)) {
      setIfNeeded(storage.getObject(PROPERTIES_CONFIG_DEFAULT_M_KEY), options);

    }
    // check if has large config
    if (storage.hasPath(PROPERTIES_CONFIG_DEFAULT_L_KEY) && DbOptionalsUtils.DB_L.contains(name)) {
      setIfNeeded(storage.getObject(PROPERTIES_CONFIG_DEFAULT_L_KEY), options);
    }

    return options;
  }
}
