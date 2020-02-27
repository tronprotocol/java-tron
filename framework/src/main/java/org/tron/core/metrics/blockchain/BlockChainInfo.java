package org.tron.core.metrics.blockchain;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j(topic = "blockChainInfo")

public class BlockChainInfo {
  private int headBlockNum;
  private long headBlockTimestamp;
  private String headBlockHash;
  private int successForkCount;
  private int failForkCount;
  private TpsInfo blockProcessTime;
  private TpsInfo tps;
  private int transactionCacheSize;
  private int missedTransactionCount;
  private List<Witness> witnesses;
  private long failProcessBlockNum;
  private String failProcessBlockReason;
  private List<DupWitness> dupWitness;

  public int getHeadBlockNum() {
    return this.headBlockNum;
  }

  public BlockChainInfo setHeadBlockNum(int headBlockNum) {
    this.headBlockNum = headBlockNum;
    return this;
  }

  public long getHeadBlockTimestamp() {
    return this.headBlockTimestamp;
  }

  public BlockChainInfo setHeadBlockTimestamp(long headBlockTimestamp) {
    this.headBlockTimestamp = headBlockTimestamp;
    return this;
  }

  public String getHeadBlockHash() {
    return this.headBlockHash;
  }

  public BlockChainInfo setHeadBlockHash(String headBlockHash) {
    this.headBlockHash = headBlockHash;
    return this;
  }

  public int getSuccessForkCount() {
    return this.successForkCount;
  }

  public BlockChainInfo setSuccessForkCount(int forkCount) {
    this.successForkCount = forkCount;
    return this;
  }

  public int getFailForkCount() {
    return this.failForkCount;
  }

  public BlockChainInfo setFailForkCount(int forkCount) {
    this.failForkCount = forkCount;
    return this;
  }

  public TpsInfo getBlockProcessTime() {
    return this.blockProcessTime;
  }

  public BlockChainInfo setBlockProcessTime(TpsInfo blockProcessTime) {
    this.blockProcessTime = blockProcessTime;
    return this;
  }

  public TpsInfo getTps() {
    return this.tps;
  }

  public BlockChainInfo setTps(TpsInfo tps) {
    this.tps = tps;
    return this;
  }

  public int getTransactionCacheSize() {
    return this.transactionCacheSize;
  }


  public BlockChainInfo setTransactionCacheSize(int transactionCacheSize) {
    this.transactionCacheSize = transactionCacheSize;
    return this;
  }

  public int getMissedTransactionCount() {
    return this.missedTransactionCount;
  }

  public BlockChainInfo setMissedTransactionCount(int missedTransactionCount) {
    this.missedTransactionCount = missedTransactionCount;
    return this;
  }

  public List<Witness> getWitnesses() {
    return this.witnesses;
  }

  public BlockChainInfo setWitnesses(List<Witness> witnesses) {
    this.witnesses = witnesses;
    return this;
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

  public List<DupWitness> getDupWitness() {
    return dupWitness;
  }

  public void setDupWitness(List<DupWitness> dupWitness) {
    this.dupWitness = dupWitness;
  }

}

