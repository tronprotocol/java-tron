package org.tron.common.runtime2.tvm.interpretor;

import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime2.tvm.ContractExecutor;

public interface OpExecutor {

  public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy);

}
