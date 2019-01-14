package org.tron.common.logsfilter.trigger;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class TransactionLogTrigger extends Trigger {

  @Override
  public void setTimeStamp(long ts) {
    super.timeStamp = ts;
  }

  @Getter
  @Setter
  private String transactionId;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private long blockNumber = -1;

  @Getter
  @Setter
  private long energyUsage;

  @Getter
  @Setter
  private long energyFee;

  @Getter
  @Setter
  private long originEnergyUsage;

  @Getter
  @Setter
  private long energyUsageTotal;

  @Getter
  @Setter
  private long netUsage;

  @Getter
  @Setter
  private  long netFee;

  @Getter
  @Setter
  private List<InternalTransactionPogo> internalTransactionPogos;

  public TransactionLogTrigger() {
    setTriggerName(Trigger.TRANSACTION_TRIGGER_NAME);
  }

  @Override
  public String toString() {
    return new StringBuilder().append("triggerName: ")
      .append(getTriggerName())
      .append("timestamp: ")
      .append(timeStamp)
      .append(", transactionId: ")
      .append(transactionId)
      .append(", blockHash: ")
      .append(blockHash)
      .append(", blockNumber: ")
      .append(blockNumber)
      .append(", energyUsage: ")
      .append(energyUsage)
      .append(", energyFee: ")
      .append(energyFee)
      .append(", originEnergyUsage: ")
      .append(originEnergyUsage)
      .append(", energyUsageTotal: ")
      .append(energyUsageTotal)
      .append(", netUsage: ")
      .append(netUsage)
      .append(", netFee: ")
      .append(netFee).toString();
  }
}
