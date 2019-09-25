package org.tron.common.runtime2.tvm.interpretor.executors;


import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Costs;
import org.tron.common.runtime2.tvm.interpretor.Op;

public class CreateOpExecutor extends OpExecutor {

  private static CreateOpExecutor INSTANCE = new CreateOpExecutor();

  private CreateOpExecutor() {
  }

  public static CreateOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    DataWord value = executor.stackPop();
    DataWord inOffset = executor.stackPop();
    DataWord inSize = executor.stackPop();
    DataWord salt = null;
    boolean isCreate2 = false;

    long energyCost =
        Costs.CREATE + calcMemEnergy(executor.getMemory().size(), memNeeded(inOffset, inSize), 0,
            op);

    if (op == Op.CREATE2) {
      isCreate2 = true;
      salt = executor.stackPop();
      energyCost += DataWord.sizeInWords(inSize.intValueSafe()) * Costs.SHA3_WORD;
    }

    executor.spendEnergy(energyCost, op.name());
    executor.createContract(value, inOffset, inSize, salt, isCreate2);
    executor.step();

  }
}
