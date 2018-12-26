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

  @Override
  public String toString(){
    return new StringBuilder().append("timestamp: ")
            .append(timeStamp)
            .append(", eventType: ")
            .append(eventType)
            .append(", blockNum: ")
            .append(blockNum)
            .append(", blockTimestamp: ")
            .append(blockTimestamp)
            .append(", trxHash: ")
            .append(trxHash)
            .append(", blockHash: ")
            .append(blockHash)
            .append(", logIndex: ")
            .append(logIndex)
            .append(", txId: ")
            .append(txId)
            .append(", contractAddress: ")
            .append(contractAddress)
            .append(", callerAddress: ")
            .append(callerAddress)
            .append(", creatorAddress: ")
            .append(creatorAddress)
            .append(", eventSignature: ")
            .append(eventSignature)
            .append(", data: ")
            .append(data)
            .append(", contractTopicMap")
            .append(contractTopicMap)
            .append(", removed: ")
            .append(removed).toString();
  }
}
