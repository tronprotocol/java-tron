package org.tron.core.metrics;

import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.program.Version;
import org.tron.protos.Protocol;


@Slf4j(topic = "metrics")
@Component
public class MetricsService {

  @Autowired
  private MonitorMetric monitorMetric;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private Manager dbManager;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  /**
   * get metrics info.
   *
   * @return metricsInfo
   */
  public MetricsInfo getMetricsInfo() {

    MetricsInfo metricsInfo = new MetricsInfo();
    int interval = 60;
    metricsInfo.setInterval(interval);
    setNodeInfo(metricsInfo);

    setBlockchainInfo(metricsInfo);

    setNetInfo(metricsInfo);

    return metricsInfo;
  }

  public Protocol.MetricsInfo getProtoMonitorInfo() {
    return getMetricsInfo().ToProtoEntity();
  }

  /**
   * set node info.
   *
   * @param data MetricsInfo
   */
  public void setNodeInfo(MetricsInfo data) {
    MetricsInfo.NodeInfo nodeInfo = new MetricsInfo.NodeInfo();

    try {
      nodeInfo.setIp(InetAddress.getLocalHost().getHostAddress());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    nodeInfo.setType(1);
    nodeInfo.setStatus(1);
    nodeInfo.setVersion(Version.getVersion());

    data.setNodeInfo(nodeInfo);
  }

  /**
   * set blockchain info.
   *
   * @param data MetricsInfo
   */
  public void setBlockchainInfo(MetricsInfo data) {
    MetricsInfo.BlockchainInfo blockChain = new MetricsInfo.BlockchainInfo();
    //BlockChainInfo blockChainInfo = new BlockChainInfo(interval);
    blockChain.setHeadBlockTimestamp(chainBaseManager.getHeadBlockTimeStamp());
    blockChain.setHeadBlockHash(dbManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderHash().toString());

    MetricsInfo.BlockchainInfo.TPSInfo blockProcessTime =
            new MetricsInfo.BlockchainInfo.TPSInfo();
    blockProcessTime.setMeanRate(2);
    blockProcessTime.setOneMinuteRate(3);
    blockProcessTime.setFiveMinuteRate(2);
    blockProcessTime.setFifteenMinuteRate(4);
    blockChain.setBlockProcessTime(blockProcessTime);
    blockChain.setForkCount(blockChain.getForkCount());
    blockChain.setHeadBlockNum((int) chainBaseManager.getHeadBlockNum());
    blockChain.setTxCacheSize(dbManager.getPendingTransactions().size());
    blockChain.setMissTxCount(100);

    //MonitorInfo.DataInfo.BlochainInfo.TPSInfo tpsInfo =
    //new MonitorInfo.DataInfo.BlochainInfo.TPSInfo();
    //tpsInfo.setMeanRate(2);
    //tpsInfo.setOneMinuteRate(3);
    //tpsInfo.setFiveMinuteRate(2);
    //tpsInfo.setFifteenMinuteRate(4);
    Meter transactionRate = monitorMetric.getMeter(MonitorMetric.BLOCKCHAIN_TPS);
    MetricsInfo.BlockchainInfo.TPSInfo tpsInfo =
            new MetricsInfo.BlockchainInfo.TPSInfo();
    tpsInfo.setMeanRate(transactionRate.getMeanRate());
    tpsInfo.setOneMinuteRate(transactionRate.getOneMinuteRate());
    tpsInfo.setFiveMinuteRate(transactionRate.getFiveMinuteRate());
    tpsInfo.setFifteenMinuteRate(transactionRate.getFifteenMinuteRate());
    blockChain.setTPS(tpsInfo);

    List<MetricsInfo.BlockchainInfo.Witness> witnesses = new ArrayList<>();
    MetricsInfo.BlockchainInfo.Witness noUpgradeSR =
            new MetricsInfo.BlockchainInfo.Witness();
    noUpgradeSR.setAddress("41d376d829440505ea13c9d1c455317d51b62e4ab6");
    noUpgradeSR.setVersion(15);
    witnesses.add(noUpgradeSR);
    blockChain.setWitnesses(witnesses);
    data.setBlockInfo(blockChain);

  }

  /**
   * set net info.
   *
   * @param data MetricsInfo
   */
  public void setNetInfo(MetricsInfo data) {
    MetricsInfo.NetInfo netInfo = new MetricsInfo.NetInfo();

    //set connection info
    netInfo.setConnectionCount(tronNetDelegate.getActivePeer().size());
    int validConnectionCount = 0;
    for (PeerConnection peerConnection : tronNetDelegate.getActivePeer()) {
      if (!(peerConnection.isNeedSyncFromUs() || peerConnection.isNeedSyncFromPeer())) {
        validConnectionCount++;
      }
    }
    netInfo.setValidConnectionCount(validConnectionCount);
    netInfo.setErrorProtoCount(10);

    MetricsInfo.NetInfo.RateInfo tcpInTraffic = new MetricsInfo.NetInfo.RateInfo();
    netInfo.setTCPInTraffic(tcpInTraffic);
    MetricsInfo.NetInfo.RateInfo tcpOutTraffic = new MetricsInfo.NetInfo.RateInfo();
    netInfo.setTCPOutTraffic(tcpOutTraffic);
    MetricsInfo.NetInfo.RateInfo udpInTraffic = new MetricsInfo.NetInfo.RateInfo();
    netInfo.setUDPInTraffic(udpInTraffic);
    MetricsInfo.NetInfo.RateInfo udpOutTraffic = new MetricsInfo.NetInfo.RateInfo();
    netInfo.setUDPOutTraffic(udpOutTraffic);

    // set api request info
    MetricsInfo.NetInfo.ApiInfo apiInfo = new MetricsInfo.NetInfo.ApiInfo();
    HttpInterceptor httpCount = new HttpInterceptor();

    apiInfo.setTotalCount(httpCount.getTotalCount());
    apiInfo.setTotalFailCount(httpCount.getFailCount());
    List<MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo> apiDetails = new ArrayList<>();
    for (Map.Entry<String, JSONObject> entry : httpCount.getEndpointMap().entrySet()) {
      MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo apiDetail =
              new MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo();
      apiDetail.setName(entry.getKey());
      apiDetail.setCount((int) entry.getValue().get(HttpInterceptor.TOTAL_REQUST));
      apiDetail.setFailCount((int) entry.getValue().get(HttpInterceptor.FAIL_REQUST));
      apiDetails.add(apiDetail);
    }
    apiInfo.setApiDetailInfo(apiDetails);
    netInfo.setApi(apiInfo);

    long disconnectionCount
            = monitorMetric.getCounter(MonitorMetric.NET_DISCONNECTION_COUNT).getCount();
    netInfo.setDisconnectionCount((int) disconnectionCount);
    List<MetricsInfo.NetInfo.DisconnectionDetailInfo> disconnectionDetails =
            new ArrayList<>();
    SortedMap<String, Counter> disconnectionReason
            = monitorMetric.getCounters(MonitorMetric.NET_DISCONNECTION_REASON);
    for (Map.Entry<String, Counter> entry : disconnectionReason.entrySet()) {
      MetricsInfo.NetInfo.DisconnectionDetailInfo detail =
              new MetricsInfo.NetInfo.DisconnectionDetailInfo();
      detail.setReason(entry.getKey());
      detail.setCount((int) entry.getValue().getCount());
      disconnectionDetails.add(detail);
    }
    MetricsInfo.NetInfo.DisconnectionDetailInfo disconnectionDetail =
            new MetricsInfo.NetInfo.DisconnectionDetailInfo();
    disconnectionDetail.setReason("TOO_MANY_PEERS");
    disconnectionDetail.setCount(12);
    disconnectionDetails.add(disconnectionDetail);
    netInfo.setDisconnectionDetail(disconnectionDetails);

    MetricsInfo.NetInfo.LatencyInfo latencyInfo =
            new MetricsInfo.NetInfo.LatencyInfo();
    latencyInfo.setDelay1S(12);
    latencyInfo.setDelay2S(5);
    latencyInfo.setDelay3S(1);
    Histogram blockLatency = monitorMetric.getHistogram(MonitorMetric.NET_BLOCK_LATENCY);
    latencyInfo.setTop99((int) blockLatency.getSnapshot().get99thPercentile());
    latencyInfo.setTop95((int) blockLatency.getSnapshot().get95thPercentile());
    latencyInfo.setTotalCount((int) blockLatency.getCount());

    MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo latencyDetail =
            new MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo();
    latencyDetail.setCount(10);
    latencyDetail.setWitness("41d376d829440505ea13c9d1c455317d51b62e4ab6");
    latencyDetail.setTop99(11);
    latencyDetail.setTop95(8);
    latencyDetail.setDelay1S(3);
    latencyDetail.setDelay2S(1);
    latencyDetail.setDelay3S(0);
    List<MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo> latencyDetailInfos =
            new ArrayList<>();
    latencyDetailInfos.add(latencyDetail);
    latencyInfo.setLatencyDetailInfo(latencyDetailInfos);
    netInfo.setLatency(latencyInfo);
    data.setNetInfo(netInfo);

  }

}
