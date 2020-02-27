package org.tron.core.metrics.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.tron.core.metrics.net.RateInfo;

import java.util.List;

@Slf4j(topic = "blockChainInfo")

public class BlockChainInfo {
  private int headBlockNum;
  private long headBlockTimestamp;
  private String headBlockHash;
  private int successForkCount;
  private int failForkCount;
  private RateInfo blockProcessTime;
  private RateInfo tps;
  private int transactionCacheSize;
  private int missedTransactionCount;
  private List<WitnessInfo> witnesses;
  private long failProcessBlockNum;
  private String failProcessBlockReason;
  private List<DupWitnessInfo> dupWitnessInfos;

  public int getHeadBlockNum() {
    return headBlockNum;
  }

  public void setHeadBlockNum(int headBlockNum) {
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

  public int getSuccessForkCount() {
    return successForkCount;
  }

  public void setSuccessForkCount(int successForkCount) {
    this.successForkCount = successForkCount;
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

  public int getMissedTransactionCount() {
    return missedTransactionCount;
  }

  public void setMissedTransactionCount(int missedTransactionCount) {
    this.missedTransactionCount = missedTransactionCount;
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

  public List<DupWitnessInfo> getDupWitnessInfos() {
    return dupWitnessInfos;
  }

  public void setDupWitnessInfos(List<DupWitnessInfo> dupWitnessInfos) {
    this.dupWitnessInfos = dupWitnessInfos;
  }

  public static Logger getLogger() {
    return logger;
  }
}

