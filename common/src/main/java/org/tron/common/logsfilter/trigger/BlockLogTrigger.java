package org.tron.common.logsfilter.trigger;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class BlockLogTrigger extends Trigger {

  @Getter
  @Setter
  private long blockNumber;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private long transactionSize;

  @Getter
  @Setter
  private long latestSolidifiedBlockNumber;

  @Getter
  @Setter
  private List<String> transactionList = new ArrayList<>();

  // true when this block is being rolled back due to a chain reorg (fork switch);
  // mirrors the Ethereum log "removed" semantics already used by ContractTrigger.
  @Getter
  @Setter
  private boolean removed;

  public BlockLogTrigger() {
    setTriggerName(Trigger.BLOCK_TRIGGER_NAME);
  }

  @Override
  public String toString() {
    return new StringBuilder().append("triggerName: ").append(getTriggerName())
        .append(", timestamp: ")
        .append(timeStamp)
        .append(", blockNumber: ")
        .append(blockNumber)
        .append(", blockhash: ")
        .append(blockHash)
        .append(", transactionSize: ")
        .append(transactionSize)
        .append(", latestSolidifiedBlockNumber: ")
        .append(latestSolidifiedBlockNumber)
        .append(", removed: ")
        .append(removed)
        .append(", transactionList: ")
        .append(transactionList).toString();
  }
}
