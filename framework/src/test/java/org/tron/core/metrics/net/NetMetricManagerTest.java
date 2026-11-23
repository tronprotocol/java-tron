package org.tron.core.metrics.net;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.protos.Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo;

public class NetMetricManagerTest {

  @Test
  public void getNetProtoInfoPreservesLatencyDetailsForEachWitness() {
    String firstWitness = "410be88a918d74d0dfd71dc84bd4abf036d0562991";
    String secondWitness = "413ed7d77d2eb807375a34e1a0043c5ba7e8926265";

    org.tron.core.metrics.net.LatencyDetailInfo first =
        new org.tron.core.metrics.net.LatencyDetailInfo();
    first.setWitness(firstWitness);
    first.setTop99(3200);
    first.setTop95(2300);
    first.setTop75(1400);
    first.setCount(50);
    first.setDelay1S(35);
    first.setDelay2S(12);
    first.setDelay3S(4);

    org.tron.core.metrics.net.LatencyDetailInfo second =
        new org.tron.core.metrics.net.LatencyDetailInfo();
    second.setWitness(secondWitness);
    second.setTop99(3600);
    second.setTop95(2700);
    second.setTop75(1600);
    second.setCount(70);
    second.setDelay1S(43);
    second.setDelay2S(18);
    second.setDelay3S(6);

    LatencyInfo latency = new LatencyInfo();
    latency.setDetail(Arrays.asList(first, second));
    RateInfo rate = new RateInfo();
    ApiInfo api = new ApiInfo();
    api.setQps(rate);
    api.setFailQps(rate);
    api.setOutTraffic(rate);
    NetInfo netInfo = new NetInfo();
    netInfo.setApi(api);
    netInfo.setTcpInTraffic(rate);
    netInfo.setTcpOutTraffic(rate);
    netInfo.setUdpInTraffic(rate);
    netInfo.setUdpOutTraffic(rate);
    netInfo.setLatency(latency);

    NetMetricManager manager = new NetMetricManager() {
      @Override
      public NetInfo getNetInfo() {
        return netInfo;
      }
    };

    List<LatencyDetailInfo> actual = manager.getNetProtoInfo().getLatency().getDetailList();
    Assert.assertEquals(Arrays.asList(
        LatencyDetailInfo.newBuilder()
            .setWitness(firstWitness)
            .setTop99(3200).setTop95(2300).setTop75(1400).setCount(50)
            .setDelay1S(35).setDelay2S(12).setDelay3S(4)
            .build(),
        LatencyDetailInfo.newBuilder()
            .setWitness(secondWitness)
            .setTop99(3600).setTop95(2700).setTop75(1600).setCount(70)
            .setDelay1S(43).setDelay2S(18).setDelay3S(6)
            .build()), actual);
  }
}
