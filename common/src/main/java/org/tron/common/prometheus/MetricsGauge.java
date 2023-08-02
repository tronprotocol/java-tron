package org.tron.common.prometheus;

import static org.tron.common.prometheus.MetricKeys.Gauge.RESOURCE_WINDOW_SIZE;
import static org.tron.common.prometheus.MetricLabels.ACCOUNT_ADDRESS;
import static org.tron.common.prometheus.MetricLabels.LABEL_RESOURCE_TYPE;
import static org.tron.common.prometheus.MetricLabels.LABEL_STAKE_VERSION;

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
    init(MetricKeys.Gauge.MINER, "tron miner.", "miner", "type");
    init(MetricKeys.Gauge.PROPOSAL, "tron proposal.",  "param", "type");
    init(MetricKeys.Gauge.CONTRACT_FACTOR, "tron contract energy factor.", "contract");
    init(MetricKeys.Gauge.CONTRACT_USAGE, "tron contract energy usage.", "contract", "type");

    init(MetricKeys.Gauge.TOTAL_RESOURCE_WEIGHT, "tron stake total resource weight.",
        LABEL_STAKE_VERSION, LABEL_RESOURCE_TYPE);
    init(RESOURCE_WINDOW_SIZE, "tron resource window size.",
        ACCOUNT_ADDRESS, LABEL_RESOURCE_TYPE);

    init(MetricKeys.Gauge.VERIFY_SIGN_SIZE, "tron verify sign trx size.", "type");
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

