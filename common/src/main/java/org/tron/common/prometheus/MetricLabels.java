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
    public static final String SR_ADD = "add";
    public static final String SR_REMOVE = "remove";

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

    // node_role label: one gauge series per witness address, whose value encodes the
    // current backup-election role as a number (0 = init, 1 = master, 2 = slaver);
    // ROLE_INIT/ROLE_MASTER/ROLE_SLAVER are the canonical role names produced by
    // BackupManager and mapped to numbers in SRMetrics.recordNodeRole()
    public static final String ADDRESS = "address";

    public static final String ROLE_INIT = "init";

    public static final String ROLE_MASTER = "master";

    public static final String ROLE_SLAVER = "slaver";

    private Gauge() {
      throw new IllegalStateException("Gauge");
    }


  }

  // Histogram
  public static class Histogram {
    public static final String MINER = "miner";
    public static final String TRAFFIC_IN = "in";
    public static final String TRAFFIC_OUT = "out";

    private Histogram() {
      throw new IllegalStateException("Histogram");
    }

  }

  // Info
  public static class Info {
    public static final String VERSION = "version";
    // identifies the genesis block: the label value is the chain id derived from the
    // genesis block hash, so the label is named genesis_block_id
    public static final String CHAIN_ID = "genesis_block_id";

    private Info() {
      throw new IllegalStateException("Info");
    }

  }

}
