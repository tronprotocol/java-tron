package org.tron.core.metrics.net;

import org.tron.protos.Protocol;

public class RateInfo {

  private double meanRate;
  private double oneMinuteRate;
  private double fiveMinuteRate;
  private double fifteenMinuteRate;
  private long count;

  public double getMeanRate() {
    return meanRate;
  }

  public void setMeanRate(double meanRate) {
    this.meanRate = meanRate;
  }

  public double getOneMinuteRate() {
    return oneMinuteRate;
  }

  public void setOneMinuteRate(double oneMinuteRate) {
    this.oneMinuteRate = oneMinuteRate;
  }

  public double getFiveMinuteRate() {
    return fiveMinuteRate;
  }

  public void setFiveMinuteRate(double fiveMinuteRate) {
    this.fiveMinuteRate = fiveMinuteRate;
  }

  public double getFifteenMinuteRate() {
    return fifteenMinuteRate;
  }

  public void setFifteenMinuteRate(double fifteenMinuteRate) {
    this.fifteenMinuteRate = fifteenMinuteRate;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

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
