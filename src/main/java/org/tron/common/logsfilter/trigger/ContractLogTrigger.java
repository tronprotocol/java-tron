package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ContractLogTrigger extends Trigger{
    @Getter
    @Setter
    private String eventType;

    @Getter
    @Setter
    private long blockNum;

    @Getter
    @Setter
    private long blockTimestamp;

    @Getter
    @Setter
    private String trxHash;

    @Getter
    @Setter
    private String blockHash;

    @Getter
    @Setter
    private long logIndex;

    @Getter
    @Setter
    private long txId;

    @Getter
    @Setter
    private String contractAddress;

    @Getter
    @Setter
    private String callerAddress;

    @Getter
    @Setter
    private String creatorAddress;

    @Getter
    @Setter
    private List<String> contractTopics;

    @Getter
    @Setter
    private String data;

    @Getter
    @Setter
    private boolean removed;

    @Override
    public String toString(){
        return new StringBuilder().append("timestamp: ")
                .append(timeStamp)
                .append(", eventType: ")
                .append(eventType)
                .append(", blockNum: ")
                .append(blockNum)
                .append(", blockTimestamp: ")
                .append(blockTimestamp)
                .append(", trxHash: ")
                .append(trxHash)
                .append(", blockHash: ")
                .append(blockHash)
                .append(", logIndex: ")
                .append(logIndex)
                .append(", txId: ")
                .append(txId)
                .append(", contractAddress: ")
                .append(contractAddress)
                .append(", callerAddress: ")
                .append(callerAddress)
                .append(", creatorAddress: ")
                .append(creatorAddress)
                .append(", contractTopics: ")
                .append(contractTopics)
                .append(", data: ")
                .append(data)
                .append(", removed: ")
                .append(removed).toString();
    }
}
