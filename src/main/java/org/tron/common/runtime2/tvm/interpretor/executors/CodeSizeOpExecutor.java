package org.tron.common.runtime2.tvm.interpretor.executors;

import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime.vm.OpCode.Tier;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Op;

public class CodeSizeOpExecutor extends OpExecutor {

  private static CodeSizeOpExecutor INSTANCE = new CodeSizeOpExecutor();

  private CodeSizeOpExecutor() {
  }

  public static CodeSizeOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    executor.spendEnergy(op == Op.CODESIZE ? OpCode.Tier.BaseTier.asInt() : Tier.ExtTier.asInt(),
        op.name());

    int length;
    if (op == Op.CODESIZE) {
      length = executor.getContractContext().getCode().length;
    } else {
      DataWord address = executor.stackPop();
      length = executor.getCodeAt(address).length;
    }
    DataWord codeLength = new DataWord(length);

    executor.stackPush(codeLength);
    executor.step();

  }
}
