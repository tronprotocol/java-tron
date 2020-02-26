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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.metrics.blockchain.BlockChainInfo;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.filter.HttpInterceptor;
import org.tron.program.Version;
import org.tron.protos.Protocol;


@Slf4j(topic = "metrics")
@Component
public class MetricsApiService {

  public List<BlockChainInfo.Witness> noUpgradedSRList = new ArrayList<>();
  private int totalSR = 27;

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

  /**
   * get metrics info.
   *
   * @return metricsInfo
   */
  public MetricsInfo getMetricsInfo() {

    MetricsInfo metricsInfo = new MetricsInfo();

    metricsInfo.setInterval((int) BlockChainInfo.startRecordTime);
    setNodeInfo(metricsInfo);

    setBlockchainInfo(metricsInfo);

    setNetInfo(metricsInfo);

    return metricsInfo;
  }

  public Protocol.MetricsInfo getProtoMonitorInfo() {
    return getMetricsInfo().toProtoEntity();
  }

  private void setNodeInfo(MetricsInfo data) {
    MetricsInfo.NodeInfo nodeInfo = new MetricsInfo.NodeInfo();
    nodeInfo.setIp(getMyIp());
    nodeInfo.setNodeType(1);
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
    MetricsInfo.BlockchainInfo blockChain = new MetricsInfo.BlockchainInfo();
    blockChain.setHeadBlockTimestamp(chainBaseManager.getHeadBlockTimeStamp());
    blockChain.setHeadBlockHash(dbManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderHash().toString());

    MetricsInfo.BlockchainInfo.TpsInfo blockProcessTime =
            new MetricsInfo.BlockchainInfo.TpsInfo();

    blockProcessTime.setMeanRate(getAvgBlockProcessTimeByGap(0));
    blockProcessTime.setOneMinuteRate(getAvgBlockProcessTimeByGap(1));
    blockProcessTime.setFiveMinuteRate(getAvgBlockProcessTimeByGap(5));
    blockProcessTime.setFifteenMinuteRate(getAvgBlockProcessTimeByGap(15));
    blockChain.setBlockProcessTime(blockProcessTime);
    blockChain.setSuccessForkCount(getSuccessForkCount());
    blockChain.setFailForkCount(getFailForkCount());
    blockChain.setHeadBlockNum((int) chainBaseManager.getHeadBlockNum());
    blockChain.setTransactionCacheSize(dbManager.getPendingTransactions().size());
    blockChain.setMissedTransactionCount(dbManager.getPendingTransactions().size()
            + dbManager.getRePushTransactions().size());


    Meter transactionRate = metricsService.getMeter(MetricsKey.BLOCKCHAIN_TPS);
    MetricsInfo.BlockchainInfo.TpsInfo tpsInfo =
            new MetricsInfo.BlockchainInfo.TpsInfo();
    tpsInfo.setMeanRate(transactionRate.getMeanRate());
    tpsInfo.setOneMinuteRate(transactionRate.getOneMinuteRate());
    tpsInfo.setFiveMinuteRate(transactionRate.getFiveMinuteRate());
    tpsInfo.setFifteenMinuteRate(transactionRate.getFifteenMinuteRate());
    blockChain.setTps(tpsInfo);

    getBlocks();
    List<MetricsInfo.BlockchainInfo.Witness> witnesses = new ArrayList<>();
    for (BlockChainInfo.Witness it : this.noUpgradedSRList) {
      MetricsInfo.BlockchainInfo.Witness noUpgradeSR =
              new MetricsInfo.BlockchainInfo.Witness();
      noUpgradeSR.setAddress(it.getAddress());
      noUpgradeSR.setVersion(it.getVersion());
      witnesses.add(noUpgradeSR);
    }
    blockChain.setWitnesses(witnesses);

    blockChain.setFailProcessBlockNum(tronNetDelegate.getFailProcessBlockNum());
    blockChain.setFailProcessBlockReason(tronNetDelegate.getFailProcessBlockReason());
    MetricsInfo.BlockchainInfo.DupWitness dupWitness = new MetricsInfo.BlockchainInfo.DupWitness();
    blockChain.setDupWitness(dupWitness);

    data.setBlockInfo(blockChain);
  }

