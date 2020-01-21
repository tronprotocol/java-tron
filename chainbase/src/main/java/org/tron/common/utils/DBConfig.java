package org.tron.common.utils;

import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.setting.RocksDbSettings;
import org.tron.common.args.GenesisBlock;

public class DBConfig {

  //Odyssey3.2 hard fork -- ForkBlockVersionConsts.ENERGY_LIMIT
  @Setter
  public static boolean ENERGY_LIMIT_HARD_FORK = false;
  @Getter
  @Setter
  private static int dbVersion;
  @Getter
  @Setter
  private static String dbEngine;
  @Getter
  @Setter
  private static String outputDirectoryConfig;
  @Getter
  @Setter
  private static Map<String, Property> propertyMap;
}
