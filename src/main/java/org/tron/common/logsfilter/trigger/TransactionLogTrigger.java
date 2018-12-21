package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

public class TransactionLogTrigger {

    @Getter
    @Setter
    private long timestamp;

    @Getter
    @Setter
    private long transactionId;

    @Getter
    @Setter
    private String transactionHash;

    @Getter
    @Setter
    private long blockId;
}
