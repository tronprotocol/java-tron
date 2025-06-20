package org.tron.common.prometheus;

import io.prometheus.client.Histogram;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "metrics")
public class MetricsHistogram {

  private static final Map<String, Histogram> container = new ConcurrentHashMap<>();

  static {
    init(MetricKeys.Histogram.INTERNAL_SERVICE_LATENCY, "Internal Service latency.",
        "class", "method");
    init(MetricKeys.Histogram.HTTP_SERVICE_LATENCY, "Http Service latency.",
        "url");
    init(MetricKeys.Histogram.GRPC_SERVICE_LATENCY, "Grpc Service latency.",
        "endpoint");
    init(MetricKeys.Histogram.JSONRPC_SERVICE_LATENCY, "JsonRpc Service latency.",
        "method");
    init(MetricKeys.Histogram.MINER_LATENCY, "miner latency.",
        "miner");
    init(MetricKeys.Histogram.PING_PONG_LATENCY, "node  ping pong  latency.");
    init(MetricKeys.Histogram.VERIFY_SIGN_LATENCY, "verify sign latency for trx , block.",
        "type");
    init(MetricKeys.Histogram.LOCK_ACQUIRE_LATENCY, "lock acquire latency.",
        "type");
    init(MetricKeys.Histogram.BLOCK_PROCESS_LATENCY,
        "process block latency for TronNetDelegate.",
        "sync");
    init(MetricKeys.Histogram.BLOCK_PUSH_LATENCY, "push block latency for Manager.");
    init(MetricKeys.Histogram.BLOCK_GENERATE_LATENCY, "generate block latency.",
        "address");

    init(MetricKeys.Histogram.PROCESS_TRANSACTION_LATENCY, "process transaction latency.",
        "type", "contract");
    init(MetricKeys.Histogram.MINER_DELAY, "miner delay time, actualTime - planTime.",
        "miner");
    init(MetricKeys.Histogram.UDP_BYTES, "udp_bytes traffic.",
        "type");
    init(MetricKeys.Histogram.TCP_BYTES, "tcp_bytes traffic.",
        "type");
    init(MetricKeys.Histogram.HTTP_BYTES, "http_bytes traffic.",
        "url", "status");
    init(MetricKeys.Histogram.MESSAGE_PROCESS_LATENCY, "process message latency.",
        "type");
    init(MetricKeys.Histogram.BLOCK_FETCH_LATENCY, "fetch block latency.");
    init(MetricKeys.Histogram.BLOCK_RECEIVE_DELAY,
        "receive block delay time, receiveTime - blockTime.");
    double[] dbBuckets = new double[]{0.000001,0.000002,0.000003,0.000004,0.000005,0.000006,
        0.000007,0.000008,0.000009,0.00001,0.00002,0.00003,0.00004,0.00005,0.00006,0.00007,
        0.00008,0.00009,0.0001,0.0005,0.001,0.01};
    init(MetricKeys.Histogram.DB_OPERATE_LATENCY, "db operate latency .", dbBuckets,
        "type", "db", "op");
  }

  private MetricsHistogram() {
    throw new IllegalStateException("MetricsHistogram");
  }

  private static void init(String name, String help, String... labels) {
    container.put(name, Histogram.build()
        .name(name)
        .help(help)
        .labelNames(labels)
        .register());
  }

  private static void init(String name, String help,double[] buckets, String... labels) {
    container.put(name, Histogram.build()
        .name(name)
        .buckets(buckets)
        .help(help)
        .labelNames(labels)
        .register());
  }

  static Histogram.Timer startTimer(String key, String... labels) {
    if (Metrics.enabled()) {
      Histogram histogram = container.get(key);
      if (histogram == null) {
        logger.info("{} not exist", key);
        return null;
      }
      return histogram.labels(labels).startTimer();
    }
    return null;
  }

  static void observeDuration(Histogram.Timer startTimer) {
    if (startTimer != null) {
      startTimer.observeDuration();
    }
  }


  static void observe(String key, double amt, String... labels) {
    if (Metrics.enabled()) {
      Histogram histogram = container.get(key);
      if (histogram == null) {
        logger.info("{} not exist", key);
        return;
      }
      histogram.labels(labels).observe(amt);
    }
  }

}

