package org.tron.core.vm.nativecontract;

import com.google.common.math.LongMath;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.StringUtil;
import org.tron.core.actuator.ActuatorConstant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.nativecontract.param.VoteWitnessParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

import java.util.Iterator;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.NOT_EXIST_STR;
import static org.tron.core.actuator.ActuatorConstant.WITNESS_EXCEPTION_STR;
import static org.tron.core.config.Parameter.ChainConstant.MAX_VOTE_NUMBER;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

@Slf4j(topic = "Processor")
public class VoteWitnessProcessor {

  public void validate(VoteWitnessParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }

    if (param.getVotes().isEmpty()) {
      throw new ContractValidateException("VoteNumber must more than 0");
    }
    if (param.getVotes().size() > MAX_VOTE_NUMBER) {
      throw new ContractValidateException(
          "VoteNumber more than maxVoteNumber " + MAX_VOTE_NUMBER);
    }

    WitnessStore witnessStore = repo.getWitnessStore();
    Iterator<Protocol.Vote> iterator = param.getVotes().iterator();
    try {
      long sum = 0;
      while (iterator.hasNext()) {
        Protocol.Vote vote = iterator.next();

        byte[] witnessAddress = vote.getVoteAddress().toByteArray();
        String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
        if (repo.getAccount(witnessAddress) != null) {
          throw new ContractValidateException(
              ACCOUNT_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
        }
        if (!witnessStore.has(witnessAddress)) {
          throw new ContractValidateException(
              WITNESS_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
        }

        long voteCount = vote.getVoteCount();
        if (voteCount <= 0) {
          throw new ContractValidateException("Vote count must be greater than 0");
        }
        sum = LongMath.checkedAdd(sum, voteCount);
      }

      AccountCapsule accountCapsule = repo.getAccount(param.getOwnerAddress());
      long tronPower = accountCapsule.getTronPower();
      sum =  LongMath.checkedMultiply(sum, TRX_PRECISION);
      if (sum > tronPower) {
        throw new ContractValidateException(
            "The total number of votes[" + sum + "] is greater than the tronPower[" + tronPower
                + "]");
      }
    } catch (ArithmeticException e) {
      throw new ContractValidateException(e.getMessage());
    }
  }

  public void execute(VoteWitnessParam param, Repository repo) {
    byte[] ownerAddress = param.getOwnerAddress();
    VoteRewardUtils.withdrawReward(ownerAddress, repo);

    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);

    VotesCapsule votesCapsule = repo.getVotesCapsule(ownerAddress);
    if (votesCapsule == null) {
      votesCapsule = new VotesCapsule(ByteString.copyFrom(ownerAddress),
          accountCapsule.getVotesList());
    }

    accountCapsule.clearVotes();
    votesCapsule.clearNewVotes();

    for (Protocol.Vote vote : param.getVotes()) {
      accountCapsule.addVotes(vote.getVoteAddress(), vote.getVoteCount());
      votesCapsule.addNewVotes(vote.getVoteAddress(), vote.getVoteCount());
    }

    repo.updateAccount(accountCapsule.createDbKey(), accountCapsule);
    repo.updateVotesCapsule(ownerAddress, votesCapsule);
  }
}
