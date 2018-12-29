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
   * this item is null if not called ContractEventParser::parseTopics(ContractEventTrigger trigger)
   */
  @Getter
  @Setter
  private Map<String, String> topicMap;

  /**
   * multi data items will be concat into a single string.
   * this item is null if not called ContractEventParser::parseData(ContractEventTrigger trigger)
   */
  @Getter
  @Setter
  private Map<String, String> dataMap;


  public ContractEventTrigger(){
    super();
    setTriggerName(Trigger.CONTRACTEVENT_TRIGGER_NAME);
  }
}
