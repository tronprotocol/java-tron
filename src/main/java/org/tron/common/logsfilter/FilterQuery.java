package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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


  public static long parseFromBlockNumber(String blockNum) {
    long number = 0;
    if (StringUtils.isEmpty(blockNum) || FilterQuery.EARLIEST.equalsIgnoreCase(blockNum)) {
      number = FilterQuery.EARLIEST_BLOCK_NUM;
    } else {
      try {
        number = Long.parseLong(blockNum);
      } catch (Exception e) {
        logger.error("invalid filter: fromBlockNumber: {}", blockNum);
        throw e;
      }
    }
    return number;
  }

  public static long parseToBlockNumber(String blockNum) {
    long number = 0;
    if (StringUtils.isEmpty(blockNum) || FilterQuery.LATEST.equalsIgnoreCase(blockNum)) {
      number = FilterQuery.LATEST_BLOCK_NUM;
    } else {
      try {
        number = Long.parseLong(blockNum);
      } catch (Exception e) {
        logger.error("invalid filter: toBlockNumber: {}", blockNum);
        throw e;
      }
    }
    return number;
  }

  public static boolean matchFilter(ContractTrigger trigger) {
    long blockNumber = trigger.getBlockNumber();

    FilterQuery filterQuery = EventPluginLoader.getInstance().getFilterQuery();
    if (Objects.isNull(filterQuery)) {
      return true;
    }

    long fromBlockNumber = filterQuery.getFromBlock();
    long toBlockNumber = filterQuery.getToBlock();

    boolean matched = false;
    if (fromBlockNumber == FilterQuery.LATEST_BLOCK_NUM
        || toBlockNumber == FilterQuery.EARLIEST_BLOCK_NUM) {
      logger.error("invalid filter: fromBlockNumber: {}, toBlockNumber: {}",
          fromBlockNumber, toBlockNumber);
      return false;
    }

    if (toBlockNumber == FilterQuery.LATEST_BLOCK_NUM) {
      if (fromBlockNumber == FilterQuery.EARLIEST_BLOCK_NUM) {
        matched = true;
      } else {
        if (blockNumber >= fromBlockNumber) {
          matched = true;
        }
      }
    } else {
      if (fromBlockNumber == FilterQuery.EARLIEST_BLOCK_NUM) {
        if (blockNumber <= toBlockNumber) {
          matched = true;
        }
      } else {
        if (blockNumber >= fromBlockNumber && blockNumber <= toBlockNumber) {
          matched = true;
        }
      }
    }

    if (!matched) {
      return false;
    }

    return filterContractAddress(trigger, filterQuery.getContractAddressList())
        && filterContractTopicList(trigger, filterQuery.getContractTopicList());
  }

  private static boolean filterContractAddress(ContractTrigger trigger, List<String> addressList) {
    addressList = addressList.stream().filter(item -> StringUtils.isNotEmpty(item))
        .collect(Collectors.toList());
    if (Objects.isNull(addressList) || addressList.isEmpty()) {
      return true;
    }

    String contractAddress = trigger.getContractAddress();
    if (Objects.isNull(contractAddress)) {
      return false;
    }

    for (String address : addressList) {
      if (contractAddress.equalsIgnoreCase(address)) {
        return true;
      }
    }
    return false;
  }

  private static boolean filterContractTopicList(ContractTrigger trigger, List<String> topList) {
    topList = topList.stream().filter(item -> StringUtils.isNotEmpty(item))
        .collect(Collectors.toList());
    if (Objects.isNull(topList) || topList.isEmpty()) {
      return true;
    }

    Set<String> hset = null;
    if (trigger instanceof ContractLogTrigger) {
      hset = ((ContractLogTrigger) trigger).getTopicList().stream().collect(Collectors.toSet());
    } else {
      hset = new HashSet<>(((ContractEventTrigger) trigger).getTopicMap().values());
    }

    for (String top : topList) {
      if (hset.contains(top)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
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
