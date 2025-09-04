package org.tron.common.logsfilter;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FilterQuery {

  public static final int EARLIEST_BLOCK_NUM = 0;
  public static final int LATEST_BLOCK_NUM = -1;
  public static final String EARLIEST = "earliest";
  public static final String LATEST = "latest";
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

  public static long parseFromBlockNumber(String blockNum) {
    long number = 0;
    if (StringUtils.isEmpty(blockNum) || FilterQuery.EARLIEST.equalsIgnoreCase(blockNum)) {
      number = FilterQuery.EARLIEST_BLOCK_NUM;
    } else {
      number = Long.parseLong(blockNum);
    }
    return number;
  }

  public static long parseToBlockNumber(String blockNum) {
    long number = 0;
    if (StringUtils.isEmpty(blockNum) || FilterQuery.LATEST.equalsIgnoreCase(blockNum)) {
      number = FilterQuery.LATEST_BLOCK_NUM;
    } else {
      number = Long.parseLong(blockNum);
    }
    return number;
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
