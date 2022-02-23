package org.tron.core.metrics.net;

import lombok.Data;
import org.tron.protos.Protocol;

@Data
public class RateInfo {
  private double meanRate;
  private double oneMinuteRate;
  private double fiveMinuteRate;
  private double fifteenMinuteRate;
  private long count;

  public Protocol.MetricsInfo.RateInfo toProtoEntity() {
    Protocol.MetricsInfo.RateInfo.Builder rateInfoBuild =
        Protocol.MetricsInfo.RateInfo.newBuilder();
    rateInfoBuild.setCount(getCount());
    rateInfoBuild.setOneMinuteRate(getOneMinuteRate());
    rateInfoBuild.setFiveMinuteRate(getFiveMinuteRate());
    rateInfoBuild.setFifteenMinuteRate(getFifteenMinuteRate());
    rateInfoBuild.setMeanRate(getMeanRate());
    return rateInfoBuild.build();
  }
}
