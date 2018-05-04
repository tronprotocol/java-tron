package stest.tron.wallet.common;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Configuration {

    private static Config config;

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
            config = ConfigFactory.load(configurationPath);
        }

        return config;
    }
}