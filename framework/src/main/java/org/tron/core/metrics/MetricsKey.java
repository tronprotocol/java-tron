package org.tron.core.metrics;

public class MetricsKey {

  public static final String BLOCKCHAIN_TPS = "blockchain.TPS";
  public static final String BLOCKCHAIN_BLOCKPROCESS_TIME = "blockchain.blockProcessTime";
  public static final String BLOCKCHAIN_BLOCK_COUNT = "blockchain.blockCount";
  public static final String BLOCKCHAIN_SUCCESS_FORK_COUNT = "blockchain.successForkCount";
  public static final String BLOCKCHAIN_FAIL_FORK_COUNT = "blockchain.failForkCount";
  public static final String NET_BLOCK_LATENCY = "net.block.latency";
  public static final String NET_BLOCK_LATENCY_WITNESS = "net.block.latency.witness.";
  public static final String NET_DISCONNECTION_COUNT = "net.disconnection.count";
  public static final String NET_DISCONNECTION_REASON = "net.disconnection.reason.";
  public static final String NODE_STATUS = "node.status";
  public static final String NET_ERROR_PROTO_COUNT = "net.errorProtoCount";
  public static final String NET_TCP_IN_TRAFFIC = "net.TCPInTraffic";
  public static final String NET_TCP_OUT_TRAFFIC = "net.TCPOutTraffic";
  public static final String NET_UDP_IN_TRAFFIC = "net.UDPInTraffic";
  public static final String NET_UDP_OUT_TRAFFIC = "net.UDPOutTraffic";
  public static final String TOTAL_REQUEST = "net.api.count";
  public static final String FAIL_REQUEST = "FAIL_REQUEST";
  public static final String OUT_TRAFFIC = "OUT_TRAFFIC";
  public final static String END_POINT_OUT_TRAFFIC = "END_POINT_OUT_TRAFFIC";
  public final static String END_POINT_ALL_REQUESTS = "END_POINT_ALL_REQUESTS";
  public final static String END_POINT_FAIL_REQUESTS= "END_POINT_FAIL_REQUESTS";
}
