package org.tron.core.metrics.net;

public class RateInfo {
  private double meanRate;
  private double oneMinuteRate;
  private double fiveMinuteRate;
  private double fifteenMinuteRate;
  private int count;

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

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}
