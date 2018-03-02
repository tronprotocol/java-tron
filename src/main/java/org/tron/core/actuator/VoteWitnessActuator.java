package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Protocal.Account;

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
    Account witnessTarget = accountSource.toBuilder()
        .addVotes(String.valueOf(voteAdd)).build();
    logger.info("voteAddress pre-voteCount is {}", witnessTarget.getVotesList());

    dbManager.getAccountStore().putAccount(voteAddress, witnessTarget);

  }
//
//  public boolean countVoteWitness(ByteString voteAddress, int countAdd) {
//    logger.info("voteAddress is {},voteAddCount is {}", voteAddress, countAdd);
//    try {
//      byte[] value = dbSource.getData(voteAddress.toByteArray());
//      if (null == value) {
//        return false;
//      }
//      Witness witnessSource = Witness.parseFrom(value).toBuilder().build();
//      logger.info("voteAddress pre-voteCount is {}", witnessSource.getVoteCount());
//      Witness witnessTarget = witnessSource.toBuilder()
//          .setVoteCount(witnessSource.getVoteCount() + countAdd).build();
//      logger.info("voteAddress pre-voteCount is {}", witnessTarget.getVoteCount());
//
//      dbSource.putData(voteAddress.toByteArray(), witnessTarget.toByteArray());
//    } catch (InvalidProtocolBufferException e) {
//      e.printStackTrace();
//    }
//    return true;
//  }
}
