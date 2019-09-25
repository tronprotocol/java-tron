package org.tron.common.runtime2.tvm.interpretor.executors;


import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Costs;
import org.tron.common.runtime2.tvm.interpretor.Op;

public class RetOpExecutor extends OpExecutor {

  private static RetOpExecutor INSTANCE = new RetOpExecutor();

  private RetOpExecutor() {
  }

  public static RetOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {

    DataWord offset = executor.stackPop();
    DataWord size = executor.stackPop();
    long energyCost = Costs.STOP + calcMemEnergy(executor.getMemory().size(),
        memNeeded(offset, size), 0, op);
    executor.spendEnergy(energyCost, op.name());
    byte[] hReturn = executor.memoryChunk(offset.intValueSafe(), size.intValueSafe());
    executor.getContractContext().getProgramResult().setHReturn(hReturn);
    executor.step();
    executor.stop();

    if (op == Op.REVERT) {
      executor.getContractContext().getProgramResult().setRevert();
    }

  }
}
