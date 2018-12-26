package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;

import java.util.Objects;

public class ContractLogTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  ContractLogTrigger contractLogTrigger;

  public ContractLogTriggerCapsule(ContractLogTrigger contractLogTrigger) {
    this.contractLogTrigger = contractLogTrigger;
  }

  @Override
  public void processTrigger(){
    EventPluginLoader.getInstance().postContractLogTrigger(contractLogTrigger);
  }
}
