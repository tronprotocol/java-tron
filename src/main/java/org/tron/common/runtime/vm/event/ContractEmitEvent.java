package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * This class is for Emit Event in Solidity.
 */
public class ContractEmitEvent {

  /**
   * decoded signature of the event.
   * decode from sha3($EventSignature) with ABI of this contract. 
   */
  @Getter
  @Setter
  private String eventSignature;

  @Getter
  @Setter
  private Map<String, String> topicMap;

  @Getter
  @Setter
  private String data;
}
