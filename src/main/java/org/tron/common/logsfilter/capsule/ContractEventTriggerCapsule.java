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
import java.util.Objects;

public class ContractEventTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  ContractEventTrigger contractEventTrigger;

  @Getter
  @Setter
  private Entry abiEntry;

  @Getter
  @Setter
  private List<byte[]> topicList;

  @Getter
  @Setter
  private byte[] data;

  public ContractEventTriggerCapsule(LogEventWrapper log) {
    this.topicList = log.getTopicList();
    this.data = log.getData();
    this.contractEventTrigger = new ContractEventTrigger(
        log.getTxId(), log.getContractAddress(), log.getCallerAddress(),
        log.getOriginAddress(), log.getCreatorAddress(), log.getBlockNum(), log.getTimeStamp());
    this.contractEventTrigger.setEventSignature(log.getEventSignature());
    this.abiEntry = log.getAbiEntry();
  }

  @Override
  public void processTrigger(){
    if (matchFilter(contractEventTrigger)){
      contractEventTrigger.setTopicMap(ContractEventParser.parseTopics(topicList, abiEntry));
      contractEventTrigger.setDataMap(ContractEventParser.parseEventData(topicList, data, abiEntry));
      EventPluginLoader.getInstance().postContractEventTrigger(contractEventTrigger);
    }
  }

  private boolean matchFilter(ContractEventTrigger contractEventTrigger){
    boolean matched = false;

    long blockNumber = contractEventTrigger.getBlockNum();

    FilterQuery filterQuery = EventPluginLoader.getInstance().getFilterQuery();
    if (Objects.isNull(filterQuery)){
      return true;
    }

    long fromBlockNumber = filterQuery.getFromBlock();
    long toBlockNumber = filterQuery.getToBlock();

    if (blockNumber <= fromBlockNumber){
      return matched;
    }

    if (toBlockNumber != FilterQuery.LATEST_BLOCK_NUM && blockNumber > toBlockNumber){
      return matched;
    }

    return true;
    
  }
}
