package org.tron.core.metrics.net;

public class Common {
  private double meanRate;
  private int oneMinute;
  private int fiveMinute;
  private int fifteenMinute;

  public double getMeanRate() {
    return this.meanRate;
  }

  public Common setMeanRate(double meanRate) {
    this.meanRate = meanRate;
    return this;
  }

  public int getOneMinute() {
    return this.oneMinute;
  }

  public Common setOneMinute(int oneMinuteCount) {
    this.oneMinute = oneMinuteCount;
    return this;
  }

  public int getFiveMinute() {
    return this.fiveMinute;
  }

  public Common setFiveMinute(int fiveMinuteCount) {
    this.fiveMinute = fiveMinuteCount;
    return this;
  }

  public int getFifteenMinute() {
    return this.fifteenMinute;
  }

  public Common setFifteenMinute(int fifteenMinuteCount) {
    this.fifteenMinute = fifteenMinuteCount;
    return this;
  }

}
