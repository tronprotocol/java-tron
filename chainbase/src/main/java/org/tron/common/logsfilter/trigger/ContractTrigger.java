package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.capsule.RawData;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

public class ContractTrigger extends Trigger {

  /**
   * unique id of this trigger. $tx_id + "_" + $index
   */
  @Getter
  @Setter
  private String uniqueId;

  /**
   * id of the transaction which produce this event.
   */
  @Getter
  @Setter
  private String transactionId;

  /**
   * address of the contract triggered by the callerAddress.
   */
  @Getter
  @Setter
  private String contractAddress;

  /**
   * caller of the transaction which produce this event.
   */
  @Getter
  @Setter
  private String callerAddress;

  /**
   * origin address of the contract which produce this event.
   */
  @Getter
  @Setter
  private String originAddress;

  /**
   * caller address of the contract which produce this event.
   */
  @Getter
  @Setter
  private String creatorAddress;

  /**
   * block number of the transaction
   */
  @Getter
  @Setter
  private Long blockNumber;

  /**
   * true if the transaction has been revoked
   */
  @Getter
  @Setter
  private boolean removed;

  @Getter
  @Setter
  private long latestSolidifiedBlockNumber;

  @Getter
  @Setter
  private LogInfo logInfo;

  @Getter
  @Setter
  private RawData rawData;

  @Getter
  @Setter
  private ABI abi;
}
