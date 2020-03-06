package org.tron.core.metrics.net;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.filter.HttpInterceptor;


@Component
public class NetMetricManager {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  /**
   * get net info.
   *
   * @return NetInfo
   */
  public NetInfo getNetInfo() {
    NetInfo netInfo = new NetInfo();
    setNetInfo(netInfo);
    return netInfo;
  }

  private void setNetInfo(NetInfo netInfo) {
    //set connection info
    netInfo.setConnectionCount(tronNetDelegate.getActivePeer().size());
    int validConnectionCount = 0;
    for (PeerConnection peerConnection : tronNetDelegate.getActivePeer()) {
      if (!(peerConnection.isNeedSyncFromUs() || peerConnection.isNeedSyncFromPeer())) {
        validConnectionCount++;
      }
    }
    netInfo.setValidConnectionCount(validConnectionCount);

    long errorProtoCount = MetricsUtil.getCounter(MetricsKey.NET_ERROR_PROTO_COUNT)
        .getCount();
    netInfo.setErrorProtoCount((int) errorProtoCount);

    RateInfo tcpInTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_TCP_IN_TRAFFIC);
    netInfo.setTcpInTraffic(tcpInTraffic);

    RateInfo tcpOutTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_TCP_OUT_TRAFFIC);
    netInfo.setTcpOutTraffic(tcpOutTraffic);

    RateInfo udpInTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_UDP_IN_TRAFFIC);
    netInfo.setUdpInTraffic(udpInTraffic);

    RateInfo udpOutTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_UDP_OUT_TRAFFIC);
    netInfo.setUdpOutTraffic(udpOutTraffic);

    // set api request info
    ApiInfo apiInfo = new ApiInfo();
    RateInfo APIQPS = MetricsUtil.getRateInfo(MetricsKey.NET_API_QPS);
    apiInfo.setQps(APIQPS);

    RateInfo FailQPS = MetricsUtil.getRateInfo(MetricsKey.NET_API_FAIL_QPS);
    apiInfo.setFailQps(FailQPS);

    RateInfo totalOutTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_API_OUT_TRAFFIC);
    apiInfo.setOutTraffic(totalOutTraffic);


    List<ApiDetailInfo> apiDetails = new ArrayList<>();
    SortedMap<String, Meter> endpointQPSMap
        = MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_QPS);
    SortedMap<String, Meter> endpointFailQPSMap
        = MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_FAIL_QPS);
    SortedMap<String, Meter> endpointOutTrafficMap
        = MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC);
    for (Map.Entry<String, Meter> entry : endpointQPSMap.entrySet()) {
      ApiDetailInfo apiDetail = new ApiDetailInfo();
      String endpointName = entry.getKey().substring(MetricsKey.NET_API_DETAIL_QPS.length());
      apiDetail.setName(endpointName);
      RateInfo APIDetailQPS = MetricsUtil.getRateInfo(entry.getValue());
      apiDetail.setQps(APIDetailQPS);
      if (endpointOutTrafficMap.containsKey(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpointName)) {
        RateInfo APIDetailOutTraffic = MetricsUtil.getRateInfo(endpointOutTrafficMap
            .get(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpointName));
        apiDetail.setOutTraffic(APIDetailOutTraffic);
      }
      if (endpointFailQPSMap.containsKey(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpointName)) {
        RateInfo APIDetailFailQps = MetricsUtil.getRateInfo(endpointFailQPSMap
            .get(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpointName));
        apiDetail.setFailQps(APIDetailFailQps);
      }
      apiDetails.add(apiDetail);
    }

    apiInfo.setDetail(apiDetails);
    netInfo.setApi(apiInfo);

    long disconnectionCount
        = MetricsUtil.getCounter(MetricsKey.NET_DISCONNECTION_COUNT).getCount();
    netInfo.setDisconnectionCount((int) disconnectionCount);
    List<DisconnectionDetailInfo> disconnectionDetails =
        new ArrayList<>();
    SortedMap<String, Counter> disconnectionReason
        = MetricsUtil.getCounters(MetricsKey.NET_DISCONNECTION_DETAIL);
    for (Map.Entry<String, Counter> entry : disconnectionReason.entrySet()) {
      DisconnectionDetailInfo detail = new DisconnectionDetailInfo();
      String reason = entry.getKey().substring(MetricsKey.NET_DISCONNECTION_DETAIL.length());
      detail.setReason(reason);
      detail.setCount((int) entry.getValue().getCount());
      disconnectionDetails.add(detail);
    }
    netInfo.setDisconnectionDetail(disconnectionDetails);

    LatencyInfo latencyInfo = getBlockLatencyInfo();
    netInfo.setLatency(latencyInfo);
  }


  private LatencyInfo getBlockLatencyInfo() {
    LatencyInfo latencyInfo = new LatencyInfo();
    long delay1SCount = MetricsUtil.getCounter(MetricsKey.NET_LATENCY + ".1S")
            .getCount();
    latencyInfo.setDelay1S((int) delay1SCount);
    long delay2SCount = MetricsUtil.getCounter(MetricsKey.NET_LATENCY + ".2S")
            .getCount();
    latencyInfo.setDelay2S((int) delay2SCount);
    long delay3SCount = MetricsUtil.getCounter(MetricsKey.NET_LATENCY + ".3S")
            .getCount();
    latencyInfo.setDelay3S((int) delay3SCount);
    Histogram blockLatency = MetricsUtil.getHistogram(MetricsKey.NET_LATENCY);
    latencyInfo.setTop99((int) blockLatency.getSnapshot().get99thPercentile());
    latencyInfo.setTop95((int) blockLatency.getSnapshot().get95thPercentile());
    latencyInfo.setTop75((int) blockLatency.getSnapshot().get75thPercentile());
    latencyInfo.setTotalCount((int) blockLatency.getCount());

    List<LatencyDetailInfo> latencyDetailInfos = new ArrayList<>();
    SortedMap<String, Histogram> witnessLatencyMap
            = MetricsUtil.getHistograms(MetricsKey.NET_LATENCY_WITNESS);
    for (Map.Entry<String, Histogram> entry : witnessLatencyMap.entrySet()) {
      LatencyDetailInfo latencyDetailTemp = new LatencyDetailInfo();
      String address = entry.getKey().substring(MetricsKey.NET_LATENCY_WITNESS.length());
      latencyDetailTemp.setCount((int) entry.getValue().getCount());
      latencyDetailTemp.setWitness(address);
      latencyDetailTemp.setTop99((int) entry.getValue().getSnapshot().get99thPercentile());
      latencyDetailTemp.setTop95((int) entry.getValue().getSnapshot().get95thPercentile());
      latencyDetailTemp.setTop75((int) entry.getValue().getSnapshot().get75thPercentile());
      long witnessDelay1S = MetricsUtil.getCounter(
              MetricsKey.NET_LATENCY_WITNESS + address + ".1S").getCount();
      latencyDetailTemp.setDelay1S((int) witnessDelay1S);
      long witnessDelay2S = MetricsUtil.getCounter(
              MetricsKey.NET_LATENCY_WITNESS + address + ".2S").getCount();
      latencyDetailTemp.setDelay2S((int) witnessDelay2S);
      long witnessDelay3S = MetricsUtil.getCounter(
              MetricsKey.NET_LATENCY_WITNESS + address + ".3S").getCount();
      latencyDetailTemp.setDelay3S((int) witnessDelay3S);
      latencyDetailInfos.add(latencyDetailTemp);
    }
    latencyInfo.setDetail(latencyDetailInfos);

    return latencyInfo;
  }
}
