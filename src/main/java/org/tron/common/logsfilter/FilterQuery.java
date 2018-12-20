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
    List<String> contractTopics;
}
