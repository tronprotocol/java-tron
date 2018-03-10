package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferContract;

public class TransferActuator extends AbstractActuator {


  TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute() {
    try {
      if (contract.is(TransferContract.class)) {
        if (dbManager == null) {
          return false;
        }
        TransferContract transferContract = contract.unpack(TransferContract.class);
        byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();
        if (!dbManager.getAccountStore().has(ownerAddress)) {
          return false;
        }
        byte[] toAddress = transferContract.getToAddress().toByteArray();
        if (!dbManager.getAccountStore().has(toAddress)) {
          return false;
        }
        long amount = transferContract.getAmount();
        dbManager.adjustBalance(ownerAddress, -amount);
        dbManager.adjustBalance(toAddress, amount);
        return true;
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    } catch (BalanceInsufficientException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    //TODO
    if (contract.is(TransferContract.class)) {
      try {
        TransferContract transferContract = contract.unpack(TransferContract.class);

      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
      }
    } else {
      throw new ContractValidateException("wrong transfer type");
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

  @Override
  public long calcFee() {
    return 0;
  }
}
