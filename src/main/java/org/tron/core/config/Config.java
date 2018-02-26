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

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

  private static final Logger logger = LoggerFactory.getLogger("Config");

  private static com.typesafe.config.Config config;

  /**
   * get config.
   */
  public static com.typesafe.config.Config getConf(String confPath) {

    if (confPath == null || "".equals(confPath)) {
      logger.error("need config file path");

      return null;
    }

    if (config == null) {
      config = ConfigFactory.load(confPath);
    }

    return config;
  }
}
