package org.tron.common.logsfilter.trigger;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class ContractEventTrigger extends ContractLogTrigger{
  /**
   * decode from sha3($EventSignature) with the ABI of this contract.
   */
  @Getter
  @Setter
  private String eventSignature;

  /**
   * decode from topicList with the ABI of this contract.
   * this item is null if not called ContractEventParser::parseTopics(ContractEventTrigger trigger)
   */
  @Getter
  @Setter
  private Map<String, Object> topicMap;

  /**
   * multi data items will be concat into a single string.
   * this item is null if not called ContractEventParser::parseData(ContractEventTrigger trigger)
   */
  @Getter
  @Setter
  private Map<String, Object> dataMap;


  public ContractEventTrigger(String txId, String contractAddress, String callerAddress,
                              String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp){
    super(txId, contractAddress, callerAddress, originAddress, creatorAddress, blockNum, blockTimestamp);
    setTriggerName(Trigger.CONTRACTEVENT_TRIGGER_NAME);
  }

  @Override
  public String toString(){
    return new StringBuilder().append("timestamp: ")
            .append(timeStamp)
            .append(", blockNum: ")
            .append(getBlockNum())
            .append(", blockTimestamp: ")
            .append(getTimeStamp())
            .append(", txId: ")
            .append(getTxId())
            .append(", contractAddress: ")
            .append(getContractAddress())
            .append(", callerAddress: ")
            .append(getCallerAddress())
            .append(", creatorAddress: ")
            .append(getCallerAddress())
            .append(", eventSignature: ")
            .append(eventSignature)
            .append(", data: ")
            .append(dataMap)
            .append(", contractTopicMap")
            .append(topicMap)
            .append(", removed: ")
            .append(isRemoved()).toString();
  }
}
