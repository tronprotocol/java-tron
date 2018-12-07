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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.tron.common.utils.FileUtil;

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
  private static final String DB_VERSION_CONFIG_KEY = "storage.db.version";
  private static final String DB_SYNC_CONFIG_KEY = "storage.db.sync";
  private static final String INDEX_DIRECTORY_CONFIG_KEY = "storage.index.directory";
  private static final String INDEX_SWITCH_CONFIG_KEY = "storage.index.switch";
  private static final String TRANSACTIONHISTORY_SWITCH_CONFIG_KEY = "storage.transHistory.switch";
  private static final String PROPERTIES_CONFIG_KEY = "storage.properties";
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

  /**
   * Default values of directory
   */
  private static final int DEFAULT_DB_VERSION = 2;
  private static final boolean DEFAULT_DB_SYNC = false;
  private static final String DEFAULT_DB_DIRECTORY = "database";
  private static final String DEFAULT_INDEX_DIRECTORY = "index";
  private static final String DEFAULT_INDEX_SWTICH = "on";

  /**
   * Default values of db options:
   * <p>
   * DEFAULT_COMPRESSION_TYPE: compressed with snappy
   * DEFAULT_BLOCK_SIZE:         4 KB =         4 * 1024 B
   * DEFAULT_WRITE_BUFFER_SIZE: 10 MB = 10 * 1024 * 1024 B
   * DEFAULT_CACHE_SIZE:        10 MB = 10 * 1024 * 1024 B
   * DEFAULT_MAX_OPEN_FILES:   100
   */
  private static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.SNAPPY;
  private static final int DEFAULT_BLOCK_SIZE = 4 * 1024;
  private static final int DEFAULT_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;
  private static final long DEFAULT_CACHE_SIZE = 10 * 1024 * 1024L;
  private static final int DEFAULT_MAX_OPEN_FILES = 100;

  /**
   * Database storage directory: /path/to/{dbDirectory}
   */
  @Getter
  @Setter
  private String dbDirectory;

  @Getter
  @Setter
  private int dbVersion;

  @Getter
  @Setter
  private boolean dbSync;

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
  private String transactionHistoreSwitch;

  /**
   * Other custom database configurations
   */
  @Getter
  @Setter
  private static class Property {
    private String name;
    private String path;
    private Options dbOptions;
  }

  /**
   * Key: dbName, Value: Property object of that database
   */
  private Map<String, Property> propertyMap;

  public static int getDbVersionFromConfig(final Config config) {
    return config.hasPath(DB_VERSION_CONFIG_KEY) ?
        config.getInt(DB_VERSION_CONFIG_KEY) : DEFAULT_DB_VERSION;
  }

  public static Boolean getDbVersionSyncFromConfig(final Config config) {
    return config.hasPath(DB_SYNC_CONFIG_KEY) ?
      config.getBoolean(DB_SYNC_CONFIG_KEY) : DEFAULT_DB_SYNC;
  }

  public static String getDbDirectoryFromConfig(final Config config) {
    return config.hasPath(DB_DIRECTORY_CONFIG_KEY) ?
        config.getString(DB_DIRECTORY_CONFIG_KEY) : DEFAULT_DB_DIRECTORY;
  }

  public static String getIndexDirectoryFromConfig(final Config config) {
    return config.hasPath(INDEX_DIRECTORY_CONFIG_KEY) ?
        config.getString(INDEX_DIRECTORY_CONFIG_KEY) : DEFAULT_INDEX_DIRECTORY;
  }

  public static String getIndexSwitchFromConfig(final Config config) {
    return config.hasPath(INDEX_SWITCH_CONFIG_KEY)
        && StringUtils.isNotEmpty(config.getString(INDEX_SWITCH_CONFIG_KEY)) ?
        config.getString(INDEX_SWITCH_CONFIG_KEY) : DEFAULT_INDEX_SWTICH;
  }

  public static String getTransactionHistoreSwitchFromConfig(final Config config) {
    return config.hasPath(TRANSACTIONHISTORY_SWITCH_CONFIG_KEY)?
      config.getString(TRANSACTIONHISTORY_SWITCH_CONFIG_KEY) : DEFAULT_TRANSACTIONHISTORY_SWITCH;
  }

  /**
   * Set propertyMap of Storage object from Config
   *
   * @param config Config object from "config.conf" file
   */
  public void setPropertyMapFromConfig(final Config config) {
    if (config.hasPath(PROPERTIES_CONFIG_KEY)) {
      propertyMap = config.getObjectList(PROPERTIES_CONFIG_KEY).stream()
          .map(Storage::createProperty)
          .collect(Collectors.toMap(Property::getName, p -> p));
    }
  }

  /**
   * Get storage path by name of database
   *
   * @param dbName name of database
   * @return path of that database
   */
  public String getPathByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getPath();
    }
    return null;
  }

  /**
   * Get database options by name of database
   *
   * @param dbName name of database
   * @return options of that database
   */
  public Options getOptionsByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getDbOptions();
    }
    return createDefaultDbOptions();
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

  private boolean hasProperty(String dbName) {
    if (propertyMap != null) {
      return propertyMap.containsKey(dbName);
    }
    return false;
  }

  private Property getProperty(String dbName) {
    return propertyMap.get(dbName);
  }

  private static Property createProperty(final ConfigObject conf) {

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
        throw new IllegalArgumentException("[storage.properties] can not create storage path: " + path);
      }

      if (!file.canWrite()) {
        throw new IllegalArgumentException("[storage.properties] permission denied to write to: " + path);
      }

      property.setPath(path);
    }

    // Check, get and set fields of Options
    Options dbOptions = createDefaultDbOptions();

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
      try {
        dbOptions.compressionType(
            CompressionType.getCompressionTypeByPersistentId(
                Integer.parseInt(
                    conf.get(COMPRESSION_TYPE_CONFIG_KEY).unwrapped().toString()
                )
            )
        );
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("[storage.properties] compressionType must be Integer type.");
      }
    }

    if (conf.containsKey(BLOCK_SIZE_CONFIG_KEY)) {
      try {
        dbOptions.blockSize(
            Integer.parseInt(
                conf.get(BLOCK_SIZE_CONFIG_KEY).unwrapped().toString()
            )
        );
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("[storage.properties] blockSize must be Integer type.");
      }
    }

    if (conf.containsKey(WRITE_BUFFER_SIZE_CONFIG_KEY)) {
      try {
        dbOptions.writeBufferSize(
            Integer.parseInt(
                conf.get(WRITE_BUFFER_SIZE_CONFIG_KEY).unwrapped().toString()
            )
        );
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("[storage.properties] writeBufferSize must be Integer type.");
      }
    }

    if (conf.containsKey(CACHE_SIZE_CONFIG_KEY)) {
      try {
        dbOptions.cacheSize(
            Long.parseLong(
                conf.get(CACHE_SIZE_CONFIG_KEY).unwrapped().toString()
            )
        );
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("[storage.properties] cacheSize must be Long type.");
      }
    }

    if (conf.containsKey(MAX_OPEN_FILES_CONFIG_KEY)) {
      try {
        dbOptions.maxOpenFiles(
            Integer.parseInt(
                conf.get(MAX_OPEN_FILES_CONFIG_KEY).unwrapped().toString()
            )
        );
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("[storage.properties] maxOpenFiles must be Integer type.");
      }
    }

    property.setDbOptions(dbOptions);
    return property;
  }

  private static Options createDefaultDbOptions() {
    Options dbOptions = new Options();

    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);

    dbOptions.compressionType(DEFAULT_COMPRESSION_TYPE);
    dbOptions.blockSize(DEFAULT_BLOCK_SIZE);
    dbOptions.writeBufferSize(DEFAULT_WRITE_BUFFER_SIZE);
    dbOptions.cacheSize(DEFAULT_CACHE_SIZE);
    dbOptions.maxOpenFiles(DEFAULT_MAX_OPEN_FILES);

    return dbOptions;
  }

}
