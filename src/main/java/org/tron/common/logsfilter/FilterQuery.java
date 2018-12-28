package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class FilterQuery {
    @Getter
    @Setter
    private long fromBlock;

    @Getter
    @Setter
    private long toBlock;

    @Getter
    @Setter
    private List<String> contractAddress;

    @Getter
    @Setter
    private List<String> contractTopics;

    public static final int EARLIEST_BLOCK_NUM = 0;
    public static final int LATEST_BLOCK_NUM = -1;

    public static final String EARLIEST = "earliest";
    public static final String LATEST = "latest";

    @Override
    public String toString(){
        return new StringBuilder().append("fromBlock: ")
                .append(fromBlock)
                .append(", toBlock: ")
                .append(toBlock)
                .append(", contractAddress: ")
                .append(contractAddress)
                .append(", contractTopics: ")
                .append(contractTopics).toString();
    }
}
