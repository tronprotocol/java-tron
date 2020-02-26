package org.tron.core.metrics.net;

public class RateInfo {
  private double meanRate;
  private double oneMinuteRate;
  private double fiveMinuteRate;
  private double fifteenMinuteRate;

  public double getMeanRate() {
    return this.meanRate;
  }

  public RateInfo setMeanRate(double meanRate) {
    this.meanRate = meanRate;
    return this;
  }

  public double getOneMinuteRate() {
    return this.oneMinuteRate;
  }

  public RateInfo setOneMinuteRate(double oneMinuteRate) {
    this.oneMinuteRate = oneMinuteRate;
    return this;
  }

  public double getFiveMinuteRate() {
    return this.fiveMinuteRate;
  }

  public RateInfo setFiveMinuteRate(double fiveMinuteRate) {
    this.fiveMinuteRate = fiveMinuteRate;
    return this;
  }

  public double getFifteenMinuteRate() {
    return this.fifteenMinuteRate;
  }

  public RateInfo setFifteenMinuteRate(double fifteenMinuteRate) {
    this.fifteenMinuteRate = fifteenMinuteRate;
    return this;
  }
}
