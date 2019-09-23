package org.tron.common.runtime2.tvm.interpretor.executors;


import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Op;
import org.tron.common.runtime2.tvm.interpretor.Op.Tier;

public class DupOpExecutor extends OpExecutor {

  private static DupOpExecutor INSTANCE = new DupOpExecutor();

  private DupOpExecutor() {
  }

  public static DupOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    executor.spendEnergy(Tier.VeryLowTier.asInt(), op.name());

    int n = op.val() - OpCode.DUP1.val() + 1;
    DataWord word1 = executor.getStack().get(executor.getStack().size() - n);
    executor.stackPush(word1.clone());
    executor.step();

  }
}
