package org.tron.common.prometheus;

import io.prometheus.client.Counter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "metrics")
class MetricsCounter {

  private static final Map<String, Counter> container  = new ConcurrentHashMap<>();

  static {
    init(MetricKeys.Counter.TXS, "tron  txs  info .", "type", "detail");
    init(MetricKeys.Counter.MINER, "tron  miner info .", "miner", "type");
    init(MetricKeys.Counter.BLOCK_FORK, "tron  block fork info .", "type");
    init(MetricKeys.Counter.SR_SET_CHANGE, "tron sr set change .", "action", "witness");
    init(MetricKeys.Counter.P2P_ERROR, "tron p2p error  info .", "type");
    init(MetricKeys.Counter.P2P_DISCONNECT, "tron p2p disconnect .", "type");
    init(MetricKeys.Counter.INTERNAL_SERVICE_FAIL, "internal Service fail.",
        "class", "method");
    init(MetricKeys.Counter.BLOCK_FETCH_ARMED,
        "in-flight fetch requests armed by the fetch-block service.");
    init(MetricKeys.Counter.BLOCK_FETCH_SECONDARY,
        "secondary fetch requests issued by the fetch-block failover estimator.");
    init(MetricKeys.Counter.BLOCK_ALREADY_KNOWN,
        "adv block responses matched to an outstanding request whose exact block id "
            + "was already known when the response was handled (best-effort: concurrent "
            + "arrivals may be missed; a duplicate is not attributed to secondary "
            + "fetches).");
  }

  private MetricsCounter() {
    throw new IllegalStateException("MetricsCounter");
  }

  private static void init(String name, String help, String... labels) {
    container.put(name, Counter.build()
        .name(name)
        .help(help)
        .labelNames(labels)
        .register());
  }

  static void inc(String key, double amt, String... labels) {
    if (Metrics.enabled()) {
      Counter counter = container.get(key);
      if (counter == null) {
        logger.info("{} not exist", key);
        return;
      }
      counter.labels(labels).inc(amt);
    }
  }
}

