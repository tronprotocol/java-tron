package org.tron.core.vm.nativecontract;

import com.google.common.math.LongMath;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.nativecontract.param.StakeParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.core.vm.nativecontract.ContractProcessorConstant.*;


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
            if(!validateFreeze(stakeParam.getOwnerAddress(), 3L, freezeBalance, Common.ResourceCode.BANDWIDTH, repository)){
                return false;
            }
            if(!executeFreeze(stakeParam.getOwnerAddress(), 3L, freezeBalance, Common.ResourceCode.BANDWIDTH, repository)){
                return false;
            }
        }
        long voteCount = stakeParam.getStakeAmount() / TRX_PRECISION;
        Protocol.Vote vote = Protocol.Vote.newBuilder()
                .setVoteAddress(ByteString.copyFrom(stakeParam.getSrAddress()))
                .setVoteCount(voteCount).build();
        if(!validateVote(stakeParam.getOwnerAddress(), vote, repository)){
            return false;
        }
        return executeVote(stakeParam.getOwnerAddress(), vote, repository);
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

    private boolean validateFreeze(byte[] ownerAddress, long frozenDuration, long frozenBalance, Common.ResourceCode resource, Repository repository) throws ContractValidateException {
        if (repository == null) {
            throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
        }

        DynamicPropertiesStore dynamicStore = repository.getDynamicPropertiesStore();

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

    private boolean validateVote(byte[] ownerAddress, Protocol.Vote vote, Repository repository) throws ContractValidateException {
        if (repository == null) {
            throw new ContractValidateException(ContractProcessorConstant.STORE_NOT_EXIST);
        }
        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }
        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
        if (accountCapsule == null) {
            throw new ContractValidateException(
                    ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
        }

        WitnessStore witnessStore = repository.getWitnessStore();
        try {
            Long sum = 0L;
            byte[] witnessCandidate = vote.getVoteAddress().toByteArray();
            if (!DecodeUtil.addressValid(witnessCandidate)) {
                throw new ContractValidateException("Invalid vote address!");
            }
            if (vote.getVoteCount() <= 0) {
                throw new ContractValidateException("vote count must be greater than 0");
            }
            String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
            if (repository.getAccount(witnessCandidate) == null) {
                throw new ContractValidateException(
                        ACCOUNT_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
            }
            if (!witnessStore.has(witnessCandidate)) {
                throw new ContractValidateException(
                        WITNESS_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
            }
            sum = vote.getVoteCount();

            long tronPower = accountCapsule.getTronPower();

            // trx -> drop. The vote count is based on TRX
            sum = LongMath
                    .checkedMultiply(sum, TRX_PRECISION);
            if (sum > tronPower) {
                throw new ContractValidateException(
                        "The total number of votes[" + sum + "] is greater than the tronPower[" + tronPower
                                + "]");
            }
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    private boolean executeFreeze(byte[] ownerAddress, long frozenDuration, long frozenBalance, Common.ResourceCode resource, Repository repository) throws ContractExeException{
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

    public boolean executeVote(byte[] ownerAddress, Protocol.Vote vote, Repository repository) throws ContractExeException {
        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        VotesCapsule votesCapsule;

        ContractService contractService = ContractService.getInstance();
        contractService.withdrawReward(ownerAddress, repository);

        if (repository.getVotesCapsule(ownerAddress) == null) {
            votesCapsule = new VotesCapsule(ByteString.copyFrom(ownerAddress),
                    accountCapsule.getVotesList());
        } else {
            votesCapsule = repository.getVotesCapsule(ownerAddress);
        }

        accountCapsule.clearVotes();
        votesCapsule.clearNewVotes();

        logger.debug("countVoteAccount, address[{}]",
                ByteArray.toHexString(vote.getVoteAddress().toByteArray()));

        votesCapsule.addNewVotes(vote.getVoteAddress(), vote.getVoteCount());
        accountCapsule.addVotes(vote.getVoteAddress(), vote.getVoteCount());

        repository.putAccountValue(accountCapsule.createDbKey(), accountCapsule);
        repository.updateVotesCapsule(ownerAddress, votesCapsule);
        return true;
    }
}
