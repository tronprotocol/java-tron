package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * This class is for Emit Event in Solidity.
 * Since Solidity Emit Event is implemented in LOG instruction.
 * The only difference between Basic Log and Emit Event is that the Emit Event has a entry in the contract's ABI.
 * So, if the contract was deployed without ABI, then every events in the contract will be deal as BASIC LOG.
 */
public class ContractEmitEvent {

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
  private Map<String, String> topicMap;

  /**
   * multi data items will be concat into a single string.
   * Each item is 32 bytes.
   */
  @Getter
  @Setter
  private String data;
  
}
