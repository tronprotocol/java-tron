package org.tron.common.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;

@Slf4j(topic = "metrics")
public class Metrics {

  public static final double MILLISECONDS_PER_SECOND = Collector.MILLISECONDS_PER_SECOND;
  private static volatile boolean initialized = false;

  private Metrics() {
    throw new IllegalStateException("Metrics");
  }

  public static synchronized  void init() {
    if(initialized) {
      return;
    }
    if (CommonParameter.getInstance().isMetricsPrometheusEnable()) {
      try {
        DefaultExports.initialize();
        new OperatingSystemExports().register(CollectorRegistry.defaultRegistry);
        new GuavaCacheExports().register(CollectorRegistry.defaultRegistry);
        int port = CommonParameter.getInstance().getMetricsPrometheusPort();
        new HTTPServer.Builder().withPort(port).build();
        logger.info("prometheus exposed on port : {}", port);
        initialized = true;
      } catch (IOException e) {
        CommonParameter.getInstance().setMetricsPrometheusEnable(false);
        logger.error("{}", e.getMessage());
      }
    }
  }

  public static boolean enabled() {
    return CommonParameter.getInstance().isMetricsPrometheusEnable();
  }

  public static void counterInc(String key, double amt, String... labels) {
    MetricsCounter.inc(key, amt, labels);
  }

  public static void gaugeInc(String key, double amt, String... labels) {
    MetricsGauge.inc(key, amt, labels);
  }

  public static void gaugeSet(String key, double amt, String... labels) {
    MetricsGauge.set(key, amt, labels);
  }

  public static Histogram.Timer histogramStartTimer(String key, String... labels) {
    return MetricsHistogram.startTimer(key, labels);
  }

  public static void histogramObserve(Histogram.Timer startTimer) {
    MetricsHistogram.observeDuration(startTimer);
  }

  public static void histogramObserve(String key, double amt, String... labels) {
    MetricsHistogram.observe(key, amt, labels);
  }
}
