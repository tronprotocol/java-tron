package org.tron.core.metrics;

import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.metrics.blockchain.BlockChainInfo;
import org.tron.core.metrics.blockchain.DupWitnessInfo;
import org.tron.core.metrics.blockchain.BlockChainMetricManager;
import org.tron.core.metrics.blockchain.StartTimeRecorder;
import org.tron.core.metrics.blockchain.WitnessInfo;
import org.tron.core.metrics.node.NodeInfo;
import org.tron.core.metrics.net.RateInfo;
import org.tron.core.metrics.net.NetInfo;
import org.tron.core.metrics.net.ApiInfo;
import org.tron.core.metrics.net.ApiDetailInfo;
import org.tron.core.metrics.net.LatencyInfo;
import org.tron.core.metrics.net.DisconnectionDetailInfo;
import org.tron.core.metrics.net.LatencyDetailInfo;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.program.Version;


@Slf4j(topic = "metrics")
@Component
public class MetricsApiService {

  @Autowired
  private MetricsService metricsService;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private Manager dbManager;

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private BlockChainMetricManager blockChainMetricManager;

  /**
   * get metrics info.
   *
   * @return metricsInfo
   */
  public MetricsInfo getMetricsInfo() {

    MetricsInfo metricsInfo = new MetricsInfo();

    metricsInfo.setInterval((int) StartTimeRecorder.getInstance().getStartRecordTime());
    setNodeInfo(metricsInfo);

    setBlockchainInfo(metricsInfo);

    setNetInfo(metricsInfo);

    return metricsInfo;
  }

  //public Protocol.MetricsInfo getProtoMonitorInfo() {
    //return getMetricsInfo().toProtoEntity();
  //}

  private void setNodeInfo(MetricsInfo data) {
    NodeInfo nodeInfo = new NodeInfo();
    nodeInfo.setIp(getMyIp());

    ByteString witnessAddress = ByteString.copyFrom(Args.getLocalWitnesses()
            .getWitnessAccountAddress(CommonParameter.getInstance().isECKeyCryptoEngine()));
    if (chainBaseManager.getWitnessScheduleStore().getActiveWitnesses().contains(witnessAddress)) {
      nodeInfo.setNodeType(1);
    } else {
      nodeInfo.setNodeType(0);
    }

    nodeInfo.setStatus(getNodeStatusByTime(0));
    nodeInfo.setVersion(Version.getVersion());
    if (backupManager.getStatus() == BackupManager.BackupStatusEnum.MASTER) {
      nodeInfo.setBackupStatus(1);
    } else {
      nodeInfo.setBackupStatus(0);
    }

    data.setNodeInfo(nodeInfo);
  }

  private void setBlockchainInfo(MetricsInfo data) {
    BlockChainInfo blockChain = new BlockChainInfo();
    blockChain.setHeadBlockTimestamp(chainBaseManager.getHeadBlockTimeStamp());
    blockChain.setHeadBlockHash(dbManager.getDynamicPropertiesStore()
        .getLatestBlockHeaderHash().toString());

    RateInfo blockProcessTime = blockChainMetricManager.getBlockProcessTime();
    blockChain.setBlockProcessTime(blockProcessTime);
    blockChain.setSuccessForkCount(getSuccessForkCount());
    blockChain.setFailForkCount(getFailForkCount());
    blockChain.setHeadBlockNum((int) chainBaseManager.getHeadBlockNum());
    blockChain.setTransactionCacheSize(dbManager.getPendingTransactions().size());
    blockChain.setMissedTransactionCount(dbManager.getPendingTransactions().size()
        + dbManager.getRePushTransactions().size());

    RateInfo tpsInfo = blockChainMetricManager.getTransactionRate();
    blockChain.setTps(tpsInfo);

    List<WitnessInfo> witnesses = blockChainMetricManager.getNoUpgradedSR();

    blockChain.setWitnesses(witnesses);

    blockChain.setFailProcessBlockNum(metricsService.getFailProcessBlockNum());
    blockChain.setFailProcessBlockReason(metricsService.getFailProcessBlockReason());
    List<DupWitnessInfo> dupWitness = getDupWitness();
    blockChain.setDupWitness(dupWitness);

    data.setBlockInfo(blockChain);
  }

