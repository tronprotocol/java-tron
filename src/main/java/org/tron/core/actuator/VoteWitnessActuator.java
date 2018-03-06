package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Protocal.Account;
import org.tron.protos.Protocal.Account.Vote;

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
        int voteAdd = voteContract.getCount();
        if (voteAdd > 0) {
          int countVote = 0;
          countVoteAccount(voteContract);
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return true;
  }

  @Override
  public boolean validator() {
    //TODO
    return false;
  }

  public void countVoteAccount(VoteWitnessContract voteContract) {
    int voteAdd = voteContract.getCount();
    logger.info("voteAddress is {},voteAddCount is {}", voteContract.getOwnerAddress(), voteAdd);

    Account accountSource = dbManager.getAccountStore().getAccount(voteContract.getOwnerAddress());
    logger.info("voteAddress pre-voteCount is {}", accountSource.getVotesList());
    Account.Builder accountBuilder = accountSource.toBuilder();

    voteContract.getVoteAddressList().forEach(voteAddress -> {
      accountBuilder
          .addVotes(Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteAdd).build());
    });
    Account accountTarget = accountBuilder.build();
    logger.info("voteAddress pre-voteCount is {}", accountTarget.getVotesList());

    dbManager.getAccountStore().putAccount(voteContract.getOwnerAddress(), accountTarget);
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;

  }

}
