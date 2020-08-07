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

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;


@Slf4j(topic = "Processor")
public class StakeProcessor{
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

    public boolean process(Object contract, Repository repository)  throws ContractValidateException,ContractExeException{
        if(!selfValidate(contract, repository)){
            return false;
        }
        StakeParam stakeParam = (StakeParam)contract;
        AccountCapsule accountCapsule = repository.getAccount(stakeParam.getOwnerAddress());
        long tronPower = accountCapsule.getTronPower();
        long freezeBalance = stakeParam.getStakeAmount() - tronPower;
        // if need freeze balance
        if(freezeBalance > 0) {
            SampleFreezeBalanceParam freezeBalanceParam;
            freezeBalanceParam = new SampleFreezeBalanceParam();
            freezeBalanceParam.setFrozenBalance(freezeBalance);
            freezeBalanceParam.setFrozenDuration(3);
            freezeBalanceParam.setOwnerAddress(stakeParam.getOwnerAddress());
            freezeBalanceParam.setResource(Common.ResourceCode.BANDWIDTH);
            if(!validateFreeze(freezeBalanceParam, repository)){
                return false;
            }
            if(!executeFreeze(freezeBalanceParam, repository)){
                return false;
            }
        }
        long voteCount = stakeParam.getStakeAmount() / TRX_PRECISION;
        SampleVoteWitnessParam voteWitnessParam = new SampleVoteWitnessParam();
        voteWitnessParam.setOwnerAddress(stakeParam.getOwnerAddress());
        voteWitnessParam.setVote(Protocol.Vote.newBuilder()
                .setVoteAddress(ByteString.copyFrom(stakeParam.getSrAddress()))
                .setVoteCount(voteCount).build());
        if(!validateVote(voteWitnessParam, repository)){
            return false;
        }
        return executeVote(voteWitnessParam, repository);
    }

    private boolean selfValidate(Object contract, Repository repository) throws ContractValidateException {
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
        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        if (accountCapsule == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }
        return true;
    }

    private boolean validateFreeze(SampleFreezeBalanceParam freezeBalanceParam, Repository repository) throws ContractValidateException {
        SampleFreezeBalanceProcessor freezeBalanceProcessor = SampleFreezeBalanceProcessor.getInstance();
        return freezeBalanceProcessor.validate(freezeBalanceParam, repository);
    }

    private boolean validateVote(SampleVoteWitnessParam voteWitnessParam, Repository repository) throws ContractValidateException {
        SampleVoteWitnessProcessor voteWitnessProcessor = SampleVoteWitnessProcessor.getInstance();
        return voteWitnessProcessor.validate(voteWitnessParam, repository);
    }

    private boolean executeFreeze(SampleFreezeBalanceParam freezeBalanceParam, Repository repository) throws ContractExeException{
        SampleFreezeBalanceProcessor freezeBalanceProcessor = SampleFreezeBalanceProcessor.getInstance();
        return freezeBalanceProcessor.execute(freezeBalanceParam, repository);
    }

    public boolean executeVote(SampleVoteWitnessParam voteWitnessParam, Repository repository) throws ContractExeException {
        SampleVoteWitnessProcessor voteWitnessProcessor = SampleVoteWitnessProcessor.getInstance();
        return voteWitnessProcessor.execute(voteWitnessParam, repository);
    }
}