  private void setNetInfo(MetricsInfo data) {
    NetInfo netInfo = new NetInfo();

    //set connection info
    netInfo.setConnectionCount(tronNetDelegate.getActivePeer().size());
    int validConnectionCount = 0;
    for (PeerConnection peerConnection : tronNetDelegate.getActivePeer()) {
      if (!(peerConnection.isNeedSyncFromUs() || peerConnection.isNeedSyncFromPeer())) {
        validConnectionCount++;
      }
    }
    netInfo.setValidConnectionCount(validConnectionCount);

    long errorProtoCount = metricsService.getCounter(MetricsKey.NET_ERROR_PROTO_COUNT)
        .getCount();
    netInfo.setErrorProtoCount((int) errorProtoCount);

    RateInfo tcpInTraffic = new RateInfo();
    Meter tcpInTrafficMeter = metricsService.getMeter(MetricsKey.NET_TCP_IN_TRAFFIC);
    tcpInTraffic.setMeanRate(tcpInTrafficMeter.getMeanRate());
    tcpInTraffic.setOneMinuteRate(tcpInTrafficMeter.getOneMinuteRate());
    tcpInTraffic.setFiveMinuteRate(tcpInTrafficMeter.getFiveMinuteRate());
    tcpInTraffic.setFifteenMinuteRate(tcpInTrafficMeter.getFifteenMinuteRate());
    netInfo.setTcpInTraffic(tcpInTraffic);

    RateInfo tcpOutTraffic = new RateInfo();
    Meter tcpOutTrafficMeter = metricsService.getMeter(MetricsKey.NET_TCP_OUT_TRAFFIC);
    tcpOutTraffic.setMeanRate(tcpOutTrafficMeter.getMeanRate());
    tcpOutTraffic.setOneMinuteRate(tcpOutTrafficMeter.getOneMinuteRate());
    tcpOutTraffic.setFiveMinuteRate(tcpOutTrafficMeter.getFiveMinuteRate());
    tcpOutTraffic.setFifteenMinuteRate(tcpOutTrafficMeter.getFifteenMinuteRate());
    netInfo.setTcpOutTraffic(tcpOutTraffic);

    RateInfo udpInTraffic = new RateInfo();
    Meter udpInTrafficMeter = metricsService.getMeter(MetricsKey.NET_UDP_IN_TRAFFIC);
    udpInTraffic.setMeanRate(udpInTrafficMeter.getMeanRate());
    udpInTraffic.setOneMinuteRate(udpInTrafficMeter.getOneMinuteRate());
    udpInTraffic.setFiveMinuteRate(udpInTrafficMeter.getFiveMinuteRate());
    udpInTraffic.setFifteenMinuteRate(udpInTrafficMeter.getFifteenMinuteRate());
    netInfo.setUdpInTraffic(udpInTraffic);

    RateInfo udpOutTraffic = new RateInfo();
    Meter udpOutTrafficMeter = metricsService.getMeter(MetricsKey.NET_UDP_OUT_TRAFFIC);
    udpOutTraffic.setMeanRate(udpOutTrafficMeter.getMeanRate());
    udpOutTraffic.setOneMinuteRate(udpOutTrafficMeter.getOneMinuteRate());
    udpOutTraffic.setFiveMinuteRate(udpOutTrafficMeter.getFiveMinuteRate());
    udpOutTraffic.setFifteenMinuteRate(udpOutTrafficMeter.getFifteenMinuteRate());
    netInfo.setUdpOutTraffic(udpOutTraffic);

    // set api request info
    RateInfo common = new RateInfo();
    common.setMeanRate(HttpInterceptor.totalRequestCount.getMeanRate());
    common.setOneMinuteRate(HttpInterceptor.totalRequestCount.getOneMinuteCount());
    common.setFiveMinuteRate(HttpInterceptor.totalRequestCount.getFiveMinuteCount());
    common.setFifteenMinuteRate(HttpInterceptor.totalRequestCount.getFifteenMinuteCount());

    ApiInfo apiInfo = new ApiInfo();
    apiInfo.setTotalCount(common);

    RateInfo commonfail = new RateInfo();
    commonfail.setMeanRate(HttpInterceptor.totalFailRequestCount.getMeanRate());
    commonfail.setOneMinuteRate(HttpInterceptor.totalFailRequestCount.getOneMinuteCount());
    commonfail.setFiveMinuteRate(HttpInterceptor.totalFailRequestCount.getFiveMinuteCount());
    commonfail.setFifteenMinuteRate(HttpInterceptor.totalFailRequestCount.getFifteenMinuteCount());

    apiInfo.setTotalFailCount(commonfail);

    RateInfo commonOutTraffic = new RateInfo();
    commonOutTraffic.setMeanRate(HttpInterceptor.outTraffic.getMeanRate());
    commonOutTraffic.setOneMinuteRate(HttpInterceptor.outTraffic.getFiveMinuteCount());
    commonOutTraffic.setFiveMinuteRate(HttpInterceptor.outTraffic.getFiveMinuteCount());
    commonOutTraffic.setFifteenMinuteRate(HttpInterceptor.outTraffic.getFifteenMinuteCount());

    apiInfo.setTotalOutTraffic(commonOutTraffic);


    List<ApiDetailInfo> apiDetails = new ArrayList<>();
    for (Map.Entry<String, JSONObject> entry : HttpInterceptor.getEndpointMap().entrySet()) {
      ApiDetailInfo apiDetail = new ApiDetailInfo();
      apiDetail.setName(entry.getKey());
      JSONObject obj = entry.getValue();
      RateInfo commomCount = new RateInfo();
      commomCount.setMeanRate((double) obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_RPS));
      commomCount.setOneMinuteRate((int) obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_ONE_MINUTE));
      commomCount.setFiveMinuteRate((int) obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_FIVE_MINUTE));
      commomCount
          .setFifteenMinuteRate((int) obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE));

      apiDetail.setCount(commomCount);
      RateInfo commonFail = new RateInfo();
      commonFail.setMeanRate((double) obj.get(HttpInterceptor.END_POINT_FAIL_REQUEST_RPS));
      commonFail.setOneMinuteRate((int) obj.get(HttpInterceptor.END_POINT_FAIL_REQUEST_ONE_MINUTE));
      commonFail.setFiveMinuteRate((int) obj.get(HttpInterceptor.END_POINT_FAIL_REQUEST_FIVE_MINUTE));
      commonFail.setFifteenMinuteRate((int) obj.get(
          HttpInterceptor.END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE));

      apiDetail.setFailCount(commonFail);

      RateInfo commonTraffic = new RateInfo();
      commonTraffic.setMeanRate((double) obj.get(HttpInterceptor.END_POINT_OUT_TRAFFIC_BPS));
      commonTraffic.setOneMinuteRate((int) obj.get(HttpInterceptor.END_POINT_OUT_TRAFFIC_ONE_MINUTE));
      commonTraffic.setFiveMinuteRate((int) obj.get(HttpInterceptor.END_POINT_OUT_TRAFFIC_FIVE_MINUTE));
      commonTraffic.setFifteenMinuteRate((int) obj.get(
          HttpInterceptor.END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE));
      apiDetail.setOutTraffic(commonTraffic);

      apiDetails.add(apiDetail);
    }
    apiInfo.setDetail(apiDetails);
    netInfo.setApi(apiInfo);

    long disconnectionCount
        = metricsService.getCounter(MetricsKey.NET_DISCONNECTION_COUNT).getCount();
    netInfo.setDisconnectionCount((int) disconnectionCount);
    List<DisconnectionDetailInfo> disconnectionDetails =
        new ArrayList<>();
    SortedMap<String, Counter> disconnectionReason
        = metricsService.getCounters(MetricsKey.NET_DISCONNECTION_REASON);
    for (Map.Entry<String, Counter> entry : disconnectionReason.entrySet()) {
      DisconnectionDetailInfo detail = new DisconnectionDetailInfo();
      String reason = entry.getKey().substring(MetricsKey.NET_DISCONNECTION_REASON.length());
      detail.setReason(reason);
      detail.setCount((int) entry.getValue().getCount());
      disconnectionDetails.add(detail);
    }

    netInfo.setDisconnectionDetail(disconnectionDetails);

    LatencyInfo latencyInfo = getBlockLatencyInfo();
    netInfo.setLatency(latencyInfo);
    data.setNetInfo(netInfo);

  }

  // get public  ip address
  private String getMyIp() {
    try {
      URL url = new URL("http://checkip.amazonaws.com");
      BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
      String ipAddress = in.readLine().trim();
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

  private LatencyInfo getBlockLatencyInfo() {
    LatencyInfo latencyInfo = new LatencyInfo();
    long delay1SCount = metricsService.getCounter(MetricsKey.NET_BLOCK_LATENCY + ".1S")
            .getCount();
    latencyInfo.setDelay1S((int) delay1SCount);
    long delay2SCount = metricsService.getCounter(MetricsKey.NET_BLOCK_LATENCY + ".2S")
            .getCount();
    latencyInfo.setDelay2S((int) delay2SCount);
    long delay3SCount = metricsService.getCounter(MetricsKey.NET_BLOCK_LATENCY + ".3S")
            .getCount();
    latencyInfo.setDelay3S((int) delay3SCount);
    Histogram blockLatency = metricsService.getHistogram(MetricsKey.NET_BLOCK_LATENCY);
    latencyInfo.setTop99((int) blockLatency.getSnapshot().get99thPercentile());
    latencyInfo.setTop95((int) blockLatency.getSnapshot().get95thPercentile());
    latencyInfo.setTotalCount((int) blockLatency.getCount());

    List<LatencyDetailInfo> latencyDetailInfos = new ArrayList<>();
    SortedMap<String, Histogram> witnessLatencyMap
            = metricsService.getHistograms(MetricsKey.NET_BLOCK_LATENCY_WITNESS);
    for (Map.Entry<String, Histogram> entry : witnessLatencyMap.entrySet()) {
      LatencyDetailInfo latencyDetailTemp = new LatencyDetailInfo();
      String address = entry.getKey().substring(MetricsKey.NET_BLOCK_LATENCY_WITNESS.length());
      latencyDetailTemp.setCount((int) entry.getValue().getCount());
      latencyDetailTemp.setWitness(address);
      latencyDetailTemp.setTop99((int) entry.getValue().getSnapshot().get99thPercentile());
      latencyDetailTemp.setTop95((int) entry.getValue().getSnapshot().get95thPercentile());
      long witnessDelay1S = metricsService.getCounter(
              MetricsKey.NET_BLOCK_LATENCY_WITNESS + address + ".1S").getCount();
      latencyDetailTemp.setDelay1S((int) witnessDelay1S);
      long witnessDelay2S = metricsService.getCounter(
              MetricsKey.NET_BLOCK_LATENCY_WITNESS + address + ".2S").getCount();
      latencyDetailTemp.setDelay2S((int) witnessDelay2S);
      long witnessDelay3S = metricsService.getCounter(
              MetricsKey.NET_BLOCK_LATENCY_WITNESS + address + ".3S").getCount();
      latencyDetailTemp.setDelay3S((int) witnessDelay3S);
      latencyDetailInfos.add(latencyDetailTemp);
    }
    latencyInfo.setDetail(latencyDetailInfos);

    return latencyInfo;
  }

  // active 1, inactive 0- there is a exception during producing a block
  private int getNodeStatusByTime(int time) {
    switch (time) {
      case 0:
        return metricsService.getMeter(MetricsKey.NODE_STATUS).getMeanRate() > 0 ? 0 : 1;
      case 1:
        return metricsService.getMeter(MetricsKey.NODE_STATUS).getOneMinuteRate() > 0 ? 0 : 1;
      case 5:
        return metricsService.getMeter(MetricsKey.NODE_STATUS).getFiveMinuteRate() > 0 ? 0 : 1;
      case 15:
        return metricsService.getMeter(MetricsKey.NODE_STATUS)
                .getFifteenMinuteRate() > 0 ? 0 : 1;
      default:
        return -1;
    }
  }

  public int getSuccessForkCount() {
    return (int) metricsService.getMeter(MetricsKey.BLOCKCHAIN_SUCCESS_FORK_COUNT).getCount();
  }

  public int getFailForkCount() {
    return (int) metricsService.getMeter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount();
  }

  private List<DupWitnessInfo> getDupWitness() {
    List<DupWitnessInfo> dupWitnesses = new ArrayList<>();
    SortedMap<String, Counter> dupWitnessMap =
            metricsService.getCounters(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT);
    for (Map.Entry<String, Counter> entry : dupWitnessMap.entrySet()) {
      DupWitnessInfo dupWitness = new DupWitnessInfo();
      String witness = entry.getKey().substring(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT.length());
      long blockNum = metricsService.getDupWitnessBlockNum().get(witness);
      dupWitness.setAddress(witness);
      dupWitness.setBlockNum(blockNum);
      dupWitness.setCount((int)entry.getValue().getCount());
      dupWitnesses.add(dupWitness);
    }
    return dupWitnesses;
  }
}
