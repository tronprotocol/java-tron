package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Contract;
import org.tron.protos.Protocal;

public class TransferActuator extends AbstractActuator {


  TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute() {
    try {
      if (contract.is(Contract.TransferContract.class)) {
        Contract.TransferContract transferContract = contract.unpack(Contract.TransferContract.class);
        ByteString ownerAddress = transferContract.getOwnerAddress();
        ByteString toAddress = transferContract.getOwnerAddress();
        long amount = transferContract.getAmount();

        if (dbManager == null) {
          return false;
        }

        Protocal.Account ownerAccount = dbManager.getAccountStore().getAccount(ownerAddress.toByteArray());
        if (ownerAccount == null) {
          return false;
        }
        if (ownerAccount.getBalance() < amount) {
          return false;
        }
        Protocal.Account toAccount = dbManager.getAccountStore().getAccount(toAddress.toByteArray());
        if (toAccount == null) {
          return false;
        }
        Protocal.Account.Builder ownerBuilder = ownerAccount.toBuilder();
        ownerBuilder.setBalance(ownerAccount.getBalance() - amount);
        ownerAccount = ownerBuilder.build();
        Protocal.Account.Builder toBuilder = toAccount.toBuilder();
        toBuilder.setBalance(toAccount.getBalance() + amount);
        toAccount = toBuilder.build();
        dbManager.getAccountStore().putAccount(new AccountCapsule(ownerAccount));
        dbManager.getAccountStore().putAccount(new AccountCapsule(toAccount));
        return true;
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean validator() {
    //TODO
    return false;
  }

  @Override
  public ByteString getOwnerAddress() {
    try {
      if (contract.is(Contract.TransferContract.class)) {
        Contract.TransferContract transferContract = contract.unpack(Contract.TransferContract.class);
        return transferContract.getOwnerAddress();
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return null;
  }
}
