package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.WitnessCreateContract;
import org.tron.protos.Protocal.Witness;

public class WitnessCteateActuator extends AbstractActuator {


  private static final Logger logger = LoggerFactory.getLogger("WitnessCteateActuator");

  WitnessCteateActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }


  @Override
  public boolean execute() {
    try {
      if (contract.is(WitnessCreateContract.class)) {
        WitnessCreateContract witnessCreateContract = contract.unpack(WitnessCreateContract.class);
        createWitness(witnessCreateContract);
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return true;
  }

  @Override
  public boolean validator() {
    return false;
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;
  }

  private void createWitness(WitnessCreateContract witnessCreateContract) {
    //Create Witness by witnessCreateContract
    Witness witness = Witness.newBuilder()
        .setAddress(witnessCreateContract.getOwnerAddress())
        .setVoteCount(0).build();

    dbManager.getWitnessStore().putWitness(witness);
  }

}
