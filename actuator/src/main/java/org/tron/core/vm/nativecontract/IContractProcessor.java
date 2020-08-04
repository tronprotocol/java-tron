package org.tron.core.vm.nativecontract;

import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;

public interface IContractProcessor {

    boolean execute(Object contract) throws ContractExeException;

    boolean validate(Object contract) throws ContractValidateException;

}
