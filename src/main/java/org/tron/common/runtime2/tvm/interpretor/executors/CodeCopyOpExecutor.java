package org.tron.common.runtime2.tvm.interpretor.executors;

import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Op;

public class CodeCopyOpExecutor extends OpExecutor {

  private static CodeCopyOpExecutor INSTANCE = new CodeCopyOpExecutor();

  private CodeCopyOpExecutor() {
  }

  public static CodeCopyOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {

    byte[] fullCode = EMPTY_BYTE_ARRAY;
    if (op == Op.CODECOPY) {
      fullCode = executor.getContractContext().getCode();
    } else {
      DataWord address = executor.stackPop();
      fullCode = executor.getCodeAt(address);
    }

    DataWord memOffsetd = executor.stackPop();
    DataWord codeOffsetd = executor.stackPop();
    DataWord lengthDatad = executor.stackPop();

    executor.spendEnergy(
        calcMemEnergy(executor.getMemory().size(),
            memNeeded(memOffsetd, lengthDatad),
            lengthDatad.longValueSafe(), op),
        op.name()
    );
    int memOffset = memOffsetd.intValueSafe();
    int codeOffset = codeOffsetd.intValueSafe();
    int lengthData = lengthDatad.intValueSafe();

    int sizeToBeCopied =
        (long) codeOffset + lengthData > fullCode.length
            ? (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset)
            : lengthData;

    byte[] codeCopy = new byte[lengthData];

    if (codeOffset < fullCode.length) {
      System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);
    }

    executor.memorySave(memOffset, codeCopy);
    executor.step();

  }
}
