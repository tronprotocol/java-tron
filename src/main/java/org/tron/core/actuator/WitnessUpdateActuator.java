package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.WitnessUpdateContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WitnessUpdateActuator extends AbstractActuator {
  WitnessUpdateActuator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  private void updateWitness(final WitnessUpdateContract witnessUpdateContract) {
    final WitnessCapsule witnessCapsule = new WitnessCapsule(witnessUpdateContract.getOwnerAddress(),
            0, witnessUpdateContract.getUpdateUrl().toString());

    this.dbManager.getWitnessStore().put(witnessCapsule.createDbKey(),
            witnessCapsule);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final WitnessUpdateContract witnessUpdateContract = this.contract
          .unpack(WitnessUpdateContract.class);
      this.updateWitness(witnessUpdateContract);
      ret.setStatus(fee, code.SUCESS);
    } catch (final InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!this.contract.is(WitnessUpdateContract.class)) {
        throw new ContractValidateException(
            "contract type error,expected type [WitnessUpdateContract],real type[" + this.contract
                .getClass() + "]");
      }

      final WitnessUpdateContract contract = this.contract.unpack(WitnessUpdateContract.class);
      Preconditions.checkNotNull(contract.getOwnerAddress(), "OwnerAddress is null");
      if (this.dbManager.getWitnessStore().get(contract.getOwnerAddress().toByteArray()) == null) {
        throw new ContractValidateException("Witness not existed");
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
      throw new ContractValidateException(ex.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(WitnessUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
