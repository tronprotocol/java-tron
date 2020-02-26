package org.tron.core.metrics.net;

public class LatencyDetailInfo {
  private String witness;
  private int top99;
  private int top95;
  private int count;
  private int delay1S;
  private int delay2S;
  private int delay3S;

  public String getWitness() {
    return this.witness;
  }

  public LatencyDetailInfo setWitness(String witness) {
    this.witness = witness;
    return this;
  }

  public int getTop99() {
    return this.top99;
  }

  public LatencyDetailInfo setTop99(int top99) {
    this.top99 = top99;
    return this;
  }

  public int getTop95() {
    return this.top95;
  }

  public LatencyDetailInfo setTop95(int top95) {
    this.top95 = top95;
    return this;
  }

  public int getCount() {
    return this.count;
  }

  public LatencyDetailInfo setCount(int count) {
    this.count = count;
    return this;
  }

  public int getDelay1S() {
    return this.delay1S;
  }

  public LatencyDetailInfo setDelay1S(int delay1S) {
    this.delay1S = delay1S;
    return this;
  }

  public int getDelay2S() {
    return this.delay2S;
  }

  public LatencyDetailInfo setDelay2S(int delay2S) {
    this.delay2S = delay2S;
    return this;
  }

  public int getDelay3S() {
    return this.delay3S;
  }

  public LatencyDetailInfo setDelay3S(int delay3S) {
    this.delay3S = delay3S;
    return this;
  }

}
