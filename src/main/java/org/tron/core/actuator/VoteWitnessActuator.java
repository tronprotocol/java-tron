package org.tron.core.actuator;

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
      if (contract.is(VoteWitnessContract.class)) {
        VoteWitnessContract voteContract = contract.unpack(VoteWitnessContract.class);
        countVoteAccount(voteContract);
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return true;
  }

  @Override
  public boolean validate() {
    //TODO witness
    return false;
  }

  public void countVoteAccount(VoteWitnessContract voteContract) {

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(voteContract.getOwnerAddress().toByteArray());

    voteContract.getVotesList().forEach(vote -> {
      accountCapsule.addVotes(vote.getVoteAddress(), vote.getVoteCount());
    });

    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;

  }

}
