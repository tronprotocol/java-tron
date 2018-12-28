package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;

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
    if (matchFilter(contractEventTrigger)){
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

    // add address topic filter here

    return true;
  }
}
