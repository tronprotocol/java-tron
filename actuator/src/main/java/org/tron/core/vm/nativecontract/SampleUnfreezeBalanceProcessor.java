package org.tron.core.vm.nativecontract;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.SampleUnfreezeBalanceParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common;

import java.util.Iterator;
import java.util.List;

import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

@Slf4j(topic = "Processor")
public class SampleUnfreezeBalanceProcessor implements IContractProcessor {
    private SampleUnfreezeBalanceProcessor(){}

    public static SampleUnfreezeBalanceProcessor getInstance(){
        return SampleUnfreezeBalanceProcessor.Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private SampleUnfreezeBalanceProcessor instance;
        Singleton() {
            instance = new SampleUnfreezeBalanceProcessor();
        }
        public SampleUnfreezeBalanceProcessor getInstance() {
            return instance;
        }
    }

    @Override
    public boolean execute(Object contract, Repository repository) {
        SampleUnfreezeBalanceParam unfreezeBalanceParam = (SampleUnfreezeBalanceParam)contract;
        byte[] ownerAddress = unfreezeBalanceParam.getOwnerAddress();
        Common.ResourceCode resource = unfreezeBalanceParam.getResource();

        DynamicPropertiesStore dynamicStore = repository.getDynamicPropertiesStore();

        ContractService contractService = ContractService.getInstance();
        contractService.withdrawReward(ownerAddress, repository);

        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        long oldBalance = accountCapsule.getBalance();

        long unfreezeBalance = 0L;

        switch (resource) {
            case BANDWIDTH:
                List<Protocol.Account.Frozen> frozenList = Lists.newArrayList();
                frozenList.addAll(accountCapsule.getFrozenList());
                Iterator<Protocol.Account.Frozen> iterator = frozenList.iterator();
                long now = dynamicStore.getLatestBlockHeaderTimestamp();
                while (iterator.hasNext()) {
                    Protocol.Account.Frozen next = iterator.next();
                    if (next.getExpireTime() <= now) {
                        unfreezeBalance += next.getFrozenBalance();
                        iterator.remove();
                    }
                }

                accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                        .setBalance(oldBalance + unfreezeBalance)
                        .clearFrozen().addAllFrozen(frozenList).build());
                break;
            case ENERGY:
                unfreezeBalance = accountCapsule.getAccountResource().getFrozenBalanceForEnergy()
                        .getFrozenBalance();

                Protocol.Account.AccountResource newAccountResource = accountCapsule.getAccountResource().toBuilder()
                        .clearFrozenBalanceForEnergy().build();
                accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                        .setBalance(oldBalance + unfreezeBalance)
                        .setAccountResource(newAccountResource).build());
                break;
            default:
                break;
        }
        switch (resource) {
            case BANDWIDTH:
                dynamicStore
                        .addTotalNetWeight(-unfreezeBalance / TRX_PRECISION);
                break;
            case ENERGY:
                dynamicStore
                        .addTotalEnergyWeight(-unfreezeBalance / TRX_PRECISION);
                break;
            default:
                break;
        }

        VotesCapsule votesCapsule = repository.getVotesCapsule(ownerAddress);
        if (votesCapsule == null) {
            votesCapsule = new VotesCapsule(ByteString.copyFrom(ownerAddress),
                    accountCapsule.getVotesList());
        }
        accountCapsule.clearVotes();
        votesCapsule.clearNewVotes();

        repository.updateAccount(ownerAddress, accountCapsule);

        repository.updateVotesCapsule(ownerAddress, votesCapsule);

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
        if (!(contract instanceof SampleUnfreezeBalanceParam)) {
            throw new ContractValidateException(
                    "contract type error, expected type [SampleUnfreezeBalanceParam], real type[" + contract
                            .getClass() + "]");
        }
        SampleUnfreezeBalanceParam unfreezeBalanceParam = (SampleUnfreezeBalanceParam)contract;
        byte[] ownerAddress = unfreezeBalanceParam.getOwnerAddress();
        Common.ResourceCode resource = unfreezeBalanceParam.getResource();

        if (!DecodeUtil.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
        if (accountCapsule == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] does not exist");
        }
        long now = dynamicStore.getLatestBlockHeaderTimestamp();

        switch (resource) {
            case BANDWIDTH:
                if (accountCapsule.getFrozenCount() <= 0) {
                    throw new ContractValidateException("no frozenBalance(BANDWIDTH)");
                }

                long allowedUnfreezeCount = accountCapsule.getFrozenList().stream()
                        .filter(frozen -> frozen.getExpireTime() <= now).count();
                if (allowedUnfreezeCount <= 0) {
                    throw new ContractValidateException("It's not time to unfreeze(BANDWIDTH).");
                }
                break;
            case ENERGY:
                Protocol.Account.Frozen frozenBalanceForEnergy = accountCapsule.getAccountResource()
                        .getFrozenBalanceForEnergy();
                if (frozenBalanceForEnergy.getFrozenBalance() <= 0) {
                    throw new ContractValidateException("no frozenBalance(Energy)");
                }
                if (frozenBalanceForEnergy.getExpireTime() > now) {
                    throw new ContractValidateException("It's not time to unfreeze(Energy).");
                }

                break;
            default:
                throw new ContractValidateException(
                        "ResourceCode error.valid ResourceCode[BANDWIDTH、Energy]");
        }

        return true;
    }

}
