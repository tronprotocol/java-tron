package org.tron.common.utils;

import static org.tron.common.parameter.CommonParameter.ENERGY_LIMIT_HARD_FORK;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.iq80.leveldb.Options;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.Constant;


public class StorageUtils {

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
    Options options;
    if (hasProperty(dbName)) {
      options = getProperty(dbName).getDbOptions();
    } else {
      options = CommonParameter.getInstance().getStorage().newDefaultDbOptions(dbName);
    }
    if (Constant.MARKET_PAIR_PRICE_TO_ORDER.equals(dbName)) {
      options.comparator(new MarketOrderPriceComparatorForLevelDB());
    }
    return options;
  }
}
