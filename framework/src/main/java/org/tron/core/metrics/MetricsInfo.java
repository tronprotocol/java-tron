package org.tron.core.metrics;

import com.alibaba.fastjson.annotation.JSONField;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.metrics.node.NodeInfo;
import org.tron.core.metrics.blockchain.BlockChainInfo;
import org.tron.core.metrics.net.NetInfo;

@Slf4j
public class MetricsInfo {
  private int interval;
  private NodeInfo node;
  private BlockChainInfo blockchain;
  private NetInfo net;

  public int getInterval() {
    return interval;
  }

  public void setInterval(int interval) {
    this.interval = interval;
  }

  public NodeInfo getNode() {
    return node;
  }

  public void setNode(NodeInfo node) {
    this.node = node;
  }

  public BlockChainInfo getBlockchain() {
    return blockchain;
  }

  public void setBlockchain(BlockChainInfo blockchain) {
    this.blockchain = blockchain;
  }

  public NetInfo getNet() {
    return net;
  }

  public void setNet(NetInfo net) {
    this.net = net;
  }

  //  public Protocol.MetricsInfo toProtoEntity() {
//    Protocol.MetricsInfo.Builder builder = Protocol.MetricsInfo.newBuilder();
//    builder.setStartTime(interval);
//
//    Protocol.MetricsInfo.NodeInfo.Builder nodeInfo =
//        Protocol.MetricsInfo.NodeInfo.newBuilder();
//    MetricsInfo.NodeInfo node = this.node;
//    nodeInfo.setIp(node.getIp());
//    nodeInfo.setNodeType(node.getNodeType());
//    nodeInfo.setStatus(node.getNodeType());
//    nodeInfo.setVersion(node.getVersion());
//
//    // set node info
//    builder.setNode(nodeInfo.build());
//
//    Protocol.MetricsInfo.BlockChainInfo.Builder blockChain =
//        Protocol.MetricsInfo.BlockChainInfo.newBuilder();
//    BlockchainInfo blockChainInfo = getBlockchainInfo();
//    blockChain.setHeadBlockTimestamp(blockChainInfo.getHeadBlockTimestamp());
//    blockChain.setHeadBlockHash(blockChainInfo.getHeadBlockHash());
//
//    Protocol.MetricsInfo.BlockChainInfo.TpsInfo.Builder blockProcessTime =
//        Protocol.MetricsInfo.BlockChainInfo.TpsInfo.newBuilder();
//    blockProcessTime.setMeanRate(blockChainInfo.getBlockProcessTime().getMeanRate());
//    blockProcessTime.setOneMinuteRate(blockChainInfo.getBlockProcessTime().getOneMinuteRate());
//    blockProcessTime.setFiveMinuteRate(blockChainInfo.getBlockProcessTime().getFiveMinuteRate());
//    blockProcessTime
//        .setFifteenMinuteRate(blockChainInfo.getBlockProcessTime().getFifteenMinuteRate());
//    blockChain.setBlockProcessTime(blockProcessTime.build());
//    blockChain.setSuccessForkCount(blockChainInfo.getSuccessForkCount());
//    blockChain.setFailForkCount(blockChain.getFailForkCount());
//    blockChain.setHeadBlockNum(blockChainInfo.getHeadBlockNum());
//    blockChain.setTransactionCacheSize(blockChainInfo.getTransactionCacheSize());
//    blockChain.setMissedTransactionCount(blockChainInfo.getMissedTransactionCount());
//
//    Protocol.MetricsInfo.BlockChainInfo.TpsInfo.Builder tpsInfo =
//        Protocol.MetricsInfo.BlockChainInfo.TpsInfo.newBuilder();
//    BlockchainInfo.TpsInfo tpsInfoTemp = blockChainInfo.getTps();
//    tpsInfo.setMeanRate(tpsInfoTemp.getMeanRate());
//    tpsInfo.setOneMinuteRate(tpsInfoTemp.getOneMinuteRate());
//    tpsInfo.setFiveMinuteRate(tpsInfoTemp.getFiveMinuteRate());
//    tpsInfo.setFifteenMinuteRate(tpsInfoTemp.getFifteenMinuteRate());
//    blockChain.setTps(tpsInfo.build());
//    for (BlockchainInfo.Witness witness : blockChainInfo.getWitnesses()) {
//      Protocol.MetricsInfo.BlockChainInfo.Witness.Builder witnessInfo =
//          Protocol.MetricsInfo.BlockChainInfo.Witness.newBuilder();
//      witnessInfo.setAddress(witness.getAddress());
//      witnessInfo.setVersion(witness.getVersion());
//      blockChain.addWitnesses(witnessInfo.build());
//    }
//    // set blockchain info
//    builder.setBlockchain(blockChain.build());
//
//
//    Protocol.MetricsInfo.NetInfo.Builder netInfo =
//        Protocol.MetricsInfo.NetInfo.newBuilder();
//    NetInfo netInfoTemp = getNetInfo();
//    netInfo.setConnectionCount(netInfoTemp.getConnectionCount());
//    netInfo.setValidConnectionCount(netInfoTemp.getValidConnectionCount());
//    netInfo.setErrorProtoCount(netInfoTemp.getErrorProtoCount());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder tcpInTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    tcpInTraffic.setMeanRate(netInfoTemp.getTcpInTraffic().getMeanRate());
//    tcpInTraffic.setOneMinuteRate(netInfoTemp.getTcpInTraffic().getOneMinuteRate());
//    tcpInTraffic.setFiveMinuteRate(netInfoTemp.getTcpInTraffic().getFiveMinuteRate());
//    tcpInTraffic.setFifteenMinuteRate(netInfoTemp.getTcpInTraffic().getFifteenMinuteRate());
//    netInfo.setTcpInTraffic(tcpInTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder tcpOutTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    tcpOutTraffic.setMeanRate(netInfoTemp.getTcpOutTraffic().getMeanRate());
//    tcpOutTraffic.setOneMinuteRate(netInfoTemp.getTcpOutTraffic().getOneMinuteRate());
//    tcpOutTraffic.setFiveMinuteRate(netInfoTemp.getTcpOutTraffic().getFiveMinuteRate());
//    tcpOutTraffic.setFifteenMinuteRate(netInfoTemp.getTcpOutTraffic().getFifteenMinuteRate());
//    netInfo.setTcpOutTraffic(tcpOutTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder udpInTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    udpInTraffic.setMeanRate(netInfoTemp.getUdpInTraffic().getMeanRate());
//    udpInTraffic.setOneMinuteRate(netInfoTemp.getUdpInTraffic().getOneMinuteRate());
//    udpInTraffic.setFiveMinuteRate(netInfoTemp.getUdpInTraffic().getFiveMinuteRate());
//    udpInTraffic.setFifteenMinuteRate(netInfoTemp.getUdpInTraffic().getFifteenMinuteRate());
//    netInfo.setUdpInTraffic(udpInTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.RateInfo.Builder udpOutTraffic =
//        Protocol.MetricsInfo.NetInfo.RateInfo.newBuilder();
//    udpOutTraffic.setMeanRate(netInfoTemp.getUdpOutTraffic().getMeanRate());
//    udpOutTraffic.setOneMinuteRate(netInfoTemp.getUdpOutTraffic().getOneMinuteRate());
//    udpOutTraffic.setFiveMinuteRate(netInfoTemp.getUdpOutTraffic().getFiveMinuteRate());
//    udpOutTraffic.setFifteenMinuteRate(netInfoTemp.getUdpOutTraffic().getFifteenMinuteRate());
//    netInfo.setUdpOutTraffic(udpOutTraffic.build());
//
//    Protocol.MetricsInfo.NetInfo.ApiInfo.common.Builder common =
//        Protocol.MetricsInfo.NetInfo.ApiInfo.common.newBuilder();
//    common.setMeanRate(netInfoTemp.getApi().getTotalCount().getMeanRate());
//    common.setOneMinute(netInfoTemp.getApi().getTotalCount().getOneMinute());
//    common.setFiveMinute(netInfoTemp.getApi().getTotalCount().getFiveMinute());
//    common.setFifteenMinute(netInfoTemp.getApi().getTotalCount().getFifteenMinute());
//    Protocol.MetricsInfo.NetInfo.ApiInfo.Builder apiInfo =
//            Protocol.MetricsInfo.NetInfo.ApiInfo.newBuilder();
//    apiInfo.setTotalCount(common.build());
//
//    common.setMeanRate(netInfoTemp.getApi().getTotalFailCount().getMeanRate());
//    common.setOneMinute(netInfoTemp.getApi().getTotalFailCount().getOneMinute());
//    common.setFiveMinute(netInfoTemp.getApi().getTotalFailCount().getFiveMinute());
//    common.setFifteenMinute(netInfoTemp.getApi().getTotalFailCount().getFifteenMinute());
//
//    apiInfo.setTotalFailCount(common.build());
//
//    common.setMeanRate(netInfoTemp.getApi().getTotalOutTraffic().getMeanRate());
//    common.setOneMinute(netInfoTemp.getApi().getTotalOutTraffic().getOneMinute());
//    common.setFiveMinute(netInfoTemp.getApi().getTotalOutTraffic().getFiveMinute());
//    common.setFifteenMinute(netInfoTemp.getApi().getTotalOutTraffic().getFifteenMinute());
//
//    apiInfo.setTotalOutTraffic(common.build());
//
//
//    for (NetInfo.ApiInfo.ApiDetailInfo apiDetailInfo : netInfoTemp.getApi().getApiDetailInfo()) {
//      Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.Builder apiDetail =
//          Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.newBuilder();
//
//      apiDetail.setName(apiDetailInfo.getName());
//      common.setMeanRate(apiDetailInfo.getCount().getMeanRate());
//      common.setOneMinute(apiDetailInfo.getCount().getOneMinute());
//      common.setFiveMinute(apiDetailInfo.getCount().getFiveMinute());
//      common.setFifteenMinute(apiDetailInfo.getCount().getFifteenMinute());
//
//      apiDetail.setCount(common.build());
//      common.setMeanRate(apiDetailInfo.getFailCount().getMeanRate());
//      common.setOneMinute(apiDetailInfo.getFailCount().getOneMinute());
//      common.setFiveMinute(apiDetailInfo.getFailCount().getFiveMinute());
//      common.setFifteenMinute(apiDetailInfo.getFailCount().getFifteenMinute());
//
//      apiDetail.setFailCount(common.build());
//
//      common.setMeanRate(apiDetailInfo.getOutTraffic().getMeanRate());
//      common.setOneMinute(apiDetailInfo.getOutTraffic().getOneMinute());
//      common.setFiveMinute(apiDetailInfo.getOutTraffic().getFiveMinute());
//      common.setFifteenMinute(apiDetailInfo.getOutTraffic().getFifteenMinute());
//      apiDetail.setOutTraffic(common.build());
//
//      apiInfo.addDetail(apiDetail.build());
//    }
//    netInfo.setApi(apiInfo.build());
//
//    netInfo.setDisconnectionCount(netInfoTemp.getDisconnectionCount());
//    for (NetInfo.DisconnectionDetailInfo disconnectionDetailInfo : netInfoTemp
//        .getDisconnectionDetail()) {
//      Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.Builder disconnectionDetail =
//          Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.newBuilder();
//      disconnectionDetail.setReason(disconnectionDetailInfo.getReason());
//      disconnectionDetail.setCount(disconnectionDetailInfo.getCount());
//      netInfo.addDisconnectionDetail(disconnectionDetail.build());
//    }
//
//    Protocol.MetricsInfo.NetInfo.LatencyInfo.Builder latencyInfo =
//        Protocol.MetricsInfo.NetInfo.LatencyInfo.newBuilder();
//    latencyInfo.setDelay1S(netInfoTemp.getLatency().getDelay1S());
//    latencyInfo.setDelay2S(netInfoTemp.getLatency().getDelay2S());
//    latencyInfo.setDelay3S(netInfoTemp.getLatency().getDelay3S());
//    latencyInfo.setTop99(netInfoTemp.getLatency().getTop99());
//    latencyInfo.setTop95(netInfoTemp.getLatency().getTop95());
//    latencyInfo.setTotalCount(netInfoTemp.getLatency().getTotalCount());
//
//    for (NetInfo.LatencyInfo.LatencyDetailInfo latencyDetailInfo : netInfoTemp.getLatency()
//        .getLatencyDetailInfo()) {
//      Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.Builder latencyDetail =
//          Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.newBuilder();
//      latencyDetail.setCount(latencyDetailInfo.getCount());
//      latencyDetail.setWitness(latencyDetailInfo.getWitness());
//      latencyDetail.setTop99(latencyDetailInfo.getTop99());
//      latencyDetail.setTop95(latencyDetailInfo.getTop95());
//      latencyDetail.setDelay1S(latencyDetailInfo.getDelay1S());
//      latencyDetail.setDelay2S(latencyDetailInfo.getDelay2S());
//      latencyDetail.setDelay3S(latencyDetailInfo.getDelay3S());
//      latencyInfo.addDetail(latencyDetail.build());
//    }
//
//    // set latency info
//    netInfo.setLatency(latencyInfo.build());
//    // set net info
//    builder.setNet(netInfo.build());
//    return builder.build();
//  }

}
