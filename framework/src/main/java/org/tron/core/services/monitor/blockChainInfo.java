package org.tron.core.services.monitor;

public class blockChainInfo {
  public static int interval;
  private static long startTime;
  private static long endTime;
  private static boolean produceExpection;
  public static int forkCount = 0;
  private long startRecordTime;

  public blockChainInfo(int interval) {
    startTime = System.currentTimeMillis();
    startRecordTime = System.currentTimeMillis();
    produceExpection = false;
    forkCount = 0;
    this.interval = interval;
  }


  public blockChainInfo(boolean produceExpection) {
    startTime = System.currentTimeMillis();
    this.produceExpection = produceExpection;
  }

  public long getBlockProduceTime() {
    if (produceExpection) {
      return 0;
    } else {
      return endTime - startTime;
    }
  }

  public void incrementForkCount() {
    long nowTime = System.currentTimeMillis();
    if (nowTime - startRecordTime > interval * 60 * 60) {  //reset every Period
      this.forkCount = 0;
      startRecordTime = nowTime;
      produceExpection = false;
    }
  }

  public void setProduceExpection(boolean produceExpection) {
    this.produceExpection = produceExpection;
  }

  public int getForkCount() {
    return this.forkCount;
  }

  public void setStartTime(long time) {
    this.startTime = time;
  }

  public void setEndTime(long time) {
    this.endTime = time;
  }

  public void setEndCurrentTime() {
    this.endTime = System.currentTimeMillis();
  }

}
