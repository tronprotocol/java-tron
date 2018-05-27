package stest.tron.wallet.common.client;

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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import static org.apache.commons.lang3.StringUtils.isBlank;


public class Configuration {

    private static Config config;

    private static final Logger logger = LoggerFactory.getLogger("Configuration");

    /**
     * Get configuration by a given path.
     *
     * @param configurationPath path to configuration file
     * @return loaded configuration
     */
    public static Config getByPath(final String configurationPath) {
        if (isBlank(configurationPath)) {
            throw new IllegalArgumentException("Configuration path is required!");
        }

        if (config == null) {
            File configFile = new File(System.getProperty("user.dir")+'/'+configurationPath);
            if(configFile.exists()){
                try {
                    config = ConfigFactory.parseReader(new InputStreamReader(new FileInputStream(configurationPath)));
                    logger.info("use user defined config file in current dir");
                } catch (FileNotFoundException e) {
                    logger.error("load user defined config file exception: " + e.getMessage());
                }
            }else {
                config = ConfigFactory.load(configurationPath);
                logger.info("user defined config file doesn't exists, use default config file in jar");
            }
        }
        return config;
    }
}
