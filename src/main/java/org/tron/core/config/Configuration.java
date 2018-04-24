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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.FileNotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;

@Slf4j
public class Configuration {

  private static com.typesafe.config.Config config;

  /**
   * Get configuration by a given path.
   *
   * @param confFileName path to configuration file
   * @return loaded configuration
   */
  public static com.typesafe.config.Config getByFileName(final String shellConfFileName, final String confFileName) {
    if (isNoneBlank(shellConfFileName)) {
      File shellConfFile = new File(shellConfFileName);
      resolveConfigFile(shellConfFileName, shellConfFile);
      return config;
    }

    if (isBlank(confFileName)) {
      throw new IllegalArgumentException("Configuration path is required!");
    } else {
      File confFile = new File(confFileName);
      resolveConfigFile(confFileName, confFile);
      return config;
    }
  }

  private static void resolveConfigFile(String fileName, File confFile) {
    if (confFile.exists()) {
      config = ConfigFactory.parseFile(confFile);
    } else if (Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName) != null) {
      config = ConfigFactory.load(fileName);
    } else {
      throw new IllegalArgumentException("Configuration path is required! No Such file " + fileName);
    }
  }
}

