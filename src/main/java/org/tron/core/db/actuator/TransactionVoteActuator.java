package org.tron.core.db.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Protocal.Transaction;

public class TransactionVoteActuator extends AbstractTransactionActuator {

  private static final Logger logger = LoggerFactory.getLogger("TransactionVoteActuator");

  TransactionVoteActuator(TransactionCapsule transactionCapsule,
      Manager dbManager) {
    super(transactionCapsule, dbManager);
  }


  @Override
  public boolean execute() {
    if (null == transactionCapsule || null == transactionCapsule.getTransaction()) {
      return false;
    }
    return voteWitnessCount(transactionCapsule.getTransaction());

  }

  private boolean voteWitnessCount(Transaction trx) {
    try {
      if (null == trx.getParameterList() || trx.getParameterList().isEmpty()) {
        return false;
      }
      Any parameter = trx.getParameterList().get(0);
      if (parameter.is(VoteWitnessContract.class)) {
        VoteWitnessContract voteContract = parameter.unpack(VoteWitnessContract.class);
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

}
