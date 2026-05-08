package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import org.tron.core.config.BeanDefaults;
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

  public static MetricsConfig fromConfig(Config config) {
    Config defaults = BeanDefaults.toConfig(new MetricsConfig());
    Config section = config.hasPath("node.metrics")
        ? BeanDefaults.stripNullLeaves(config.getConfig("node.metrics")).withFallback(defaults)
        : defaults;
    return ConfigBeanFactory.create(section, MetricsConfig.class);
  }
}
