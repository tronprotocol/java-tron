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
