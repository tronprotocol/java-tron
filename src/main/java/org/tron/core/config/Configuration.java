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

import com.typesafe.config.ConfigFactory;
import org.tron.core.config.args.Account;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.Witness;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Configuration {

  private static com.typesafe.config.Config config;

  /**
   * Get configuration by a given path.
   *
   * @param configurationPath path to configuration file
   * @return loaded configuration
   */
  public static com.typesafe.config.Config getByPath(final String configurationPath) {
    if (isBlank(configurationPath)) {
      throw new IllegalArgumentException("Configuration path is required!");
    }

    if (config == null) {
      config = ConfigFactory.load(configurationPath);
    }

    return config;
  }

  public static List<Account> getAccountsFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList("genesis.block.assets").stream()
            .map(Account::buildFromConfig)
            .collect(Collectors.toCollection(ArrayList::new));
  }


  public static List<Witness> getWitnessesFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList("genesis.block.witnesses").stream()
            .map(Witness::buildFromConfig)
            .collect(Collectors.toCollection(ArrayList::new));
  }
}
