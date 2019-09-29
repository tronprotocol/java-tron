package org.tron.common.runtime2.tvm;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.*;
import org.tron.common.runtime.vm.program.Stack;
import org.tron.common.runtime2.tvm.interpretor.Op;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.tron.common.crypto.Hash.sha3;
import static org.tron.common.runtime.vm.OpCode.*;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;


@Slf4j(topic = "VM2")
public class Interpreter2 {
  private static final BigInteger _32_ = BigInteger.valueOf(32);
  public static final String ADDRESS_LOG = "address: ";
  private static final String ENERGY_LOG_FORMATE = "{} Op:[{}]  Energy:[{}] Deep:[{}] Hint:[{}]";


  private static class InterpreterInstance {
    private static final Interpreter2 instance = new Interpreter2();
  }

  public static Interpreter2 getInstance() {
    return InterpreterInstance.instance;
  }


  public void play(ContractExecutor env) {
    if (isNotEmpty(env.getContractContext().getOps())) {
      while (!env.isStopped()) {
        this.step(env);
      }
    }
  }


  public void step(ContractExecutor env) {
    try {
      Op op = Op.code(env.getCurrentOp());
      if (op == null) {
        throw org.tron.common.runtime.vm.program.Program.Exception
            .invalidOpCode(env.getCurrentOp());
      }
      env.setLastOp(op.val());
      env.verifyStackSize(op.require());
      //check not exceeding stack limits
      env.verifyStackOverflow(op.require(), op.ret());
      //spend energy
      //checkcpu limit
      env.checkCPUTimeLimit(op.name());
      //step
      op.getOpExecutor().exec(op,env);
      env.setPreviouslyExecutedOp(op.val());
      String hint = "exec:"+op.name()+" stack:"+env.getStack().size()+" mem:"+env.getMemory().size()+" pc:"+env.getPC()+" stacktop:"+env.getStack().safepeek()+" ene:"+env.getContractContext().getProgramResult().getEnergyUsed();
      env.getContractContext().addOpHistory(hint);

    } catch (RuntimeException e) {
      logger.info("VM halted: [{}]", e.getMessage());
      if (!(e instanceof org.tron.common.runtime.vm.program.Program.TransferException)) {
        env.spendAllEnergy();
      }
      env.resetFutureRefund();
      env.stop();
      throw e;
    }
  }



}
