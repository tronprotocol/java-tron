package org.tron.core.metrics;

public class BlockChainInfo {
  public static int interval;
  public static int totalSuccessForkCount = 0;
  public static int totalFailForkCount = 0;
  public static long startRecordTime;
  private static long startTime;
  private static int totalProduceExpectionCount = 0;
  public static int produceBlockexpectionCount = 0;
  private int successForkCount = 0;
  private int failForkCount = 0;
  private boolean produceBlockExpection;

  public BlockChainInfo(int interval) {
    this.produceBlockExpection = false;
    this.interval = interval;
  }

  public BlockChainInfo(boolean produceExpection) {
    this.produceBlockExpection = produceExpection;
    long nowTime = System.currentTimeMillis();
    if (nowTime - startTime > this.interval * 60 * 60) {
      this.successForkCount = 0;
      this.failForkCount = 0;
    }
  }
//
//  public void incrementForkCount() {
//    long nowTime = System.currentTimeMillis();
//    if (nowTime - startRecordTime > interval * 60 * 60) {  //reset every Period
//      this.successForkCount = 0;
//      produceBlockExpection = false;
//    }
//  }

  public void setProduceExpection(boolean produceExpection) {

    if (produceExpection == true) {
      long nowTime = System.currentTimeMillis();
      this.totalProduceExpectionCount++;
      if (nowTime - startTime > this.interval * 60 * 60) {
        startTime = nowTime;
        this.produceBlockexpectionCount = 0;
      } else {
        this.produceBlockexpectionCount++;
      }
    }
    this.produceBlockExpection = produceExpection;
  }

  public int getSuccessForkCount() {
    return this.successForkCount;
  }

  public int getBlockProduceExpectionCount(){
     return this.produceBlockexpectionCount;
  }


}
