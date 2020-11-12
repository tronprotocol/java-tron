package org.tron.core.vm.nativecontract;

import com.google.common.math.LongMath;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.nativecontract.param.StakeParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

import static org.tron.core.actuator.ActuatorConstant.*;


@Slf4j(topic = "Processor")
public class StakeProcessor {

  public void process(StakeParam stakeParam, Repository repository)
      throws ContractValidateException, ContractExeException {
    selfValidate(stakeParam, repository);
    AccountCapsule accountCapsule = repository.getAccount(stakeParam.getOwnerAddress());
    long tronPower = accountCapsule.getTronPower();
    long freezeBalance = stakeParam.getStakeAmount() - tronPower;
    // if need freeze balance
    if (freezeBalance > 0) {
      long frozenDuration = repository.getDynamicPropertiesStore().getMinFrozenTime();
      validateFreeze(stakeParam.getOwnerAddress(), frozenDuration, freezeBalance, repository);
      executeFreeze(stakeParam.getOwnerAddress(), frozenDuration, freezeBalance, stakeParam.getNow(), repository);
    } else {
      logger.info("no need to freeze for stake");
    }
    long voteCount = stakeParam.getStakeAmount() / ChainConstant.TRX_PRECISION;
    Protocol.Vote vote = Protocol.Vote.newBuilder()
        .setVoteAddress(ByteString.copyFrom(stakeParam.getSrAddress()))
        .setVoteCount(voteCount).build();
    validateVote(stakeParam.getOwnerAddress(), vote, repository);
    executeVote(stakeParam.getOwnerAddress(), vote, repository);
  }

  private void selfValidate(StakeParam stakeParam, Repository repository)
      throws ContractValidateException {
    if (stakeParam == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (repository == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }

    byte[] ownerAddress = stakeParam.getOwnerAddress();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
              ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
    }
  }

  private void validateFreeze(byte[] ownerAddress, long frozenDuration,
                              long frozenBalance, Repository repository)
      throws ContractValidateException {
    AccountCapsule accountCapsule = repository.getAccount(ownerAddress);

    if (frozenBalance <= 0) {
      throw new ContractValidateException("frozenBalance must be positive");
    }
    if (frozenBalance < ChainConstant.TRX_PRECISION) {
      throw new ContractValidateException("frozenBalance must be more than 1TRX");
    }

    int frozenCount = accountCapsule.getFrozenCount();
    if (frozenCount > 1) {
      throw new ContractValidateException("frozenCount must be 0 or 1");
    }
    if (frozenBalance > accountCapsule.getBalance()) {
      throw new ContractValidateException("frozenBalance must be less than accountBalance");
    }
  }

  private void validateVote(byte[] ownerAddress, Protocol.Vote vote, Repository repository)
      throws ContractValidateException {
    AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
    WitnessStore witnessStore = repository.getWitnessStore();
    try {
      long sum;
      {
        byte[] witnessCandidate = vote.getVoteAddress().toByteArray();
        if (!DecodeUtil.addressValid(witnessCandidate)) {
          throw new ContractValidateException("Invalid vote address!");
        }
        if (vote.getVoteCount() <= 0) {
          throw new ContractValidateException("vote count must be greater than 0");
        }
        if (repository.getAccount(witnessCandidate) == null) {
          String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
          throw new ContractValidateException(
              ContractProcessorConstant.ACCOUNT_EXCEPTION_STR
                  + readableWitnessAddress + ContractProcessorConstant.NOT_EXIST_STR);
        }
        if (!witnessStore.has(witnessCandidate)) {
          String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
          throw new ContractValidateException(
              ContractProcessorConstant.WITNESS_EXCEPTION_STR
                  + readableWitnessAddress + ContractProcessorConstant.NOT_EXIST_STR);
        }
        sum = vote.getVoteCount();
      }
      long tronPower = accountCapsule.getTronPower();

      // trx -> drop. The vote count is based on TRX
      sum = LongMath.checkedMultiply(sum, ChainConstant.TRX_PRECISION);
      if (sum > tronPower) {
        throw new ContractValidateException(
            "The total number of votes[" + sum + "] is greater than the tronPower[" + tronPower
                + "]");
      }
    } catch (ArithmeticException e) {
      logger.error(e.getMessage(), e);
      throw new ContractValidateException("error when sum all votes");
    }
  }

  private void executeFreeze(byte[] ownerAddress, long frozenDuration,
                             long frozenBalance, long now, Repository repository)
      throws ContractExeException {
    AccountCapsule accountCapsule = repository.getAccount(ownerAddress);

    long duration = frozenDuration * ChainConstant.FROZEN_PERIOD;

    long newBalance = accountCapsule.getBalance() - frozenBalance;

    long expireTime = now + duration;
    long newFrozenBalanceForBandwidth =
        frozenBalance + accountCapsule.getFrozenBalance();
    accountCapsule.setFrozenForBandwidth(newFrozenBalanceForBandwidth, expireTime);

    accountCapsule.setBalance(newBalance);
    repository.updateAccount(accountCapsule.createDbKey(), accountCapsule);
    repository
            .addTotalNetWeight(frozenBalance / ChainConstant.TRX_PRECISION);
  }

  private void executeVote(byte[] ownerAddress, Protocol.Vote vote, Repository repository)
      throws ContractExeException {
    VotesCapsule votesCapsule;

    ContractService contractService = ContractService.getInstance();
    contractService.withdrawReward(ownerAddress, repository);

    AccountCapsule accountCapsule = repository.getAccount(ownerAddress);
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
  }
}
