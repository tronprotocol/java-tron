package org.tron.common.prometheus;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "metrics")
public class MetricKeys {

  private MetricKeys() {
    throw new IllegalStateException("MetricsKey");
  }

  // Counter
  public static class Counter {
    public static final String TXS = "tron:txs";
    public static final String MINER = "tron:miner";
    public static final String BLOCK_FORK = "tron:block_fork";
    public static final String P2P_ERROR = "tron:p2p_error";
    public static final String P2P_DISCONNECT = "tron:p2p_disconnect";
    public static final String INTERNAL_SERVICE_FAIL = "tron:internal_service_fail";

    private Counter() {
      throw new IllegalStateException("Counter");
    }

  }

  // Gauge
  public static class Gauge {
    public static final String HEADER_HEIGHT = "tron:header_height";
    public static final String HEADER_TIME = "tron:header_time";
    public static final String PEERS = "tron:peers";
    public static final String DB_SIZE_BYTES = "tron:db_size_bytes";
    public static final String DB_SST_LEVEL = "tron:db_sst_level";
    public static final String MANAGER_QUEUE = "tron:manager_queue_size";
    public static final String TX_CACHE = "tron:tx_cache";

    private Gauge() {
      throw new IllegalStateException("Gauge");
    }

  }

  // Histogram
  public static class Histogram {
    public static final String HTTP_SERVICE_LATENCY = "tron:http_service_latency_seconds";
    public static final String GRPC_SERVICE_LATENCY = "tron:grpc_service_latency_seconds";
    public static final String MINER_LATENCY = "tron:miner_latency_seconds";
    public static final String PING_PONG_LATENCY = "tron:ping_pong_latency_seconds";
    public static final String VERIFY_SIGN_LATENCY = "tron:verify_sign_latency_seconds";
    public static final String LOCK_ACQUIRE_LATENCY = "tron:lock_acquire_latency_seconds";
    public static final String BLOCK_PROCESS_LATENCY = "tron:block_process_latency_seconds";
    public static final String BLOCK_PUSH_LATENCY = "tron:block_push_latency_seconds";
    public static final String BLOCK_GENERATE_LATENCY = "tron:block_generate_latency_seconds";
    public static final String PROCESS_TRANSACTION_LATENCY =
        "tron:process_transaction_latency_seconds";
    public static final String MINER_DELAY = "tron:miner_delay_seconds";
    public static final String UDP_BYTES = "tron:udp_bytes";
    public static final String TCP_BYTES = "tron:tcp_bytes";
    public static final String HTTP_BYTES = "tron:http_bytes";
    public static final String INTERNAL_SERVICE_LATENCY = "tron:internal_service_latency_seconds";

    private Histogram() {
      throw new IllegalStateException("Histogram");
    }

  }

}
