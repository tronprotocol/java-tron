package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.WitnessCreateContract;

public class WitnessCreateActuator extends AbstractActuator {


  private static final Logger logger = LoggerFactory.getLogger("WitnessCreateActuator");

  WitnessCreateActuator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }


  @Override
  public boolean execute() {
    try {
      final WitnessCreateContract witnessCreateContract = this.contract
          .unpack(WitnessCreateContract.class);
      this.createWitness(witnessCreateContract);
    } catch (final InvalidProtocolBufferException e) {
      throw new RuntimeException("Parse contract error", e);
    }
    return true;
  }

  @Override
  public boolean validate() {
    try {
      if (!this.contract.is(WitnessCreateContract.class)) {
        throw new RuntimeException(
            "contract type error,expected type [AccountCreateContract],real type[" + this.contract
                .getClass() + "]");
      }

      final WitnessCreateContract contract = this.contract.unpack(WitnessCreateContract.class);

      Preconditions.checkNotNull(contract.getOwnerAddress(), "OwnerAddress is null");

      if (this.dbManager.getWitnessStore().getWitness(contract.getOwnerAddress()) != null) {
        throw new RuntimeException("Witness has existed");
      }

    } catch (final Exception ex) {
      throw new RuntimeException("Validate WitnessCreateContract error.", ex);
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() {
    return null;
  }

  private void createWitness(final WitnessCreateContract witnessCreateContract) {
    //Create Witness by witnessCreateContract
    final WitnessCapsule witnessCapsule = new WitnessCapsule(
        witnessCreateContract.getOwnerAddress(), 0, "");

    this.dbManager.getWitnessStore().putWitness(witnessCapsule);
  }

}
