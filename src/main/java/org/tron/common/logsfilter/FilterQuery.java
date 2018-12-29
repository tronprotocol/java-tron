package org.tron.common.logsfilter;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
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
        List<String> topList = filterQuery.getContractAddressList();
        List<String> addressList = filterQuery.getContractAddressList();

        if (blockNumber < fromBlockNumber){
            return false;
        }

        if (toBlockNumber != FilterQuery.LATEST_BLOCK_NUM && blockNumber > toBlockNumber){
            return false;
        }

        String contractAddress = trigger.getContractAddress();
        for (String address: addressList){
            if (contractAddress.equalsIgnoreCase(address)){
                matched = true;
                break;
            }
        }

        if (matched == false) {
            return false;
        }

        Set<String> hset = topList.stream().collect(Collectors.toSet());
        for (String top : ((ContractLogTrigger)trigger).getTopicList()) {
            if (hset.contains(top)) {
                return true;
            }
        }

        return false;
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
