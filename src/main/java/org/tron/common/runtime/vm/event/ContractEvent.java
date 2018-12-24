package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

public class ContractEvent {

  public enum  EventType {
    BASIC_LOG,           // Successfully generated block
    EMIT_EVENT,
  }

  /**
   * Contract Event Type, Current Support Solidity Basic Log & Solidity Emit Event
   */
  @Getter
  @Setter
  private EventType type;

  /**
   * id of the transaction which produce this event.
   */
  @Getter
  @Setter
  private String txId;

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
  private Long blockNum;

  /**
   * block timestamp of the transaction
   */
  @Getter
  @Setter
  private Long blockTimestamp;

}
