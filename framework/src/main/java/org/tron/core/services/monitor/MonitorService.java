package org.tron.core.services.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.protos.Protocol;
import org.tron.core.services.filter.HttpInterceptor;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "monitorService")
@Component
public class MonitorService {

  @Autowired
  private MonitorMetric monitorMetric;

  public Protocol.MonitorInfo getMonitorInfo() {
    MonitorInfo monitorInfo = new MonitorInfo();
    monitorInfo.setStatus(1);
    monitorInfo.setMsg("success");
    MonitorInfo.DataInfo data = new MonitorInfo.DataInfo();
    data.setInterval(60);
    setNodeInfo(data);

    setBlockchainInfo(data);

    setNetInfo(data);

    monitorInfo.setDataInfo(data);

    return monitorInfo.ToProtoEntity();
  }

  public void setNodeInfo(MonitorInfo.DataInfo data) {
    MonitorInfo.DataInfo.NodeInfo nodeInfo = new MonitorInfo.DataInfo.NodeInfo();
    nodeInfo.setIp("127.0.0.1");
    nodeInfo.setType(1);
    nodeInfo.setStatus(1);
    nodeInfo.setVersion("3.6.5");
    nodeInfo.setNoUpgradedSRCount(2);

    List<MonitorInfo.DataInfo.NodeInfo.NoUpgradedSR> noUpgradeSRs = new ArrayList<>();
    MonitorInfo.DataInfo.NodeInfo.NoUpgradedSR noUpgradeSR =
        new MonitorInfo.DataInfo.NodeInfo.NoUpgradedSR();
    noUpgradeSR.setAddress("41d376d829440505ea13c9d1c455317d51b62e4ab6");
    noUpgradeSR.setUrl("http://blockchain.org");
    noUpgradeSRs.add(noUpgradeSR);
    nodeInfo.setNoUpgradedSRList(noUpgradeSRs);
    data.setNodeInfo(nodeInfo);
  }

  public void setBlockchainInfo(MonitorInfo.DataInfo data) {
    MonitorInfo.DataInfo.BlochainInfo blockChain = new MonitorInfo.DataInfo.BlochainInfo();
    blockChain.setHeadBlockTimestamp(1581957662);
    blockChain.setHeadBlockHash("000000000105c43e397da4a5c73cf39be735520875cf04c9d91f371103d05ec0");
    blockChain.setBlockProcessTime(1000);
    blockChain.setForkCount(1);
    blockChain.setHeadBlockNum(10000);
    blockChain.setTxCacheSize(1000);
    blockChain.setMissTxCount(100);

//    MonitorInfo.DataInfo.BlochainInfo.TPSInfo tpsInfo =
//        new MonitorInfo.DataInfo.BlochainInfo.TPSInfo();
//    tpsInfo.setMeanRate(2);
//    tpsInfo.setOneMinuteRate(3);
//    tpsInfo.setFiveMinuteRate(2);
//    tpsInfo.setFifteenMinuteRate(4);
    Meter transactionRate = monitorMetric.getMeter(MonitorMetric.BLOCKCHAIN_TPS);
    MonitorInfo.DataInfo.BlochainInfo.TPSInfo tpsInfo =
            new MonitorInfo.DataInfo.BlochainInfo.TPSInfo();
    tpsInfo.setMeanRate(transactionRate.getMeanRate());
    tpsInfo.setOneMinuteRate(transactionRate.getOneMinuteRate());
    tpsInfo.setFiveMinuteRate(transactionRate.getFiveMinuteRate());
    tpsInfo.setFifteenMinuteRate(transactionRate.getFifteenMinuteRate());

    blockChain.setTPS(tpsInfo);
    data.setBlockInfo(blockChain);

  }

