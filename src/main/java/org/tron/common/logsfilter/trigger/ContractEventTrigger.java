package org.tron.common.logsfilter.trigger;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class ContractEventTrigger extends ContractTrigger{
  /**
   * decode from sha3($EventSignature) with the ABI of this contract.
   */
  @Getter
  @Setter
  private String eventSignature;

  /**
   * decode from topicList with the ABI of this contract.
   */
  @Getter
  @Setter
  private Map<String, Object> topicMap;

  /**
   * multi data items will be concat into a single string.
   * Each item is 32 bytes.
   */
  @Getter
  @Setter
  private Map<String, Object> dataMap;


  public ContractEventTrigger(String txId, String contractAddress, String callerAddress,
                              String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp){
    super(txId, contractAddress, callerAddress, originAddress, creatorAddress, blockNum, blockTimestamp);
  }
}
