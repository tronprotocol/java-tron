package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ContractLogEvent extends ContractEvent {

  /**
   * topic list produced by the smart contract LOG function
   */
  @Getter
  @Setter
  private List<String> topicList;

  /**
   * data produced by the smart contract LOG function
   */
  @Getter
  @Setter
  private String data;

}
