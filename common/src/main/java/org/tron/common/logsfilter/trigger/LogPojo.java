package org.tron.common.logsfilter.trigger;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class LogPojo {
  @Getter
  @Setter
  private String address;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private long blockNumber;

  @Getter
  @Setter
  private String data;

  @Getter
  @Setter
  private long logIndex;

  @Getter
  @Setter
  private List<String> topicList;

  @Getter
  @Setter
  private String transactionHash;

  @Getter
  @Setter
  private long transactionIndex;
}
