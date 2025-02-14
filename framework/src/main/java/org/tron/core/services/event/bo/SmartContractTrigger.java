package org.tron.core.services.event.bo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;

@Data
public class SmartContractTrigger {
  private List<ContractLogTrigger> contractLogTriggers = new ArrayList<>();
  private List<ContractEventTrigger> contractEventTriggers = new ArrayList<>();
  private List<ContractLogTrigger> redundancies = new ArrayList<>();
}
