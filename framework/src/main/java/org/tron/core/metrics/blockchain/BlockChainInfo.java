package org.tron.core.metrics.blockchain;

import java.util.List;
import org.tron.core.metrics.net.RateInfo;

public class BlockChainInfo {

  private long headBlockNum;
  private long headBlockTimestamp;
  private String headBlockHash;
  private int forkCount;
  private int failForkCount;
  private RateInfo blockProcessTime;
  private RateInfo tps;
  private int transactionCacheSize;
  private RateInfo missedTransaction;
  private List<WitnessInfo> witnesses;
  private long failProcessBlockNum;
  private String failProcessBlockReason;
  private List<DupWitnessInfo> dupWitness;

  public long getHeadBlockNum() {
    return headBlockNum;
  }

  public void setHeadBlockNum(long headBlockNum) {
    this.headBlockNum = headBlockNum;
  }

  public long getHeadBlockTimestamp() {
    return headBlockTimestamp;
  }

  public void setHeadBlockTimestamp(long headBlockTimestamp) {
    this.headBlockTimestamp = headBlockTimestamp;
  }

  public String getHeadBlockHash() {
    return headBlockHash;
  }

  public void setHeadBlockHash(String headBlockHash) {
    this.headBlockHash = headBlockHash;
  }

  public int getForkCount() {
    return forkCount;
  }

  public void setForkCount(int forkCount) {
    this.forkCount = forkCount;
  }

  public int getFailForkCount() {
    return failForkCount;
  }

  public void setFailForkCount(int failForkCount) {
    this.failForkCount = failForkCount;
  }

  public RateInfo getBlockProcessTime() {
    return blockProcessTime;
  }

  public void setBlockProcessTime(RateInfo blockProcessTime) {
    this.blockProcessTime = blockProcessTime;
  }

  public RateInfo getTps() {
    return tps;
  }

  public void setTps(RateInfo tps) {
    this.tps = tps;
  }

  public int getTransactionCacheSize() {
    return transactionCacheSize;
  }

  public void setTransactionCacheSize(int transactionCacheSize) {
    this.transactionCacheSize = transactionCacheSize;
  }

  public RateInfo getMissedTransaction() {
    return missedTransaction;
  }

  public void setMissedTransaction(RateInfo missedTransaction) {
    this.missedTransaction = missedTransaction;
  }

  public List<WitnessInfo> getWitnesses() {
    return witnesses;
  }

  public void setWitnesses(List<WitnessInfo> witnesses) {
    this.witnesses = witnesses;
  }

  public long getFailProcessBlockNum() {
    return failProcessBlockNum;
  }

  public void setFailProcessBlockNum(long failProcessBlockNum) {
    this.failProcessBlockNum = failProcessBlockNum;
  }

  public String getFailProcessBlockReason() {
    return failProcessBlockReason;
  }

  public void setFailProcessBlockReason(String failProcessBlockReason) {
    this.failProcessBlockReason = failProcessBlockReason;
  }

  public List<DupWitnessInfo> getDupWitness() {
    return dupWitness;
  }

  public void setDupWitness(List<DupWitnessInfo> dupWitness) {
    this.dupWitness = dupWitness;
  }

}