  public void setNetInfo(MonitorInfo.DataInfo data) {
    MonitorInfo.DataInfo.NetInfo netInfo = new MonitorInfo.DataInfo.NetInfo();
    netInfo.setConnectionCount(20);
    netInfo.setValidConnectionCount(19);
    netInfo.setErrorProtoCount(10);
    netInfo.setTCPInTraffic(10000);
    netInfo.setTCPOutTraffic(10001);
    netInfo.setDisconnectionCount(12);
    netInfo.setUDPInTraffic(1000);
    netInfo.setUDPOutTraffic(1001);

    // set api request info
    MonitorInfo.DataInfo.NetInfo.ApiInfo apiInfo = new MonitorInfo.DataInfo.NetInfo.ApiInfo();
    HttpInterceptor httpCount=new HttpInterceptor();
    apiInfo.setTotalCount(httpCount.getInstance().getTotalCount());
    apiInfo.setTotalFailCount(httpCount.getInstance().getFailCount());
    List<MonitorInfo.DataInfo.NetInfo.ApiInfo.ApiDetailInfo> apiDetails = new ArrayList<>();
    for(Map.Entry<String,JSONObject> entry: httpCount.getInstance().getEndpointMap().entrySet()){
      MonitorInfo.DataInfo.NetInfo.ApiInfo.ApiDetailInfo apiDetail =
          new MonitorInfo.DataInfo.NetInfo.ApiInfo.ApiDetailInfo();
      apiDetail.setName(entry.getKey());
      apiDetail.setCount((int)entry.getValue().get(HttpInterceptor.TOTAL_REQUST));
      apiDetail.setFailCount((int)entry.getValue().get(HttpInterceptor.FAIL_REQUST));
      apiDetails.add(apiDetail);
    }
    apiInfo.setApiDetailInfo(apiDetails);
    netInfo.setApi(apiInfo);

    List<MonitorInfo.DataInfo.NetInfo.DisconnectionDetailInfo> disconnectionDetails =
        new ArrayList<>();
    MonitorInfo.DataInfo.NetInfo.DisconnectionDetailInfo disconnectionDetail =
        new MonitorInfo.DataInfo.NetInfo.DisconnectionDetailInfo();

    disconnectionDetail.setReason("TOO_MANY_PEERS");
    disconnectionDetail.setCount(12);
    disconnectionDetails.add(disconnectionDetail);
    netInfo.setDisconnectionDetail(disconnectionDetails);

    MonitorInfo.DataInfo.NetInfo.LatencyInfo latencyInfo =
        new MonitorInfo.DataInfo.NetInfo.LatencyInfo();
    latencyInfo.setDelay1S(12);
    latencyInfo.setDelay2S(5);
    latencyInfo.setDelay3S(1);
    Histogram blockLatency = monitorMetric.getHistogram(MonitorMetric.NET_BLOCK_LATENCY);
    latencyInfo.setTop99((int)blockLatency.getSnapshot().get99thPercentile());
    latencyInfo.setTop95((int)blockLatency.getSnapshot().get95thPercentile());
    latencyInfo.setTotalCount((int)blockLatency.getCount());

    MonitorInfo.DataInfo.NetInfo.LatencyInfo.LatencyDetailInfo latencyDetail =
        new MonitorInfo.DataInfo.NetInfo.LatencyInfo.LatencyDetailInfo();
    latencyDetail.setCount(10);
    latencyDetail.setWitness("41d376d829440505ea13c9d1c455317d51b62e4ab6");
    latencyDetail.setTop99(11);
    latencyDetail.setTop95(8);
    latencyDetail.setDelay1S(3);
    latencyDetail.setDelay2S(1);
    latencyDetail.setDelay3S(0);
    List<MonitorInfo.DataInfo.NetInfo.LatencyInfo.LatencyDetailInfo> latencyDetailInfos =
        new ArrayList<>();
    latencyDetailInfos.add(latencyDetail);
    latencyInfo.setLatencyDetailInfo(latencyDetailInfos);
    netInfo.setLatency(latencyInfo);
    data.setNetInfo(netInfo);

  }

}
