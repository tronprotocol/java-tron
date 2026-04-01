package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
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

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  /**
   * Create MetricsConfig from the "node.metrics" section of the application config.
   */
  public static MetricsConfig fromConfig(Config config) {
    Config section = config.getConfig("node.metrics");
    return ConfigBeanFactory.create(section, MetricsConfig.class);
  }
}
