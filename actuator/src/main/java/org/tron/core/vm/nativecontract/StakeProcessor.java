package org.tron.core.vm.nativecontract;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.nativecontract.param.SampleFreezeBalanceParam;
import org.tron.core.vm.nativecontract.param.StakeParam;
import org.tron.core.vm.nativecontract.param.SampleVoteWitnessParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;


@Slf4j(topic = "Processor")
public class StakeProcessor implements IContractProcessor {
    private StakeProcessor(){}

    public static StakeProcessor getInstance(){
        return StakeProcessor.Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private StakeProcessor instance;
        Singleton() {
            instance = new StakeProcessor();
        }
        public StakeProcessor getInstance() {
            return instance;
        }
    }

    @Override
    public boolean execute(Object contract, Repository repository) throws ContractExeException {
        StakeParam stakeParam = (StakeParam)contract;
        byte[] ownerAddress = stakeParam.getOwnerAddress();
        byte[] srAddress = stakeParam.getSrAddress();
        long stakeAmount = stakeParam.getStakeAmount();
        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);

        long tronPower = accountCapsule.getTronPower();
        // if need freeze balance
        if(tronPower < stakeAmount){
            long freezeBalance = stakeAmount - tronPower;
            long duration = 3;
            SampleFreezeBalanceParam freezeBalanceParam = new SampleFreezeBalanceParam();
            freezeBalanceParam.setFrozenBalance(freezeBalance);
            freezeBalanceParam.setFrozenDuration(duration);
            freezeBalanceParam.setOwnerAddress(ownerAddress);
            freezeBalanceParam.setResource(Common.ResourceCode.BANDWIDTH);

            SampleFreezeBalanceProcessor freezeBalanceProcessor = SampleFreezeBalanceProcessor.getInstance();
            freezeBalanceProcessor.execute(freezeBalanceParam, repository);
        }

        SampleVoteWitnessProcessor voteWitnessProcessor = SampleVoteWitnessProcessor.getInstance();
        SampleVoteWitnessParam voteWitnessParam = new SampleVoteWitnessParam();
        voteWitnessParam.setOwnerAddress(ownerAddress);
        voteWitnessParam.setVote(Protocol.Vote.newBuilder().setVoteAddress(ByteString.copyFrom(srAddress)).setVoteCount(stakeAmount).build());
        voteWitnessProcessor.execute(voteWitnessParam, repository);

        return false;
    }

    @Override
    public boolean validate(Object contract, Repository repository) throws ContractValidateException {
        if (contract == null) {
            throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
        }
        if (repository == null) {
            throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
        }

        if (!(contract instanceof StakeParam)) {
            throw new ContractValidateException(
                    "contract type error,expected type [StakeParam],real type[" + contract
                            .getClass() + "]");
        }
        StakeParam stakeParam = (StakeParam)contract;
        byte[] ownerAddress = stakeParam.getOwnerAddress();
        byte[] srAddress = stakeParam.getSrAddress();
        long stakeAmount = stakeParam.getStakeAmount();

        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        if (accountCapsule == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        boolean freezeValidateResult = true;
        long tronPower = accountCapsule.getTronPower();
        // if need freeze balance
        if(tronPower < stakeAmount){
            long freezeBalance = stakeAmount - tronPower;
            long duration = 3;
            SampleFreezeBalanceParam freezeBalanceParam = new SampleFreezeBalanceParam();
            freezeBalanceParam.setFrozenBalance(freezeBalance);
            freezeBalanceParam.setFrozenDuration(duration);
            freezeBalanceParam.setOwnerAddress(ownerAddress);
            freezeBalanceParam.setResource(Common.ResourceCode.BANDWIDTH);

            SampleFreezeBalanceProcessor freezeBalanceProcessor = SampleFreezeBalanceProcessor.getInstance();
            freezeValidateResult = freezeBalanceProcessor.validate(freezeBalanceParam, repository);
        }

        SampleVoteWitnessProcessor voteWitnessProcessor = SampleVoteWitnessProcessor.getInstance();
        SampleVoteWitnessParam voteWitnessParam = new SampleVoteWitnessParam();
        voteWitnessParam.setOwnerAddress(ownerAddress);
        voteWitnessParam.setVote(Protocol.Vote.newBuilder().setVoteAddress(ByteString.copyFrom(srAddress)).setVoteCount(stakeAmount).build());
        return voteWitnessProcessor.validate(voteWitnessParam, repository) && freezeValidateResult;
    }
}
