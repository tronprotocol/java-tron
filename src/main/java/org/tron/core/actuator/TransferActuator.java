package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocal.Account;

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
        ByteString ownerAddress = transferContract.getOwnerAddress();
        AccountCapsule ownerAccount = dbManager.getAccountStore().get(ownerAddress.toByteArray());
        if (ownerAccount == null) {
          return false;
        }

        long amount = transferContract.getAmount();
        if (ownerAccount.getBalance() < amount) {
          return false;
        }
        ByteString toAddress = transferContract.getToAddress();
        AccountCapsule toAccount = dbManager.getAccountStore().get(toAddress.toByteArray());
        if (toAccount == null) {
          return false;
        }
        ownerAccount.setBalance(ownerAccount.getBalance() - amount);
        dbManager.getAccountStore().put(ownerAddress.toByteArray(), ownerAccount);

        toAccount = dbManager.getAccountStore().get(toAddress.toByteArray());
        toAccount.setBalance(toAccount.getBalance() + amount);
        dbManager.getAccountStore().put(toAddress.toByteArray(), toAccount);
        return true;
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean validate() {
    //TODO
    return false;
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
