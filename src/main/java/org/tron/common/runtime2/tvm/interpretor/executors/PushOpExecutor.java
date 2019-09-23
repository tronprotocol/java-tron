package org.tron.common.runtime2.tvm.interpretor.executors;


import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Op;
import org.tron.common.runtime2.tvm.interpretor.Op.Tier;

public class PushOpExecutor extends OpExecutor {

  private static PushOpExecutor INSTANCE = new PushOpExecutor();

  private PushOpExecutor() {
  }

  public static PushOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    executor.spendEnergy(Tier.VeryLowTier.asInt(), op.name());

    executor.step();
    int nPush = op.val() - Op.PUSH1.val() + 1;
    byte[] data = executor.sweep(nPush);

    executor.stackPush(data);

  }
}
