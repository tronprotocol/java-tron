package org.tron.common.logsfilter.capsule;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;

public class TransactionLogTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  TransactionLogTrigger transactionLogTrigger;

  public TransactionLogTriggerCapsule(TransactionCapsule trxCasule, BlockCapsule blockCapsule) {
    transactionLogTrigger = new TransactionLogTrigger();
    if (Objects.nonNull(blockCapsule)) {
      transactionLogTrigger.setBlockId(blockCapsule.getBlockId().toString());
    }
    transactionLogTrigger.setTransactionId(trxCasule.getTransactionId().toString());
    transactionLogTrigger.setTimestamp(trxCasule.getTimestamp());
  }

  @Override
  public void processTrigger(){
    EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
  }
}
