package org.tron.common.logsfilter;

import java.util.ArrayList;
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
  private long fromBlock = EARLIEST_BLOCK_NUM;
  @Getter
  @Setter
  private long toBlock = LATEST_BLOCK_NUM;
  @Getter
  @Setter
  private List<String> contractAddressList = new ArrayList<>();
  @Getter
  @Setter
  private List<String> contractTopicList = new ArrayList<>();

  @Getter
  @Setter
  private String name = "";

  @Getter
  @Setter
  private String disable;

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

  @Override
  public String toString() {
    return new StringBuilder()
        .append("name: ")
        .append(name)
        .append(", fromBlock: ")
        .append(fromBlock)
        .append(", toBlock: ")
        .append(toBlock)
        .append(", contractAddress: ")
        .append(contractAddressList)
        .append(", contractTopics: ")
        .append(contractTopicList)
        .append(", disable: ")
        .append(disable).toString();
  }
}
