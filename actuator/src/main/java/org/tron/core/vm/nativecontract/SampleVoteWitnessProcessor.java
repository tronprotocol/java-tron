package org.tron.core.vm.nativecontract;

import com.google.common.math.LongMath;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.nativecontract.param.SampleUnfreezeBalanceParam;
import org.tron.core.vm.nativecontract.param.SampleVoteWitnessParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

import java.util.Objects;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.core.vm.nativecontract.ContractProcessorConstant.*;

@Slf4j(topic = "Processor")
public class SampleVoteWitnessProcessor implements IContractProcessor {

    private SampleVoteWitnessProcessor(){}

    public static SampleVoteWitnessProcessor getInstance(){
        return SampleVoteWitnessProcessor.Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private SampleVoteWitnessProcessor instance;
        Singleton() {
            instance = new SampleVoteWitnessProcessor();
        }
        public SampleVoteWitnessProcessor getInstance() {
            return instance;
        }
    }
    @Override
    public boolean execute(Object contract, Repository repository) {
        SampleVoteWitnessParam voteWitnessParam = (SampleVoteWitnessParam) contract;
        countVoteAccount(voteWitnessParam, repository);
        return true;
    }

    @Override
    public boolean validate(Object contract, Repository repository) throws ContractValidateException {
        if (Objects.isNull(contract)) {
            throw new ContractValidateException(CONTRACT_NULL);
        }
        if (!(contract instanceof SampleVoteWitnessParam)) {
            throw new ContractValidateException(
                    "contract type error, expected type [SampleVoteWitnessParam], real type[" + contract
                            .getClass() + "]");
        }
        SampleVoteWitnessParam voteWitnessParam = (SampleVoteWitnessParam) contract;
        if (repository == null) {
            throw new ContractValidateException(ContractProcessorConstant.STORE_NOT_EXIST);
        }
        byte[] ownerAddress = voteWitnessParam.getOwnerAddress();
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
            Protocol.Vote vote = voteWitnessParam.getVote();
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

    private void countVoteAccount(SampleVoteWitnessParam voteWitnessParam, Repository repository) {
        byte[] ownerAddress = voteWitnessParam.getOwnerAddress();
        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        VotesCapsule votesCapsule;

        ContractService contractService = new ContractService(repository);
        contractService.withdrawReward(ownerAddress);

        if (repository.getVotesCapsule(ownerAddress) == null) {
            votesCapsule = new VotesCapsule(ByteString.copyFrom(voteWitnessParam.getOwnerAddress()),
                    accountCapsule.getVotesList());
        } else {
            votesCapsule = repository.getVotesCapsule(ownerAddress);
        }

        accountCapsule.clearVotes();
        votesCapsule.clearNewVotes();

        Protocol.Vote vote = voteWitnessParam.getVote();
        logger.debug("countVoteAccount, address[{}]",
                ByteArray.toHexString(vote.getVoteAddress().toByteArray()));

        votesCapsule.addNewVotes(vote.getVoteAddress(), vote.getVoteCount());
        accountCapsule.addVotes(vote.getVoteAddress(), vote.getVoteCount());

        repository.putAccountValue(accountCapsule.createDbKey(), accountCapsule);
        repository.updateVotesCapsule(ownerAddress, votesCapsule);
    }

}
