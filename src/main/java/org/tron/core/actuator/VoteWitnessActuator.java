package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        int voteAdd = voteContract.getCount();
        if (voteAdd > 0) {
          voteContract.getVoteAddressList().forEach(voteAddress -> {
            if (null != dbManager) {
              dbManager.getWitnessStore().countvoteWitness(voteAddress, voteAdd);
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

}
