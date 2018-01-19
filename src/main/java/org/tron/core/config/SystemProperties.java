/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.config;

import com.typesafe.config.Config;

public class SystemProperties {

  private final static String DEFAULT_BLOCKS_LOADER = "";
  private static SystemProperties CONFIG;
  private static boolean useOnlySpringConfig = false;
  private String databaseDir = null;
  private Config config;

  public static SystemProperties getDefault() {
    return useOnlySpringConfig ? null : getSpringDefault();
  }

  static SystemProperties getSpringDefault() {
    if (CONFIG == null) {
      CONFIG = new SystemProperties();
    }
    return CONFIG;
  }

  public Config getConfig() {
    return config;
  }

  public String blocksLoader() {
    return config.hasPath("blocks.loader") ?
        config.getString("blocks.loader") : DEFAULT_BLOCKS_LOADER;
  }

  @ValidateMe
  public boolean recordBlocks() {
    return config.getBoolean("record.blocks");
  }

  @ValidateMe
  public String databaseDir() {
    return databaseDir == null ? config.getString("database.dir") : databaseDir;
  }

  @ValidateMe
  public String dumpDir() {
    return config.getString("dump.dir");
  }

  private @interface ValidateMe {
  }
}
