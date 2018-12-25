package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.BlockLogTrigger;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.common.logsfilter.trigger.Trigger;
import org.tron.core.capsule.BlockCapsule;

public class BlockLogTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  BlockLogTrigger blockLogTrigger;

  public BlockLogTriggerCapsule(BlockCapsule block) {
    blockLogTrigger = new BlockLogTrigger();
    blockLogTrigger.setBlockHash(block.getBlockId().toString());
    blockLogTrigger.setTimeStamp(System.currentTimeMillis());
    blockLogTrigger.setBlockNumber(block.getNum());
  }

  @Override
  public void processTrigger(){
    EventPluginLoader.getInstance().postBlockTrigger( blockLogTrigger);
  }
}
