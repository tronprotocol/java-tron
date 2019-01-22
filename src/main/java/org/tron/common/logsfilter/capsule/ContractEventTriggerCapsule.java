package org.tron.common.logsfilter.capsule;

import java.util.List;
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

  public void setLastestSolidifiedBlockNumber(long lastestSolidifiedBlockNumber) {
    contractEventTrigger.setLastestSolidifiedBlockNumber(lastestSolidifiedBlockNumber);
  }

  public ContractEventTriggerCapsule(LogEventWrapper log) {
    this.contractEventTrigger = new ContractEventTrigger();

    this.contractEventTrigger.setTransactionId(log.getTransactionId());
    this.contractEventTrigger.setContractAddress(log.getContractAddress());
    this.contractEventTrigger.setCallerAddress(log.getCallerAddress());
    this.contractEventTrigger.setOriginAddress(log.getOriginAddress());
    this.contractEventTrigger.setCreatorAddress(log.getCreatorAddress());
    this.contractEventTrigger.setBlockNumber(log.getBlockNumber());
    this.contractEventTrigger.setTimeStamp(log.getTimeStamp());

    this.topicList = log.getTopicList();
    this.data = log.getData();
    this.contractEventTrigger.setEventSignature(log.getEventSignature());
    this.contractEventTrigger.setEventSignatureFull(log.getEventSignatureFull());
    this.contractEventTrigger.setEventName(log.getAbiEntry().getName());
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
