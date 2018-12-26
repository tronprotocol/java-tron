package org.tron.common.logsfilter.trigger;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class ContractEventTrigger extends Trigger{
  @Getter
  @Setter
  private String eventType;

  @Getter
  @Setter
  private long blockNum;

  @Getter
  @Setter
  private long blockTimestamp;

  @Getter
  @Setter
  private String trxHash;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private long logIndex;

  @Getter
  @Setter
  private long txId;

  @Getter
  @Setter
  private String contractAddress;

  @Getter
  @Setter
  private String callerAddress;

  @Getter
  @Setter
  private String creatorAddress;

  @Getter
  @Setter
  private String eventSignature;

  @Getter
  @Setter
  private String data;

  @Getter
  @Setter
  private boolean removed;

  @Getter
  @Setter
  private Map<String, String> contractTopicMap;
}
