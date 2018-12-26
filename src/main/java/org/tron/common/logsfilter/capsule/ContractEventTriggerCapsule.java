package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;

import java.util.Objects;

public class ContractEventTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  ContractEventTrigger contractEventTrigger;

  public ContractEventTriggerCapsule(ContractEventTrigger contractEventTrigger) {
    this.contractEventTrigger = contractEventTrigger;
  }

  @Override
  public void processTrigger(){
    EventPluginLoader.getInstance().postContractEventTrigger(contractEventTrigger);
  }
}
