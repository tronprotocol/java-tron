package org.tron.core.vm.nativecontract;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.nativecontract.param.SampleUnfreezeBalanceParam;
import org.tron.core.vm.nativecontract.param.UnstakeParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.contract.Common;

@Slf4j(topic = "Processor")
public class UnstakeProcessor implements IContractProcessor {
    private UnstakeProcessor(){}

    public UnstakeProcessor getInstance(){
        return UnstakeProcessor.Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private UnstakeProcessor instance;
        Singleton() {
            instance = new UnstakeProcessor();
        }
        public UnstakeProcessor getInstance() {
            return instance;
        }
    }

    @Override
    public boolean execute(Object contract, Repository repository) throws ContractExeException {
        UnstakeParam unstakeParam = (UnstakeParam)contract;
        byte[] ownerAddress = unstakeParam.getOwnerAddress();

        SampleUnfreezeBalanceProcessor unfreezeBalanceProcessor = SampleUnfreezeBalanceProcessor.getInstance();
        SampleUnfreezeBalanceParam unfreezeBalanceParam = new SampleUnfreezeBalanceParam();
        unfreezeBalanceParam.setOwnerAddress(ownerAddress);
        unfreezeBalanceParam.setResource(Common.ResourceCode.BANDWIDTH);
        unfreezeBalanceProcessor.execute(unfreezeBalanceParam, repository);
        return true;
    }

    @Override
    public boolean validate(Object contract, Repository repository) throws ContractValidateException {
        if (contract == null) {
            throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
        }
        if (repository == null) {
            throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
        }

        if (!(contract instanceof UnstakeParam)) {
            throw new ContractValidateException(
                    "contract type error,expected type [UnstakeParam],real type[" + contract
                            .getClass() + "]");
        }
        UnstakeParam unstakeParam = (UnstakeParam)contract;
        byte[] ownerAddress = unstakeParam.getOwnerAddress();

        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }
        SampleUnfreezeBalanceProcessor unfreezeBalanceProcessor = SampleUnfreezeBalanceProcessor.getInstance();
        SampleUnfreezeBalanceParam unfreezeBalanceParam = new SampleUnfreezeBalanceParam();
        unfreezeBalanceParam.setOwnerAddress(ownerAddress);
        unfreezeBalanceParam.setResource(Common.ResourceCode.BANDWIDTH);
        return unfreezeBalanceProcessor.validate(unfreezeBalanceParam, repository);
    }

}
