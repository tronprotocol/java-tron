package org.tron.core.vm.nativecontract;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.SampleFreezeBalanceParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.contract.Common;


import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;


@Slf4j(topic = "Processor")
public class SampleFreezeBalanceProcessor implements IContractProcessor {
    private SampleFreezeBalanceProcessor(){}

    public static SampleFreezeBalanceProcessor getInstance(){
        return Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private SampleFreezeBalanceProcessor instance;
        Singleton() {
            instance = new SampleFreezeBalanceProcessor();
        }
        public SampleFreezeBalanceProcessor getInstance() {
            return instance;
        }
    }

    @Override
    public boolean execute(Object contract, Repository repository) {
        SampleFreezeBalanceParam freezeBalanceParam = (SampleFreezeBalanceParam)contract;
        byte[] ownerAddress = freezeBalanceParam.getOwnerAddress();
        long frozenDuration = freezeBalanceParam.getFrozenDuration();
        long frozenBalance = freezeBalanceParam.getFrozenBalance();
        Common.ResourceCode resource = freezeBalanceParam.getResource();

        DynamicPropertiesStore dynamicStore = repository.getDynamicPropertiesStore();
        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);

        long now = dynamicStore.getLatestBlockHeaderTimestamp();
        long duration = frozenDuration * FROZEN_PERIOD;

        long newBalance = accountCapsule.getBalance() - frozenBalance;

        long expireTime = now + duration;

        switch (resource) {
            case BANDWIDTH:

                long newFrozenBalanceForBandwidth =
                        frozenBalance + accountCapsule.getFrozenBalance();
                accountCapsule.setFrozenForBandwidth(newFrozenBalanceForBandwidth, expireTime);
                repository
                        .addTotalNetWeight(frozenBalance / TRX_PRECISION);
                break;
            case ENERGY:

                long newFrozenBalanceForEnergy =
                        frozenBalance + accountCapsule.getAccountResource()
                                .getFrozenBalanceForEnergy()
                                .getFrozenBalance();
                accountCapsule.setFrozenForEnergy(newFrozenBalanceForEnergy, expireTime);
                repository
                        .addTotalEnergyWeight(frozenBalance / TRX_PRECISION);
                break;
            default:
                logger.debug("Resource Code Error.");
        }

        accountCapsule.setBalance(newBalance);
        repository.updateAccount(accountCapsule.createDbKey(), accountCapsule);

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

        DynamicPropertiesStore dynamicStore = repository.getDynamicPropertiesStore();
        if (!(contract instanceof SampleFreezeBalanceParam)) {
            throw new ContractValidateException(
                    "contract type error,expected type [SampleFreezeBalanceParam],real type[" + contract
                            .getClass() + "]");
        }
        SampleFreezeBalanceParam freezeBalanceParam = (SampleFreezeBalanceParam)contract;
        byte[] ownerAddress = freezeBalanceParam.getOwnerAddress();
        long frozenDuration = freezeBalanceParam.getFrozenDuration();
        long frozenBalance = freezeBalanceParam.getFrozenBalance();
        Common.ResourceCode resource = freezeBalanceParam.getResource();

        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        if (accountCapsule == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        if (frozenBalance <= 0) {
            throw new ContractValidateException("frozenBalance must be positive");
        }
        if (frozenBalance < TRX_PRECISION) {
            throw new ContractValidateException("frozenBalance must be more than 1TRX");
        }

        int frozenCount = accountCapsule.getFrozenCount();
        if (!(frozenCount == 0 || frozenCount == 1)) {
            throw new ContractValidateException("frozenCount must be 0 or 1");
        }
        if (frozenBalance > accountCapsule.getBalance()) {
            throw new ContractValidateException("frozenBalance must be less than accountBalance");
        }

        long minFrozenTime = dynamicStore.getMinFrozenTime();
        long maxFrozenTime = dynamicStore.getMaxFrozenTime();

        boolean needCheckFrozeTime = CommonParameter.getInstance()
                .getCheckFrozenTime() == 1;//for test
        if (needCheckFrozeTime && !(frozenDuration >= minFrozenTime
                && frozenDuration <= maxFrozenTime)) {
            throw new ContractValidateException(
                    "frozenDuration must be less than " + maxFrozenTime + " days "
                            + "and more than " + minFrozenTime + " days");
        }

        switch (resource) {
            case BANDWIDTH:
                break;
            case ENERGY:
                break;
            default:
                throw new ContractValidateException(
                        "ResourceCode error,valid ResourceCode[BANDWIDTH、ENERGY]");
        }

        return true;
    }
}
