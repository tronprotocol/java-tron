package org.tron.core.metrics.net;

public class LatencyDetailInfo {

  private String witness;
  private int top99;
  private int top95;
  private int top75;
  private int count;
  private int delay1S;
  private int delay2S;
  private int delay3S;

  public String getWitness() {
    return witness;
  }

  public void setWitness(String witness) {
    this.witness = witness;
  }

  public int getTop99() {
    return top99;
  }

  public void setTop99(int top99) {
    this.top99 = top99;
  }

  public int getTop95() {
    return top95;
  }

  public void setTop95(int top95) {
    this.top95 = top95;
  }

  public int getTop75() {
    return top75;
  }

  public void setTop75(int top75) {
    this.top75 = top75;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getDelay1S() {
    return delay1S;
  }

  public void setDelay1S(int delay1S) {
    this.delay1S = delay1S;
  }

  public int getDelay2S() {
    return delay2S;
  }

  public void setDelay2S(int delay2S) {
    this.delay2S = delay2S;
  }

  public int getDelay3S() {
    return delay3S;
  }

  public void setDelay3S(int delay3S) {
    this.delay3S = delay3S;
  }
}
