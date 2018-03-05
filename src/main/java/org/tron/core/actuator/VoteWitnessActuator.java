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
          voteContract.getVoteAddressList().forEach(voteAddress -> {
            if (null != dbManager) {
              //dbManager.getWitnessStore().countVoteWitness(voteAddress, voteAdd);
              countVoteAccount(voteAddress, voteAdd);
            }
          });
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return true;
  }

  @Override
  public boolean Validator() {
    //TODO
    return false;
  }

  public void countVoteAccount(ByteString voteAddress, int voteAdd) {
    logger.info("voteAddress is {},voteAddCount is {}", voteAddress, voteAdd);

    Account accountSource = dbManager.getAccountStore().getAccount(voteAddress);
    logger.info("voteAddress pre-voteCount is {}", accountSource.getVotesList());
    Account accountTarget = accountSource.toBuilder().addVotes(
        Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteAdd)
            .build()).build();
    logger.info("voteAddress pre-voteCount is {}", accountTarget.getVotesList());

    dbManager.getAccountStore().putAccount(voteAddress, accountTarget);

  }

}
