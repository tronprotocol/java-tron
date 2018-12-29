package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;
import org.pf4j.util.StringUtils;
import org.tron.common.logsfilter.trigger.ContractTrigger;

import java.util.List;
import java.util.Objects;

public class FilterQuery {
    @Getter
    @Setter
    private long fromBlock;

    @Getter
    @Setter
    private long toBlock;

    @Getter
    @Setter
    private List<String> contractAddressList;

    @Getter
    @Setter
    private List<String> contractTopicList;

    public static final int EARLIEST_BLOCK_NUM = 0;
    public static final int LATEST_BLOCK_NUM = -1;

    public static final String EARLIEST = "earliest";
    public static final String LATEST = "latest";

    public static boolean matchFilter(ContractTrigger trigger){
        boolean matched = false;

        long blockNumber = trigger.getBlockNum();

        FilterQuery filterQuery = EventPluginLoader.getInstance().getFilterQuery();
        if (Objects.isNull(filterQuery)){
            return true;
        }

        long fromBlockNumber = filterQuery.getFromBlock();
        long toBlockNumber = filterQuery.getToBlock();
        List<String> addressList = filterQuery.getContractAddressList();

        if (blockNumber <= fromBlockNumber){
            return matched;
        }

        if (toBlockNumber != FilterQuery.LATEST_BLOCK_NUM && blockNumber > toBlockNumber){
            return matched;
        }

        String contractAddress = trigger.getContractAddress();
        for (String address: addressList){
            if (contractAddress.equalsIgnoreCase(address)){
                matched = true;
                break;
            }
        }

        // add topic filter here

        return true;
    }

    @Override
    public String toString(){
        return new StringBuilder().append("fromBlock: ")
                .append(fromBlock)
                .append(", toBlock: ")
                .append(toBlock)
                .append(", contractAddress: ")
                .append(contractAddressList)
                .append(", contractTopics: ")
                .append(contractTopicList).toString();
    }
}
