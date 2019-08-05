package org.tron.common.runtime2;

import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.VMIllegalException;

/**
 * Interface to Run the Transaction.
 */
public interface TxRunner {
  void execute() throws ContractValidateException, ContractExeException, VMIllegalException;

  ProgramResult getResult();

  void finalization();
}
