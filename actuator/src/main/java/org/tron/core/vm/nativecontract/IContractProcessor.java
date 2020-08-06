package org.tron.core.vm.nativecontract;

import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.repository.Repository;

public interface IContractProcessor {

    boolean execute(Object contract, Repository repository) throws ContractExeException;

    boolean validate(Object contract, Repository repository) throws ContractValidateException;

}
