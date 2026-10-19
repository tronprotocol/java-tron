package org.tron.common.prometheus;

import io.prometheus.client.Info;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "metrics")
class MetricsInfo {

  private static final Map<String, Info> container = new ConcurrentHashMap<>();

  private static final Map<String, String[]> lastLabels = new ConcurrentHashMap<>();

  static {
    init(MetricKeys.Info.NODE_INFO, "tron node info.",
        MetricLabels.Info.VERSION, MetricLabels.Info.CHAIN_ID,
        MetricLabels.Info.ADDRESS, MetricLabels.Info.ROLE);
  }

  private MetricsInfo() {
    throw new IllegalStateException("MetricsInfo");
  }

  private static void init(String name, String help, String... labels) {
    container.put(name, Info.build()
        .name(name)
        .help(help)
        .labelNames(labels)
        .register());
  }

  static void set(String key, String... labels) {
    if (Metrics.enabled()) {
      Info info = container.get(key);
      if (info == null) {
        logger.info("{} not exist", key);
        return;
      }
      String[] oldLabels = lastLabels.put(key, labels);
      if (oldLabels != null && !Arrays.equals(oldLabels, labels)) {
        // label values are part of the series identity: drop the previous one so an
        // address or role change does not leave a stale series behind
        info.remove(oldLabels);
      }
      info.labels(labels);
    }
  }
}
