package org.tron.core.metrics.blockchain;

public class TpsInfo {
  private double meanRate;
  private double oneMinuteRate;
  private double fiveMinuteRate;
  private double fifteenMinuteRate;

  public double getMeanRate() {
    return this.meanRate;
  }

  public TpsInfo setMeanRate(double meanRate) {
    this.meanRate = meanRate;
    return this;
  }

  public double getOneMinuteRate() {
    return this.oneMinuteRate;
  }

  public TpsInfo setOneMinuteRate(double oneMinuteRate) {
    this.oneMinuteRate = oneMinuteRate;
    return this;
  }

  public double getFiveMinuteRate() {
    return this.fiveMinuteRate;
  }

  public TpsInfo setFiveMinuteRate(double fiveMinuteRate) {
    this.fiveMinuteRate = fiveMinuteRate;
    return this;
  }

  public double getFifteenMinuteRate() {
    return this.fifteenMinuteRate;
  }

  public TpsInfo setFifteenMinuteRate(double fifteenMinuteRate) {
    this.fifteenMinuteRate = fifteenMinuteRate;
    return this;
  }
}
