package org.tron.common.utils;

import static org.tron.common.utils.DBConfig.ENERGY_LIMIT_HARD_FORK;
import static org.tron.common.utils.DbOptionalsUtils.createDefaultDbOptions;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.tron.common.parameter.CommonParameter;


public class StorageUtils {
  public static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.SNAPPY;
  public static final int DEFAULT_BLOCK_SIZE = 4 * 1024;
  public static final int DEFAULT_WRITE_BUFFER_SIZE = 10 * 1024 * 1024;
  public static final long DEFAULT_CACHE_SIZE = 10 * 1024 * 1024L;
  public static final int DEFAULT_MAX_OPEN_FILES = 100;

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
    if (CommonParameter.getInstance().getStorage()
        .getPropertyMap() != null) {
      return CommonParameter.getInstance().getStorage()
          .getPropertyMap().containsKey(dbName);
    }
    return false;
  }

  private static Property getProperty(String dbName) {
    return CommonParameter.getInstance().getStorage()
        .getPropertyMap().get(dbName);
  }

  public static String getOutputDirectory() {
    if (!"".equals(CommonParameter.getInstance().getOutputDirectory())
        && !CommonParameter.getInstance().getOutputDirectory().endsWith(File.separator)) {
      return CommonParameter.getInstance().getOutputDirectory() + File.separator;
    }
    return CommonParameter.getInstance().getOutputDirectory();
  }

  public static Options getOptionsByDbName(String dbName) {
    if (hasProperty(dbName)) {
      return getProperty(dbName).getDbOptions();
    }
    return createDefaultDbOptions();
  }
}
