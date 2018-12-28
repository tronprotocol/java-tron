package org.tron.common.logsfilter.trigger;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    private List<String> transactionList = new ArrayList<>();

    public BlockLogTrigger(){
        setTriggerName(Trigger.BLOCK_TRIGGER_NAME);
    }

    @Override
    public String toString(){
      return new StringBuilder().append("triggerName: ").append(getTriggerName())
        .append("timestamp: ")
        .append(timeStamp)
        .append(", blockNumber: ")
        .append(blockNumber)
        .append(", blockhash: ")
        .append(blockHash)
        .append(", transaction size: ")
        .append(transactionSize)
        .append(", transaction list: ")
        .append(transactionList).toString();
    }
}
