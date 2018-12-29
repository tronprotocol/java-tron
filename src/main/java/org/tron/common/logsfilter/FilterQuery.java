package org.tron.common.logsfilter;

import com.google.common.collect.Streams;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
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


  public static long parseFilterQueryBlockNumber(String blockNum) {
    long number = 0;
    if (StringUtils.isEmpty(blockNum) || FilterQuery.LATEST.equalsIgnoreCase(blockNum)){
      number =  FilterQuery.LATEST_BLOCK_NUM;
    }
    else if (FilterQuery.EARLIEST.equalsIgnoreCase(blockNum)){
      number = FilterQuery.EARLIEST_BLOCK_NUM;
    }
    else {
      try {
        number =  Long.parseLong(blockNum);
      } catch (Exception e) {
        throw e;
      }
    }
    return number;
  }

    public static boolean matchFilter(ContractTrigger trigger){
        long blockNumber = trigger.getBlockNum();

        FilterQuery filterQuery = EventPluginLoader.getInstance().getFilterQuery();
        if (Objects.isNull(filterQuery)){
            return true;
        }

        long fromBlockNumber = filterQuery.getFromBlock();
        long toBlockNumber = filterQuery.getToBlock();

        if (blockNumber < fromBlockNumber){
            return false;
        }

        if (toBlockNumber != FilterQuery.LATEST_BLOCK_NUM && blockNumber > toBlockNumber){
            return false;
        }

        return filterContractAddress(trigger, filterQuery.getContractAddressList())
          && filterContractTopicList(trigger, filterQuery.getContractTopicList());
    }

    private static boolean filterContractAddress(ContractTrigger trigger, List<String> addressList) {
        if (addressList.isEmpty()) return true;

        String contractAddress = trigger.getContractAddress();
        for (String address: addressList){
            if (contractAddress.equalsIgnoreCase(address)){
                return true;
            }
        }
        return false;
    }

    private static boolean filterContractTopicList(ContractTrigger trigger, List<String> topList) {
        if (topList.isEmpty()) return true;
        Set<String> hset = null;
        if (trigger instanceof ContractLogTrigger) {
            hset = ((ContractLogTrigger) trigger).getTopicList().stream().collect(Collectors.toSet());
        } else {
            hset = new HashSet<>(((ContractEventTrigger)trigger).getTopicMap().values());
        }

        for (String top : topList) {
            if (hset.contains(top)){
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
