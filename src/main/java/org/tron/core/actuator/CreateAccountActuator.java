package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Protocol.AccountType;

public class CreateAccountActuator extends AbstractActuator {


  CreateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute() {
    try {
      if (contract.is(AccountCreateContract.class)) {
        AccountCreateContract accountCreateContract = contract.unpack(AccountCreateContract.class);
        ByteString ownerAddress = accountCreateContract.getOwnerAddress();
        ByteString accountName = accountCreateContract.getAccountName();
        AccountType type = accountCreateContract.getType();
        int typeValue = accountCreateContract.getTypeValue();
        if (null != dbManager) {
          boolean accountExist = dbManager.getAccountStore()
              .isAccountExist(ownerAddress.toByteArray());
          if (null != accountName && !accountExist) {
            AccountCapsule accountCapsule = new AccountCapsule(ownerAddress, accountName, type,
                typeValue);
            dbManager.getAccountStore().createAccount(ownerAddress.toByteArray(), accountCapsule);
          }
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }

    return true;
  }

  @Override
  public boolean validator() {
    //TODO
    return false;
  }

  @Override
  public ByteString getOwnerAddress() {
    try {
      return contract.unpack(AccountCreateContract.class).getOwnerAddress();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return null;
  }
}
