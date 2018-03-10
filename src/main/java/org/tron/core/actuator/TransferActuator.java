package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.TransferContract;

public class TransferActuator extends AbstractActuator {


  TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute() {
    try {
      TransferContract transferContract = contract.unpack(TransferContract.class);
      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(),
          -transferContract.getAmount());
      dbManager.adjustBalance(transferContract.getToAddress().toByteArray(),
          transferContract.getAmount());
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Parse contract error", e);
    }
    return true;
  }

  @Override
  public boolean validate() {
    try {
      if (!contract.is(TransferContract.class)) {
        throw new RuntimeException(
            "contract type error,expected type [TransferContract],real type[" + contract
                .getClass() + "]");
      }

      TransferContract transferContract = this.contract.unpack(TransferContract.class);

      Preconditions.checkNotNull(transferContract.getOwnerAddress(), "OwnerAddress is null");
      Preconditions.checkNotNull(transferContract.getToAddress(), "ToAddress is null");
      Preconditions.checkNotNull(transferContract.getAmount(), "Amount is null");

      if (!dbManager.getAccountStore().has(transferContract.getOwnerAddress().toByteArray())) {
        throw new RuntimeException("Validate TransferContract error, no OwnerAccount.");
      }
      if (!dbManager.getAccountStore().has(transferContract.getToAddress().toByteArray())) {
        throw new RuntimeException("Validate TransferContract error, no ToAccount.");
      }
      long amount = transferContract.getAmount();
      if (amount < 0) {
        throw new RuntimeException("Amount is less than 0.");
      }
    } catch (Exception ex) {
      throw new RuntimeException("Validate TransferContract error.", ex);
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() {
    try {
      if (contract.is(TransferContract.class)) {
        TransferContract transferContract = contract.unpack(TransferContract.class);
        return transferContract.getOwnerAddress();
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return null;
  }
}
