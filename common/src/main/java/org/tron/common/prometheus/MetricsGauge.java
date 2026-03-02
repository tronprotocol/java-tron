package org.tron.common.prometheus;

import io.prometheus.client.Gauge;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "metrics")
class MetricsGauge {


  private static final Map<String, Gauge> container = new ConcurrentHashMap<>();

  static {
    init(MetricKeys.Gauge.MANAGER_QUEUE, "tron  manager.queue.size .", "type");
    init(MetricKeys.Gauge.HEADER_HEIGHT, "header  height .");
    init(MetricKeys.Gauge.HEADER_TIME, "header time .");
    init(MetricKeys.Gauge.PEERS, "tron peers.size .", "type");
    init(MetricKeys.Gauge.DB_SIZE_BYTES, "tron  db  size .", "type", "db", "level");
    init(MetricKeys.Gauge.DB_SST_LEVEL, "tron  db  files .", "type", "db", "level");
    init(MetricKeys.Gauge.TX_CACHE, "tron tx cache info.", "type");
  }

  private MetricsGauge() {
    throw new IllegalStateException("MetricsGauge");
  }

  private static void init(String name, String help, String... labels) {
    container.put(name, Gauge.build()
        .name(name)
        .help(help)
        .labelNames(labels)
        .register());
  }


  static void inc(String key, double delta, String... labels) {
    if (Metrics.enabled()) {
      Gauge gauge = container.get(key);
      if (gauge == null) {
        logger.info("{} not exist", key);
        return;
      }
      gauge.labels(labels).inc(delta);
    }
  }

  static void set(String key, double v, String... labels) {
    if (Metrics.enabled()) {
      Gauge gauge = container.get(key);
      if (gauge == null) {
        logger.info("{} not exist", key);
        return;
      }
      gauge.labels(labels).set(v);
    }
  }

}

