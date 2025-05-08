package org.tron.core.services.event.bo;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;

public class SmartContractTrigger {
  @Getter
  @Setter
  private List<ContractLogTrigger> contractLogTriggers = new ArrayList<>();
  @Getter
  @Setter
  private List<ContractEventTrigger> contractEventTriggers = new ArrayList<>();
  @Getter
  @Setter
  private List<ContractLogTrigger> redundancies = new ArrayList<>();
}
