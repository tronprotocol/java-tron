package org.tron.common.logsfilter.trigger;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class ContractLogTrigger extends ContractTrigger {

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

  public ContractLogTrigger() {
    super();
    setTriggerName(Trigger.CONTRACTLOG_TRIGGER_NAME);
  }
}
