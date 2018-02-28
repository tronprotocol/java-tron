package org.tron.core.db.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.WitnessStore;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Protocal.Transaction;

public class TransactionVoteActuator extends AbstractTransactionActuator {

  private static final Logger logger = LoggerFactory.getLogger("TransactionVoteActuator");

  TransactionVoteActuator(TransactionCapsule transactionCapsule,
      TronDatabase tronDatabase) {
    super(transactionCapsule, tronDatabase);
  }


  @Override
  public boolean execute() {
    //TODO
    return true;
  }

  private void voteWitnessCount(Transaction trx) {
    try {
      if (trx.getParameterList() == null || trx.getParameterList().isEmpty()) {
        return;
      }
      Any parameter = trx.getParameterList().get(0);
      if (parameter.is(VoteWitnessContract.class)) {
        VoteWitnessContract voteContract = parameter.unpack(VoteWitnessContract.class);
        int voteAdd = voteContract.getCount();
        if (voteAdd > 0) {
          voteContract.getVoteAddressList().forEach(voteAddress -> {
            if (tronDatabase instanceof WitnessStore) {
              ((WitnessStore) tronDatabase).countvoteWitness(voteAddress, voteAdd);
            }
          });
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

}
