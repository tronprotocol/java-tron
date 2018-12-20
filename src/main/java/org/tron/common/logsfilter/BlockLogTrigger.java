package org.tron.common.logsfilter;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class BlockLogTrigger {
    @Getter
    @Setter
    private long timeStamp;

    @Getter
    @Setter
    private long blockNumber;

    @Getter
    @Setter
    private String blockHash;

    @Getter
    @Setter
    private String transactionSize;

    @Getter
    @Setter
    private List<String> transactionList;

}
