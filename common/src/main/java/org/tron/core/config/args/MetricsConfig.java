package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Metrics configuration bean. Field names match config.conf keys under "node.metrics".
 * Contains nested sub-beans for prometheus and influxdb sections.
 */
@Slf4j
@Getter
@Setter
public class MetricsConfig {

  private boolean storageEnable = false;
  private PrometheusConfig prometheus = new PrometheusConfig();
  private InfluxDbConfig influxdb = new InfluxDbConfig();

  @Getter
  @Setter
  public static class PrometheusConfig {
    private boolean enable = false;
    private int port = 9527;
  }

  @Getter
  @Setter
  public static class InfluxDbConfig {
    private String ip = "";
    private int port = 8086;
    private String database = "metrics";
    private int metricsReportInterval = 10;
  }

  private static final Config DEFAULTS = ConfigFactory.parseString(
      "storageEnable = false\n"
          + "prometheus { enable = false, port = 9527 }\n"
          + "influxdb { ip = \"\", port = 8086, database = metrics,"
          + " metricsReportInterval = 10 }\n"
  );

  /**
   * Create MetricsConfig from the "node.metrics" section of the application config.
   */
  public static MetricsConfig fromConfig(Config config) {
    Config section = config.hasPath("node.metrics")
        ? config.getConfig("node.metrics").withFallback(DEFAULTS)
        : DEFAULTS;
    return ConfigBeanFactory.create(section, MetricsConfig.class);
  }
}
