package org.tron.common.runtime2.tvm.interpretor.executors;


import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime.vm.program.Stack;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Op;
import org.tron.common.runtime2.tvm.interpretor.Op.Tier;

public class SwapOpExecutor extends OpExecutor {

  private static SwapOpExecutor INSTANCE = new SwapOpExecutor();

  private SwapOpExecutor() {
  }

  public static SwapOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    executor.spendEnergy(Tier.VeryLowTier.asInt(), op.name());

    Stack stack = executor.getStack();
    int n = op.val() - OpCode.SWAP1.val() + 2;
    stack.swap(stack.size() - 1, stack.size() - n);
    executor.step();
  }
}
