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
    public static final String MINE_NEW = "new";
    public static final String MINE_DEL = "del";
    public static final String TXS_SUCCESS = SUCCESS;
    public static final String TXS_FAIL = FAIL;
    public static final String TXS_FAIL_ERROR = "error";
    public static final String TXS_FAIL_BIG = "big";
    public static final String TXS_FAIL_EXPIRED = "expired";
    public static final String TXS_FAIL_TIMEOUT = "timeout";
    public static final String TXS_FAIL_SIG = "sig";
    public static final String TXS_FAIL_TAPOS = "tapos";
    public static final String TXS_FAIL_DUP = "dup";
    public static final String SNAPSHOT_GET_SUCCESS = SUCCESS;
    public static final String SNAPSHOT_GET_MISS = "miss";
    public static final String SNAPSHOT_GET_REACH_ROOT = "reachRoot";
    public static final String SNAPSHOT_GET_NOT_REACH_ROOT = "notReachRoot";
    public static final String DB_GET_MISS = "miss";
    public static final String DB_GET_SUCCESS = SUCCESS;


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

    public static final String DB_GET = "get";
    public static final String DB_PUT = "put";
    public static final String DB_DEL = "del";
    public static final String DB_NEXT = "next";
    public static final String DB_BATCH = "batch";
    public static final String DB_PREFIX = "prefix";
    public static final String DB_GET_LATEST_VALUES = "getLatestValues";
    public static final String DB_GET_VALUES_NEXT = "getValuesNext";
    public static final String DB_GET_KEYS_NEXT = "getKeysNext";
    public static final String DB_GET_VALUES_PREV = "getValuesPrev";
    public static final String DB_GET_TOTAL = "getTotal";
    public static final String DB_UPDATE_BY_BATCH_INNER = "updateByBatchInner";
    public static final String DB_UPDATE_BY_BATCH_INNER_WITH_OPTIONS = "updateByBatchInnerWithOptions";
    public static final String DB_INNER_BATCH_UPDATE = "innerBatchUpdate";
    public static final String DB_UPDATE_BY_BATCH_WITH_OPTIONS = "updateByBatchWithOptions";
    public static final String DB_UPDATE_BY_BATCH = "updateByBatch";

    public static final String SNAPSHOT_GET = "get";
    public static final String SNAPSHOT_PUT = "put";







    public static final String CHECKPOINT_CREATE = "create";
    public static final String CHECKPOINT_DELETE = "delete";
    public static final String CHECKPOINT_FLUSH = "flush";
    public static final String CHECKPOINT_TOTAL = "total";

    private Histogram() {
      throw new IllegalStateException("Histogram");
    }

  }

}
