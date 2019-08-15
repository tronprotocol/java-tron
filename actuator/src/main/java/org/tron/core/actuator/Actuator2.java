package org.tron.core.actuator;

import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;

public interface Actuator2 {

    void execute(TransactionContext context) throws ContractExeException;

    void validate(TransactionContext context) throws ContractValidateException;
}