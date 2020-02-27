package org.tron.core.services.filter;

import java.math.BigInteger;

public class RequestCount {
  private int oneMinuteCount;
  private int fiveMinuteCount;
  private int fifteenMinuteCount;
  private BigInteger total;
  private double meanRate;

  public RequestCount() {
    oneMinuteCount = 0;
    fiveMinuteCount = 0;
    fifteenMinuteCount = 0;
    meanRate = 0.0;
    total = new BigInteger("1");
  }

  public void bigIntegerIncrement(BigInteger total) {
    BigInteger one = new BigInteger("1");
    total.add(one);
  }

  public void bigIntegerIncrement(BigInteger total, int value) {
    BigInteger values = new BigInteger(String.valueOf(value));
    total.add(values);
  }

  public void allIncrement() {
    oneMinuteCount++;
    fiveMinuteCount++;
    fifteenMinuteCount++;
    bigIntegerIncrement(total);
  }

  public void allIncrement(int size) {
    oneMinuteCount = oneMinuteCount + size;
    fiveMinuteCount = fiveMinuteCount + size;
    fifteenMinuteCount = fifteenMinuteCount + size;
    bigIntegerIncrement(total, size);
  }

  public void allReset() {
    oneMinuteCount = 0;
    fiveMinuteCount = 0;
    fifteenMinuteCount = 0;
  }

  public void resetOneMinute() {
    oneMinuteCount = 0;
  }

  public void resetFiveMinute() {
    fiveMinuteCount = 0;
  }

  public void resetFifteenMinute() {
    fifteenMinuteCount = 0;
  }

  public void caculteMeanRate(long seconds) {
    BigInteger Seconds = new BigInteger(String.valueOf(seconds));
    meanRate = total.divide(Seconds).doubleValue();
  }

  public int getOneMinuteCount() {
    return oneMinuteCount;
  }

  public int getFiveMinuteCount() {
    return fiveMinuteCount;
  }

  public int getFifteenMinuteCount() {
    return fifteenMinuteCount;
  }

  public double getMeanRate() {
    return meanRate;
  }

  public double getOneMinuteRate() {
    return oneMinuteCount / (double) 60;
  }

  public double getFiveMinuteRate() {
    return fiveMinuteCount / (double) 5 * 60;
  }

  public double getFifteenMinuteRate() {
    return fifteenMinuteCount / (double) 15 * 60;
  }

}