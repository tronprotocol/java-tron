package org.tron.core.actuator;

import org.tron.core.db.TransactionContext;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;

public interface ActuatorRuntime {

  public void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;
}
