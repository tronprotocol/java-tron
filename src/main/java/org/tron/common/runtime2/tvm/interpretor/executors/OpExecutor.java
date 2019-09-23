package org.tron.common.runtime2.tvm.interpretor.executors;

import java.math.BigInteger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.program.Program.OutOfMemoryException;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.VMConstant;
import org.tron.common.runtime2.tvm.interpretor.Costs;
import org.tron.common.runtime2.tvm.interpretor.Op;

public abstract class OpExecutor {

  public void exec(Op op, ContractExecutor executor) {
  }

  ;

  public long calcMemEnergy(long oldMemSize, BigInteger newMemSize,
      long copySize, Op op) {
    long energyCost = 0;

    checkMemorySize(op, newMemSize);

    // memory drop consume calc
    long memoryUsage = (newMemSize.longValueExact() + 31) / 32 * 32;
    if (memoryUsage > oldMemSize) {
      long memWords = (memoryUsage / 32);
      long memWordsOld = (oldMemSize / 32);
      long memEnergy = (Costs.MEMORY * memWords + memWords * memWords / 512)
          - (Costs.MEMORY * memWordsOld + memWordsOld * memWordsOld / 512);
      energyCost += memEnergy;
    }

    if (copySize > 0) {
      long copyEnergy = Costs.COPY_ENERGY * ((copySize + 31) / 32);
      energyCost += copyEnergy;
    }
    return energyCost;
  }

  protected void checkMemorySize(Op op, BigInteger newMemSize) {
    if (newMemSize.compareTo(VMConstant.MEM_LIMIT) > 0) {
      throw memoryOverflow(op.name());
    }
  }

  /**
   * Utility to calculate new total memory size needed for an operation. <br/> Basically just offset
   * + size, unless size is 0, in which case the result is also 0.
   *
   * @param offset starting position of the memory
   * @param size number of bytes needed
   * @return offset + size, unless size is 0. In that case memNeeded is also 0.
   */
  protected BigInteger memNeeded(DataWord offset, DataWord size) {
    return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
  }


  public static OutOfMemoryException memoryOverflow(String op) {
    return new OutOfMemoryException("Out of Memory when '%s' operation executing", op);
  }

}
