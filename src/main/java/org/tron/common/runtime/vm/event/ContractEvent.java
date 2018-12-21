package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

public class ContractEvent {

  enum  EventType {
    BASIC_LOG,           // Successfully generated block
    EMIT_EVENT,
  }

  @Getter
  @Setter
  private String txId;

  @Getter
  @Setter
  private String contractAddress;

  @Getter
  @Setter
  private String callerAddress;

  @Getter
  @Setter
  private String creatorAddress;

  @Getter
  @Setter
  private Long blockNum;

  @Getter
  @Setter
  private Long blockTimestamp;
}
