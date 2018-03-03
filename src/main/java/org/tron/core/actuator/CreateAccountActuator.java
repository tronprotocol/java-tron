package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Protocal.Account;
import org.tron.protos.Protocal.AccountType;

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
            Account account = Account.newBuilder().setAddress(ownerAddress)
                .setAccoutName(accountName).setType(type).setTypeValue(typeValue).build();
            AccountStore accountStore = dbManager.getAccountStore();
            accountStore.createAccount(ownerAddress.toByteArray(), account.toByteArray());
          }
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }

    return true;
  }

  @Override
  public boolean Validator() {
    //TODO
    return false;
  }
}
