package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.VoteWitnessContract;

public class VoteWitnessActuator extends AbstractActuator {

  private static final Logger logger = LoggerFactory.getLogger("VoteWitnessActuator");

  VoteWitnessActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }


  @Override
  public boolean execute() {
    try {
        VoteWitnessContract voteContract = contract.unpack(VoteWitnessContract.class);
        countVoteAccount(voteContract);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Parse contract error", e);
    }
    return true;
  }

  @Override
  public boolean validator() {try {
    if (!contract.is(VoteWitnessContract.class)) {
      throw new RuntimeException(
          "contract type error,expected type [VoteWitnessContract],real type[" + contract
              .getClass() + "]");
    }

    VoteWitnessContract contract = this.contract.unpack(VoteWitnessContract.class);

    Preconditions.checkNotNull(contract.getOwnerAddress(), "OwnerAddress is null");

    boolean accountExist = dbManager.getAccountStore()
        .isAccountExist(contract.getOwnerAddress().toByteArray());
    if (!accountExist) {
      throw new RuntimeException("OwnerAddress not exists");
    }

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .getAccount(contract.getOwnerAddress());

  } catch (Exception ex) {
    throw new RuntimeException("Validate AccountCreateContract error.", ex);
  }

    return true;
  }

  private void countVoteAccount(VoteWitnessContract voteContract) {

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .getAccount(voteContract.getOwnerAddress());

    voteContract.getVotesList().forEach(vote -> {
      accountCapsule.addVotes(vote.getVoteAddress(), vote.getVoteCount());
    });

    dbManager.getAccountStore().putAccount(accountCapsule.getAddress(), accountCapsule);
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;

  }

}
