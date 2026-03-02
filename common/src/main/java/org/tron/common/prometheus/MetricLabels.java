package org.tron.common.prometheus;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "metrics")
public class MetricLabels {

  public static final String SUCCESS = "success";
  public static final String FAIL = "fail";
  public static final String ALL = "all";
  public static final String UNDEFINED = "undefined";
  public static final String BLOCK = "block";
  public static final String TRX = "trx";

  private MetricLabels() {
    throw new IllegalStateException("MetricsLabels");
  }

  // Counter
  public static class Counter {

    public static final String MINE_SUCCESS = SUCCESS;
    public static final String MINE_MISS = "miss";
    public static final String MINE_DUP = "dup";
    public static final String TXS_SUCCESS = SUCCESS;
    public static final String TXS_FAIL = FAIL;
    public static final String TXS_FAIL_ERROR = "error";
    public static final String TXS_FAIL_BIG = "big";
    public static final String TXS_FAIL_EXPIRED = "expired";
    public static final String TXS_FAIL_TIMEOUT = "timeout";
    public static final String TXS_FAIL_SIG = "sig";
    public static final String TXS_FAIL_TAPOS = "tapos";
    public static final String TXS_FAIL_DUP = "dup";

    private Counter() {
      throw new IllegalStateException("Counter");
    }


  }

  // Gauge
  public static class Gauge {
    public static final String QUEUE_PENDING = "pending";

    public static final String QUEUE_REPUSH = "repush";

    public static final String QUEUE_POPPED = "popped";

    public static final String QUEUE_QUEUED = "queued";

    public static final String PEERS_ACTIVE = "active";

    public static final String PEERS_PASSIVE = "passive";

    public static final String PEERS_ALL = "all";

    public static final String PEERS_VALID = "valid";

    private Gauge() {
      throw new IllegalStateException("Gauge");
    }


  }

  // Histogram
  public static class Histogram {
    public static final String TRAFFIC_IN = "in";
    public static final String TRAFFIC_OUT = "out";

    private Histogram() {
      throw new IllegalStateException("Histogram");
    }

  }

}
