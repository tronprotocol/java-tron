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
}
