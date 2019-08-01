package org.tron.common.runtime2.tvm;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime2.config.VMConfig;
import org.tron.core.capsule.AccountCapsule;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

@Slf4j(topic = "VM2")
@Data
public class Program {

  InternalTransaction.TrxType trxType;

  private boolean isStatic;

  private byte[] contractAddress;

  private long energyLimit;

  private long vmStartInUs;

  private long vmShouldEndInUs;

  long callValue = 0;

  long tokenValue = 0;

  long tokenId = 0;

  AccountCapsule creator;

  AccountCapsule caller;

  byte[] callerAddress;

  private byte[] ops;

  private byte[] rootTransactionId;

  private InternalTransaction internalTransaction;

  ProgramResult programResult = new ProgramResult();

  private VMConfig vmConfig;

  public void setOps(byte[] ops) {
    this.ops = nullToEmpty(ops);
  }
}
