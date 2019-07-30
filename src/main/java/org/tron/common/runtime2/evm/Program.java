package org.tron.common.runtime2.evm;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.core.capsule.AccountCapsule;

@Slf4j(topic = "VM2")
public class Program {
  @Getter
  @Setter
  private long energyLimit;
  @Getter
  @Setter
  private long vmStartInUs;
  @Getter
  @Setter
  private long vmShouldEndInUs;
  @Getter
  @Setter
  long callValue = 0;
  @Getter
  @Setter
  long tokenValue = 0;
  @Getter
  @Setter
  long tokenId = 0;
  @Getter
  @Setter
  AccountCapsule creator;
  @Getter
  @Setter
  AccountCapsule caller;//for new contract
  @Getter
  @Setter
  byte[] callerAddress;
  @Getter
  @Setter
  private byte[] ops;
  @Getter
  @Setter
  private byte[] rootTransactionId;
  @Getter
  @Setter
  private InternalTransaction internalTransaction;
}
