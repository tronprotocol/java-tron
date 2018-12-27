package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.ContractEventParser;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.runtime.vm.LogEventWrapper;
import org.tron.protos.Protocol.SmartContract.ABI.Entry;

import java.util.Objects;

public class ContractEventTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  ContractEventTrigger contractEventTrigger;

  @Getter
  @Setter
  private Entry abiEntry;

  public ContractEventTriggerCapsule(LogEventWrapper log) {
    this.contractEventTrigger = new ContractEventTrigger(
        log.getTxId(), log.getContractAddress(), log.getCallerAddress(),
        log.getOriginAddress(), log.getCreatorAddress(), log.getBlockNum(), log.getBlockTimestamp());
    this.abiEntry = log.getAbiEntry();
  }

  @Override
  public void processTrigger(){
    contractEventTrigger.setDataMap(ContractEventParser.parseEventData(contractEventTrigger, abiEntry));
    contractEventTrigger.setTopicMap(ContractEventParser.parseTopics(contractEventTrigger, abiEntry));
    EventPluginLoader.getInstance().postContractEventTrigger(contractEventTrigger);
  }
}