  private void setNetInfo(MetricsInfo data) {
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

    long errorProtoCount = metricsService.getCounter(MetricsKey.NET_ERROR_PROTO_COUNT)
            .getCount();
    netInfo.setErrorProtoCount((int) errorProtoCount);

    MetricsInfo.NetInfo.RateInfo tcpInTraffic = new MetricsInfo.NetInfo.RateInfo();
    Meter tcpInTrafficMeter = metricsService.getMeter(MetricsKey.NET_TCP_IN_TRAFFIC);
    tcpInTraffic.setMeanRate(tcpInTrafficMeter.getMeanRate());
    tcpInTraffic.setOneMinuteRate(tcpInTrafficMeter.getOneMinuteRate());
    tcpInTraffic.setFiveMinuteRate(tcpInTrafficMeter.getFiveMinuteRate());
    tcpInTraffic.setFifteenMinuteRate(tcpInTrafficMeter.getFifteenMinuteRate());
    netInfo.setTcpInTraffic(tcpInTraffic);

    MetricsInfo.NetInfo.RateInfo tcpOutTraffic = new MetricsInfo.NetInfo.RateInfo();
    Meter tcpOutTrafficMeter = metricsService.getMeter(MetricsKey.NET_TCP_OUT_TRAFFIC);
    tcpOutTraffic.setMeanRate(tcpOutTrafficMeter.getMeanRate());
    tcpOutTraffic.setOneMinuteRate(tcpOutTrafficMeter.getOneMinuteRate());
    tcpOutTraffic.setFiveMinuteRate(tcpOutTrafficMeter.getFiveMinuteRate());
    tcpOutTraffic.setFifteenMinuteRate(tcpOutTrafficMeter.getFifteenMinuteRate());
    netInfo.setTcpOutTraffic(tcpOutTraffic);

    MetricsInfo.NetInfo.RateInfo udpInTraffic = new MetricsInfo.NetInfo.RateInfo();
    Meter udpInTrafficMeter = metricsService.getMeter(MetricsKey.NET_UDP_IN_TRAFFIC);
    udpInTraffic.setMeanRate(udpInTrafficMeter.getMeanRate());
    udpInTraffic.setOneMinuteRate(udpInTrafficMeter.getOneMinuteRate());
    udpInTraffic.setFiveMinuteRate(udpInTrafficMeter.getFiveMinuteRate());
    udpInTraffic.setFifteenMinuteRate(udpInTrafficMeter.getFifteenMinuteRate());
    netInfo.setUdpInTraffic(udpInTraffic);

    MetricsInfo.NetInfo.RateInfo udpOutTraffic = new MetricsInfo.NetInfo.RateInfo();
    Meter udpOutTrafficMeter = metricsService.getMeter(MetricsKey.NET_UDP_OUT_TRAFFIC);
    udpOutTraffic.setMeanRate(udpOutTrafficMeter.getMeanRate());
    udpOutTraffic.setOneMinuteRate(udpOutTrafficMeter.getOneMinuteRate());
    udpOutTraffic.setFiveMinuteRate(udpOutTrafficMeter.getFiveMinuteRate());
    udpOutTraffic.setFifteenMinuteRate(udpOutTrafficMeter.getFifteenMinuteRate());
    netInfo.setUdpOutTraffic(udpOutTraffic);

    // set api request info
    MetricsInfo.NetInfo.ApiInfo apiInfo = new MetricsInfo.NetInfo.ApiInfo();

    MetricsInfo.NetInfo.ApiInfo.Common common=new MetricsInfo.NetInfo.ApiInfo.Common();

    common.setMeanRate(HttpInterceptor.totalrequestCount.getMeanRate());
    common.setOneMinute(HttpInterceptor.totalrequestCount.getOneMinuteCount());
    common.setFiveMinute(HttpInterceptor.totalrequestCount.getFiveMinuteCount());
    common.setFifteenMinute(HttpInterceptor.totalrequestCount.getFifteenMinuteCount());

    apiInfo.setTotalCount(common);

    MetricsInfo.NetInfo.ApiInfo.Common commonfail=new MetricsInfo.NetInfo.ApiInfo.Common();
    commonfail.setMeanRate(HttpInterceptor.totalFailRequestCount.getMeanRate());
    commonfail.setOneMinute(HttpInterceptor.totalFailRequestCount.getOneMinuteCount());
    commonfail.setFiveMinute(HttpInterceptor.totalFailRequestCount.getFiveMinuteCount());
    commonfail.setFifteenMinute(HttpInterceptor.totalFailRequestCount.getFifteenMinuteCount());

    apiInfo.setTotalFailCount(commonfail);

    MetricsInfo.NetInfo.ApiInfo.Common commonOutTraffic=new MetricsInfo.NetInfo.ApiInfo.Common();
    commonOutTraffic.setMeanRate(HttpInterceptor.outTraffic.getMeanRate());
    commonOutTraffic.setOneMinute(HttpInterceptor.outTraffic.getFiveMinuteCount());
    commonOutTraffic.setFiveMinute(HttpInterceptor.outTraffic.getFiveMinuteCount());
    commonOutTraffic.setFifteenMinute(HttpInterceptor.outTraffic.getFifteenMinuteCount());

    apiInfo.setTotalOutTraffic(commonOutTraffic);


    List<MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo> apiDetails = new ArrayList<>();
    for (Map.Entry<String, JSONObject> entry : HttpInterceptor.getEndpointMap().entrySet()) {
      MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo apiDetail =
              new MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo();
      apiDetail.setName(entry.getKey());
      JSONObject obj=entry.getValue();
      MetricsInfo.NetInfo.ApiInfo.Common commomCount=new MetricsInfo.NetInfo.ApiInfo.Common();
      commomCount.setMeanRate((double)obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_RPS));
      commomCount.setOneMinute((int)obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_ONE_MINUTE));
      commomCount.setFiveMinute((int)obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_FIVE_MINUTE));
      commomCount.setFifteenMinute((int)obj.get(HttpInterceptor.END_POINT_ALL_REQUESTS_FIFTEEN_MINUTE));

      apiDetail.setCount(commomCount);
      MetricsInfo.NetInfo.ApiInfo.Common commonFail=new MetricsInfo.NetInfo.ApiInfo.Common();
      commonFail.setMeanRate((double)obj.get(HttpInterceptor.END_POINT_FAIL_REQUEST_RPS));
      commonFail.setOneMinute((int)obj.get(HttpInterceptor.END_POINT_FAIL_REQUEST_ONE_MINUTE));
      commonFail.setFiveMinute((int)obj.get(HttpInterceptor.END_POINT_FAIL_REQUEST_FIVE_MINUTE));
      commonFail.setFifteenMinute((int)obj.get(
          HttpInterceptor.END_POINT_FAIL_REQUEST_FIFTEEN_MINUTE));

      apiDetail.setFailCount(commonFail);

      MetricsInfo.NetInfo.ApiInfo.Common commonTraffic=new MetricsInfo.NetInfo.ApiInfo.Common();
      commonTraffic.setMeanRate((double)obj.get(HttpInterceptor.END_POINT_OUT_TRAFFIC_BPS));
      commonTraffic.setOneMinute((int)obj.get(HttpInterceptor.END_POINT_OUT_TRAFFIC_ONE_MINUTE));
      commonTraffic.setFiveMinute((int)obj.get(HttpInterceptor.END_POINT_OUT_TRAFFIC_FIVE_MINUTE));
      commonTraffic.setFifteenMinute((int)obj.get(
          HttpInterceptor.END_POINT_OUT_TRAFFIC_FIFTEEN_MINUTE));
      apiDetail.setOutTraffic(commonTraffic);

      apiDetails.add(apiDetail);
    }
    apiInfo.setApiDetailInfo(apiDetails);
    netInfo.setApi(apiInfo);

    long disconnectionCount
            = metricsService.getCounter(MetricsKey.NET_DISCONNECTION_COUNT).getCount();
    netInfo.setDisconnectionCount((int) disconnectionCount);
    List<MetricsInfo.NetInfo.DisconnectionDetailInfo> disconnectionDetails =
            new ArrayList<>();
    SortedMap<String, Counter> disconnectionReason
            = metricsService.getCounters(MetricsKey.NET_DISCONNECTION_REASON);
    for (Map.Entry<String, Counter> entry : disconnectionReason.entrySet()) {
      MetricsInfo.NetInfo.DisconnectionDetailInfo detail =
              new MetricsInfo.NetInfo.DisconnectionDetailInfo();
      String reason = entry.getKey().substring(MetricsKey.NET_DISCONNECTION_REASON.length());
      detail.setReason(reason);
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

  private MetricsInfo.NetInfo.LatencyInfo getBlockLatencyInfo() {
    MetricsInfo.NetInfo.LatencyInfo latencyInfo =
            new MetricsInfo.NetInfo.LatencyInfo();
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

    List<MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo> latencyDetailInfos =
            new ArrayList<>();
    SortedMap<String, Histogram> witnessLatencyMap
            = metricsService.getHistograms(MetricsKey.NET_BLOCK_LATENCY_WITNESS);
    for (Map.Entry<String, Histogram> entry : witnessLatencyMap.entrySet()) {
      MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo latencyDetailTemp =
              new MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo();
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
    latencyInfo.setLatencyDetailInfo(latencyDetailInfos);

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

  // gap: 1 minute, 5 minute, 15 minute, 0: avg for total block and time
  private double getAvgBlockProcessTimeByGap(int gap) {
    Meter meterBlockProcessTime =
        metricsService.getMeter(MetricsKey.BLOCKCHAIN_BLOCKPROCESS_TIME);
    Meter meterBlockTxCount = metricsService.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_COUNT);
    if (meterBlockTxCount.getCount() == 0) {
      return 0;
    }
    switch (gap) {
      case 0:
        return (meterBlockProcessTime.getCount() / (double)meterBlockTxCount.getCount());
      case 1:
        int gapMinuteTimeBlock =
            Math.round(Math.round(meterBlockProcessTime.getOneMinuteRate() * 60));
        int gapMinuteCount = Math.round(Math.round(meterBlockTxCount.getOneMinuteRate() * 60));
        return   gapMinuteTimeBlock / (double)gapMinuteCount;
      case 5:
        int gapFiveTimeBlock =
            Math.round(Math.round(meterBlockProcessTime.getFiveMinuteRate() * gap * 60));
        int gapFiveTimeCount =
            Math.round(Math.round(meterBlockTxCount.getFiveMinuteRate() * gap * 60));
        return gapFiveTimeBlock / (double) gapFiveTimeCount;
      case 15:
        int gapFifteenTimeBlock =
            Math.round(Math.round(meterBlockProcessTime.getFifteenMinuteRate() * gap * 60));
        int gapFifteenTimeCount =
            Math.round(Math.round(meterBlockTxCount.getFifteenMinuteRate() * gap * 60));
        return gapFifteenTimeBlock / (double)gapFifteenTimeCount;

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

  private void getBlocks()  {

    List<BlockCapsule> blocks = chainBaseManager.getBlockStore().getBlockByLatestNum(totalSR);

    // get max version number
    int maxVersion = 0;
    for (BlockCapsule it : blocks) {
      maxVersion = Math.max(maxVersion,
              it.getInstance().getBlockHeader().getRawData().getVersion());
    }
    // find no Upgrade SR
    for (BlockCapsule it : blocks) {
      if (it.getInstance().getBlockHeader().getRawData().getVersion() != maxVersion) {
        BlockChainInfo.Witness witness = new BlockChainInfo.Witness(
            it.getWitnessAddress().toStringUtf8(),
            it.getInstance().getBlockHeader().getRawData().getVersion());
        this.noUpgradedSRList.add(witness);
      }
    }
  }
}
