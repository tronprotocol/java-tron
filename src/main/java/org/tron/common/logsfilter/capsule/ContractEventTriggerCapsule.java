package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.ContractEventParser;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.runtime.vm.LogEventWrapper;
import org.tron.protos.Protocol.SmartContract.ABI.Entry;

import java.util.List;

public class ContractEventTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  private List<byte[]> topicList;

  @Getter
  @Setter
  private byte[] data;

  @Getter
  @Setter
  ContractEventTrigger contractEventTrigger;

  @Getter
  @Setter
  private Entry abiEntry;

  public ContractEventTriggerCapsule(LogEventWrapper log) {
    this.contractEventTrigger = new ContractEventTrigger();

    this.contractEventTrigger.setTxId(log.getTxId());
    this.contractEventTrigger.setContractAddress(log.getContractAddress());
    this.contractEventTrigger.setCallerAddress(log.getCallerAddress());
    this.contractEventTrigger.setOriginAddress(log.getOriginAddress());
    this.contractEventTrigger.setCreatorAddress(log.getCreatorAddress());
    this.contractEventTrigger.setBlockNum(log.getBlockNum());
    this.contractEventTrigger.setTimeStamp(log.getTimeStamp());

    this.topicList = log.getTopicList();
    this.data = log.getData();
    this.contractEventTrigger.setEventSignature(log.getEventSignature());
    this.abiEntry = log.getAbiEntry();
  }

  @Override
  public void processTrigger() {
    contractEventTrigger.setTopicMap(ContractEventParser.parseTopics(topicList, abiEntry));
    contractEventTrigger.setDataMap(ContractEventParser.parseEventData(data, topicList, abiEntry));

    if (FilterQuery.matchFilter(contractEventTrigger)) {
      EventPluginLoader.getInstance().postContractEventTrigger(contractEventTrigger);
    }
  }
}
