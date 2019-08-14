package org.tron.common.utils;

import java.io.File;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.tron.common.storage.rocksdb.RocksDbSettings;
import org.tron.core.config.args.GenesisBlock;

public class DBConfig {

  @Getter
  @Setter
  private static int dbVersion;

  @Getter
  @Setter
  private static String dbEngine;

  @Getter
  @Setter
  private static String outputDirectory;

  @Getter
  @Setter
  private static Map<String, Property> propertyMap;

  @Getter
  @Setter
  private static GenesisBlock genesisBlock;

  @Getter
  @Setter
  private static boolean dbSync;

  @Getter
  @Setter
  private static RocksDbSettings rocksDbSettings;

  @Getter
  @Setter
  private static int allowMultiSign;

  @Getter
  @Setter
  private static long maintenanceTimeInterval; // (ms)

  @Getter
  @Setter
  private static long allowAdaptiveEnergy; //committee parameter

  @Getter
  @Setter
  private static long allowDelegateResource; //committee parameter

  @Getter
  @Setter
  private static long allowTvmTransferTrc10; //committee parameter

  @Getter
  @Setter
  private static long allowTvmConstantinople; //committee parameter

  @Getter
  @Setter
  private static long allowTvmSolidity059; //committee parameter

  @Getter
  @Setter
  private static long allowSameTokenName; //committee parameter

  @Getter
  @Setter
  private static long allowCreationOfContracts; //committee parameter

  @Getter
  @Setter
  private static long allowShieldedTransaction; //committee parameter

  @Getter
  @Setter
  private static String Blocktimestamp;

  @Getter
  @Setter
  private static long allowAccountStateRoot;

  @Getter
  @Setter
  private static long blockNumForEneryLimit;

  @Getter
  @Setter
  private static long proposalExpireTime; // (ms)

  @Getter
  @Setter
  private static long allowProtoFilterNum;

  @Getter
  @Setter
  private static int checkFrozenTime; // for test only

  public static boolean ALLOW_TVM_CONSTANTINOPLE = false;

  //Odyssey3.2 hard fork -- ForkBlockVersionConsts.ENERGY_LIMIT
  @Setter
  public static boolean ENERGY_LIMIT_HARD_FORK = false;

  @Getter
  @Setter
  private static String dbDirectory;

  @Getter
  @Setter
  private static boolean fullNodeAllowShieldedTransaction;

  @Getter
  @Setter
  private static String zenTokenId;

  private static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.SNAPPY;
  private static final int DEFAULT_BLOCK_SIZE = 4 * 1024;
  private static final int DEFAULT_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;
  private static final long DEFAULT_CACHE_SIZE = 10 * 1024 * 1024L;
  private static final int DEFAULT_MAX_OPEN_FILES = 100;

  public static boolean allowTvmConstantinople() {
    return ALLOW_TVM_CONSTANTINOPLE;
  }

  public static boolean getEnergyLimitHardFork() {
    return ENERGY_LIMIT_HARD_FORK;
  }

  public static String getOutputDirectoryByDbName(String dbName) {
    String path = getPathByDbName(dbName);
    if (!StringUtils.isBlank(path)) {
      return path;
    }
    return getOutputDirectory();
  }

  public static String getPathByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getPath();
    }
    return null;
  }

  private static boolean hasProperty(String dbName) {
    if (propertyMap != null) {
      return propertyMap.containsKey(dbName);
    }
    return false;
  }

  private static Property getProperty(String dbName) {
    return propertyMap.get(dbName);
  }

  public static String getOutputDirectory() {
    if (!outputDirectory.equals("") && !outputDirectory.endsWith(File.separator)) {
      return outputDirectory + File.separator;
    }
    return outputDirectory;
  }

  public static Options getOptionsByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getDbOptions();
    }
    return createDefaultDbOptions();
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
