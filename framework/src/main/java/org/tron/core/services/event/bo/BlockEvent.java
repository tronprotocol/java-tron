package org.tron.core.services.event.bo;

import java.util.List;
import lombok.Data;
import org.tron.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.SolidityTriggerCapsule;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.core.capsule.BlockCapsule;

@Data
public class BlockEvent {
  private BlockCapsule.BlockId blockId;
  private BlockCapsule.BlockId parentId;
  private BlockCapsule.BlockId solidId;
  private long blockTime;

  private BlockLogTriggerCapsule blockLogTriggerCapsule;
  private List<TransactionLogTriggerCapsule> transactionLogTriggerCapsules;
  private SolidityTriggerCapsule solidityTriggerCapsule;
  private SmartContractTrigger smartContractTrigger;

  public BlockEvent() {}

  public BlockEvent(BlockCapsule.BlockId blockId) {
    this.blockId = blockId;
  }
}

