package org.tron.core.services.event.bo;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.tron.common.logsfilter.capsule.SolidityTriggerCapsule;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.core.capsule.BlockCapsule;

public class BlockEvent {
  @Getter
  @Setter
  private BlockCapsule.BlockId blockId;
  @Getter
  @Setter
  private BlockCapsule.BlockId parentId;
  @Getter
  @Setter
  private BlockCapsule.BlockId solidId;
  @Getter
  @Setter
  private BlockLogTriggerCapsule blockLogTriggerCapsule;
  @Getter
  @Setter
  private List<TransactionLogTriggerCapsule> transactionLogTriggerCapsules;
  @Getter
  @Setter
  private SolidityTriggerCapsule solidityTriggerCapsule;
  @Getter
  @Setter
  private SmartContractTrigger smartContractTrigger;

  public BlockEvent() {}

  public BlockEvent(BlockCapsule.BlockId blockId) {
    this.blockId = blockId;
  }
}

