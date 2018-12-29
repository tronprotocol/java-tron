package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.ContractEventParser;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.runtime.vm.LogEventWrapper;
import org.tron.protos.Protocol.SmartContract.ABI.Entry;

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
        log.getOriginAddress(), log.getCreatorAddress(), log.getBlockNum(), log.getTimeStamp());
    this.contractEventTrigger.setTopicList(log.getTopicList());
    this.contractEventTrigger.setData(log.getData());
    this.contractEventTrigger.setEventSignature(log.getEventSignature());
    this.abiEntry = log.getAbiEntry();
  }

  @Override
  public void processTrigger(){
    contractEventTrigger.setTopicMap(ContractEventParser.parseTopics(contractEventTrigger, abiEntry));
    contractEventTrigger.setDataMap(ContractEventParser.parseEventData(contractEventTrigger, abiEntry));

    if (FilterQuery.matchFilter(contractEventTrigger)){
      EventPluginLoader.getInstance().postContractEventTrigger(contractEventTrigger);
    }
  }
}
