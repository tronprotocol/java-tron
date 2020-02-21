package org.tron.core.metrics;

import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
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

//  @Autowired
//  private BlockChainInfo blockChainInfo;

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

    nodeInfo.setIp(getMyIp());
    nodeInfo.setType(1);
    nodeInfo.setStatus(BlockChainInfo.produceBlockexpectionCount >= 1 ? 0 : 1);
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
    blockChain.setHeadBlockTimestamp(chainBaseManager.getHeadBlockTimeStamp());
    blockChain.setHeadBlockHash(dbManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderHash().toString());

    MetricsInfo.BlockchainInfo.TPSInfo blockProcessTime =
            new MetricsInfo.BlockchainInfo.TPSInfo();
    Meter meterBlockProcessTime =
            monitorMetric.getMeter(MonitorMetric.BLOCKCHAIN_BLOCKPROCESS_TIME);
    blockProcessTime.setMeanRate(meterBlockProcessTime.getMeanRate());
    blockProcessTime.setOneMinuteRate(meterBlockProcessTime.getOneMinuteRate());
    blockProcessTime.setFiveMinuteRate(meterBlockProcessTime.getFiveMinuteRate());
    blockProcessTime.setFifteenMinuteRate(meterBlockProcessTime.getFifteenMinuteRate());
    blockChain.setBlockProcessTime(blockProcessTime);
    blockChain.setForkCount((int) monitorMetric.
            getMeter(MonitorMetric.BLOCKCHAIN_SUCCESS_FORK_COUNT).getCount());
    blockChain.setHeadBlockNum((int) chainBaseManager.getHeadBlockNum());
    blockChain.setTxCacheSize(dbManager.getPendingTransactions().size());
    blockChain.setMissTxCount(dbManager.getPendingTransactions().size() +
            dbManager.getRePushTransactions().size());

    //MonitorInfo.DataInfo.BlochainInfo.TPSInfo tpsInfo =
    //new MonitorInfo.DataInfo.BlochainInfo.TPSInfo();

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

    MetricsInfo.NetInfo.LatencyInfo latencyInfo = getBlockLatencyInfo();
    netInfo.setLatency(latencyInfo);
    data.setNetInfo(netInfo);

  }

  /**
   * get host ip address
   *
   * @param @data none
   *              return string
   */
  public String getMyIp() {
    try {
      URL url = new URL("http://checkip.amazonaws.com");
      BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
      String ipAddress = new String();
      ipAddress = in.readLine().trim();
      if (ipAddress.length() == 0) {
        return InetAddress.getLocalHost().getHostAddress();
      } else {
        return ipAddress;
      }
    } catch (Exception e) {
      // This try will give the Private IP of the Host.
      try {
        InetAddress ip = InetAddress.getLocalHost();
        return ip.getHostAddress().trim();
      } catch (Exception ex) {
        return "GET IP ERROR";
      }
    }
  }

  private MetricsInfo.NetInfo.LatencyInfo getBlockLatencyInfo() {
    MetricsInfo.NetInfo.LatencyInfo latencyInfo =
            new MetricsInfo.NetInfo.LatencyInfo();
    long delay1SCount = monitorMetric.getCounter(MonitorMetric.NET_BLOCK_LATENCY + ".1S")
            .getCount();
    latencyInfo.setDelay1S((int) delay1SCount);
    long delay2SCount = monitorMetric.getCounter(MonitorMetric.NET_BLOCK_LATENCY + ".2S")
            .getCount();
    latencyInfo.setDelay2S((int) delay2SCount);
    long delay3SCount = monitorMetric.getCounter(MonitorMetric.NET_BLOCK_LATENCY + ".3S")
            .getCount();
    latencyInfo.setDelay3S((int) delay3SCount);
    Histogram blockLatency = monitorMetric.getHistogram(MonitorMetric.NET_BLOCK_LATENCY);
    latencyInfo.setTop99((int) blockLatency.getSnapshot().get99thPercentile());
    latencyInfo.setTop95((int) blockLatency.getSnapshot().get95thPercentile());
    latencyInfo.setTotalCount((int) blockLatency.getCount());

    List<MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo> latencyDetailInfos =
            new ArrayList<>();
    SortedMap<String, Histogram> witnessLatencyMap
            = monitorMetric.getHistograms(MonitorMetric.NET_BLOCK_LATENCY_WITNESS);
    for (Map.Entry<String, Histogram> entry : witnessLatencyMap.entrySet()) {
      MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo latencyDetailTemp =
              new MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo();
      String address = entry.getKey().substring(MonitorMetric.NET_BLOCK_LATENCY_WITNESS.length());
      latencyDetailTemp.setCount((int) entry.getValue().getCount());
      latencyDetailTemp.setWitness(address);
      latencyDetailTemp.setTop99((int) entry.getValue().getSnapshot().get99thPercentile());
      latencyDetailTemp.setTop95((int) entry.getValue().getSnapshot().get95thPercentile());
      long witnessDelay1S = monitorMetric.getCounter(
              MonitorMetric.NET_BLOCK_LATENCY_WITNESS + address + ".1S").getCount();
      latencyDetailTemp.setDelay1S((int) witnessDelay1S);
      long witnessDelay2S = monitorMetric.getCounter(
              MonitorMetric.NET_BLOCK_LATENCY_WITNESS + address + ".2S").getCount();
      latencyDetailTemp.setDelay2S((int) witnessDelay2S);
      long witnessDelay3S = monitorMetric.getCounter(
              MonitorMetric.NET_BLOCK_LATENCY_WITNESS + address + ".3S").getCount();
      latencyDetailTemp.setDelay3S((int) witnessDelay3S);
      latencyDetailInfos.add(latencyDetailTemp);
    }
//    MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo latencyDetail =
//            new MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo();
//    latencyDetail.setCount(10);
//    latencyDetail.setWitness("41d376d829440505ea13c9d1c455317d51b62e4ab6");
//    latencyDetail.setTop99(11);
//    latencyDetail.setTop95(8);
//    latencyDetail.setDelay1S(3);
//    latencyDetail.setDelay2S(1);
//    latencyDetail.setDelay3S(0);
//    latencyDetailInfos.add(latencyDetail);
    latencyInfo.setLatencyDetailInfo(latencyDetailInfos);

    return latencyInfo;
  }
}
