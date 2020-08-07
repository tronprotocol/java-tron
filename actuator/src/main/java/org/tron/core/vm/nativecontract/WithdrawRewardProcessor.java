package org.tron.core.vm.nativecontract;

import com.google.common.math.LongMath;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.WithdrawRewardParam;
import org.tron.core.vm.repository.Repository;

import java.util.Arrays;
import java.util.Objects;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.vm.nativecontract.ContractProcessorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.vm.nativecontract.ContractProcessorConstant.CONTRACT_NULL;

@Slf4j(topic = "Processor")
public class WithdrawRewardProcessor implements IContractProcessor {

    private WithdrawRewardProcessor(){}

    public static WithdrawRewardProcessor getInstance(){
        return WithdrawRewardProcessor.Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private WithdrawRewardProcessor instance;
        Singleton() {
            instance = new WithdrawRewardProcessor();
        }
        public WithdrawRewardProcessor getInstance() {
            return instance;
        }
    }

    @Override
    public boolean execute(Object contract, Repository repository) throws ContractExeException {
        WithdrawRewardParam withdrawRewardParam = (WithdrawRewardParam) contract;
        byte[] targetAddress = withdrawRewardParam.getTargetAddress();
        AccountCapsule accountCapsule = repository.getAccount(targetAddress);

        ContractService contractService = ContractService.getInstance();
        contractService.withdrawReward(targetAddress,repository);

        repository.updateLastWithdrawCycle(targetAddress, repository.getDynamicPropertiesStore().getCurrentCycleNumber()
                );

        long oldBalance = accountCapsule.getBalance();
        long allowance = accountCapsule.getAllowance();

        long now = repository.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
        accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                .setBalance(oldBalance + allowance)
                .setAllowance(0L)
                .setLatestWithdrawTime(now)
                .build());
        repository.putAccountValue(accountCapsule.createDbKey(), accountCapsule);
        return true;
    }

    @Override
    public boolean validate(Object contract, Repository repository) throws ContractValidateException {
        if (Objects.isNull(contract)) {
            throw new ContractValidateException(CONTRACT_NULL);
        }
        if (!(contract instanceof WithdrawRewardParam)) {
            throw new ContractValidateException(
                    "contract type error, expected type [WithdrawRewardParam], real type[" + contract
                            .getClass() + "]");
        }
        WithdrawRewardParam withdrawRewardParam = (WithdrawRewardParam) contract;
        if (repository == null) {
            throw new ContractValidateException(ContractProcessorConstant.STORE_NOT_EXIST);
        }
        byte[] targetAddress = withdrawRewardParam.getTargetAddress();
        if (!DecodeUtil.addressValid(targetAddress)) {
            throw new ContractValidateException("Invalid address");
        }
        AccountCapsule accountCapsule = repository.getAccount(targetAddress);
        DynamicPropertiesStore dynamicStore = repository.getDynamicPropertiesStore();
        ContractService contractService = ContractService.getInstance();
        if (accountCapsule == null) {
            String readableOwnerAddress = StringUtil.createReadableString(targetAddress);
            throw new ContractValidateException(
                    ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] not exists");
        }
        String readableOwnerAddress = StringUtil.createReadableString(targetAddress);
        boolean isGP = CommonParameter.getInstance()
                .getGenesisBlock().getWitnesses().stream().anyMatch(witness ->
                        Arrays.equals(targetAddress, witness.getAddress()));
        if (isGP) {
            throw new ContractValidateException(
                    ACCOUNT_EXCEPTION_STR + readableOwnerAddress
                            + "] is a guard representative and is not allowed to withdraw Balance");
        }

        long latestWithdrawTime = accountCapsule.getLatestWithdrawTime();
        long now = dynamicStore.getLatestBlockHeaderTimestamp();
        long witnessAllowanceFrozenTime = dynamicStore.getWitnessAllowanceFrozenTime() * FROZEN_PERIOD;

        if (now - latestWithdrawTime < witnessAllowanceFrozenTime) {
            throw new ContractValidateException("The last withdraw time is "
                    + latestWithdrawTime + ", less than 24 hours");
        }

        if (accountCapsule.getAllowance() <= 0 &&
                contractService.queryReward(targetAddress, repository) <= 0) {
            throw new ContractValidateException("witnessAccount does not have any reward");
        }
        try {
            LongMath.checkedAdd(accountCapsule.getBalance(), accountCapsule.getAllowance());
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

}
