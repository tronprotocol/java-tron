package org.tron.core.metrics;

public class MetricsKey {

  public static final String BLOCKCHAIN_TPS = "blockchain.tps";
  public static final String BLOCKCHAIN_BLOCK_PROCESS_TIME = "blockchain.blockProcessTime";
  public static final String BLOCKCHAIN_FORK_COUNT = "blockchain.forkCount";
  public static final String BLOCKCHAIN_FAIL_FORK_COUNT = "blockchain.failForkCount";
  public static final String BLOCKCHAIN_MISSED_TRANSACTION = "blockchain.missedTransaction";
  public static final String BLOCKCHAIN_DUP_WITNESS = "blockchain.dupWitness.";
  public static final String NET_LATENCY = "net.latency";
  public static final String NET_LATENCY_WITNESS = "net.latency.witness.";
  public static final String NET_DISCONNECTION_COUNT = "net.disconnectionCount";
  public static final String NET_DISCONNECTION_DETAIL = "net.disconnectionDetail.";
  public static final String NET_ERROR_PROTO_COUNT = "net.errorProtoCount";
  public static final String NET_TCP_IN_TRAFFIC = "net.tcpInTraffic";
  public static final String NET_TCP_OUT_TRAFFIC = "net.tcpOutTraffic";
  public static final String NET_UDP_IN_TRAFFIC = "net.udpInTraffic";
  public static final String NET_UDP_OUT_TRAFFIC = "net.udpOutTraffic";
  public static final String NET_API_OUT_TRAFFIC = "net.api.outTraffic";
  public static final String NET_API_QPS = "net.api.qps";
  public static final String NET_API_FAIL_QPS = "net.api.failQps";
  public static final String NET_API_DETAIL_QPS = "net.api.detail.qps.";
  public static final String NET_API_DETAIL_FAIL_QPS = "net.api.detail.failQps.";
  public static final String NET_API_DETAIL_OUT_TRAFFIC = "net.api.detail.outTraffic.";

}
