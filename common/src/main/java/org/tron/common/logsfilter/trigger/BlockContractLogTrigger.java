package org.tron.common.logsfilter.trigger;

import com.google.common.hash.BloomFilter;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class BlockContractLogTrigger extends Trigger {

  @Getter
  @Setter
  private long blockNumber;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private long latestSolidifiedBlockNumber;

  // BloomFilter for Base58(contractAddress)+Hex(topic0)
  @Getter
  @Setter
  private BloomFilter<String> bloomFilterContractAndTopic;
  // BloomFilter for Base58(contractAddress)
  @Getter
  @Setter
  private BloomFilter<String> bloomFilterContract;
  // BloomFilter for Hex(topic0)
  @Getter
  @Setter
  private BloomFilter<String> bloomFilterTopic;

  @Getter
  @Setter
  private List<TransactionInBlock> transactionList = new ArrayList<>();

  public BlockContractLogTrigger() {
    setTriggerName(Trigger.BLOCK_CONTRACTLOG_TRIGGER_NAME);
  }


  public class TransactionInBlock {

    @Getter
    @Setter
    private String transactionId;
    @Getter
    @Setter
    private String contractAddress;
    @Getter
    @Setter
    private int transactionIndex;
    @Getter
    @Setter
    private List<ContractLogTrigger> logList = new ArrayList<>();
  }

}

