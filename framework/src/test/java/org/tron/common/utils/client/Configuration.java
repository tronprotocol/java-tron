package org.tron.common.utils.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Configuration {

  private static final Logger logger = LoggerFactory.getLogger("Configuration");
  private static Config config;

  /**
   * constructor.
   */

  public static Config getByPath(final String configurationPath) {
    if (StringUtils.isBlank(configurationPath)) {
      throw new IllegalArgumentException("Configuration path is required!");
    }

    if (config == null) {
      File configFile = new File(System.getProperty("user.dir") + '/' + configurationPath);
      if (configFile.exists()) {
        try (FileInputStream fis = new FileInputStream(configurationPath);
            InputStreamReader isr = new InputStreamReader(fis)) {
          config = ConfigFactory.parseReader(isr);
          logger.info("use user defined config file in current dir");
        } catch (Exception e) {
          logger.error("load user defined config file exception: {}", e.getMessage());
        }
      } else {
        config = ConfigFactory.load(configurationPath);
        logger.info("user defined config file doesn't exists, use default config file in jar");
      }
    }
    return config;
  }
}
