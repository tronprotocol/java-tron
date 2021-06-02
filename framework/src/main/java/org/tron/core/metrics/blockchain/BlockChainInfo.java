package org.tron.core.metrics.blockchain;

import java.util.List;
import lombok.Data;
import org.tron.core.metrics.net.RateInfo;

@Data
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
}
