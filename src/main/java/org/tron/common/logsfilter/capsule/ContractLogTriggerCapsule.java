package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.FilterQuery;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;

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
    if (matchFilter(contractLogTrigger)){
        EventPluginLoader.getInstance().postContractLogTrigger(contractLogTrigger);
    }
  }


  private boolean matchFilter(ContractLogTrigger contractLogTrigger){
    boolean matched = false;

    long blockNumber = contractLogTrigger.getBlockNum();

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
