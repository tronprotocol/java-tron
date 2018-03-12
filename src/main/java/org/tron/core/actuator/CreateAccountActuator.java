package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountCreateContract;

public class CreateAccountActuator extends AbstractActuator {


  CreateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute() throws ContractExeException {
    try {
      AccountCreateContract accountCreateContract = contract.unpack(AccountCreateContract.class);
      AccountCapsule accountCapsule = new AccountCapsule(accountCreateContract);
      dbManager.getAccountStore()
          .put(accountCreateContract.getOwnerAddress().toByteArray(), accountCapsule);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!contract.is(AccountCreateContract.class)) {
        throw new ContractValidateException(
            "contract type error,expected type [AccountCreateContract],real type[" + contract
                .getClass() + "]");
      }

      AccountCreateContract contract = this.contract.unpack(AccountCreateContract.class);

      Preconditions.checkNotNull(contract.getAccountName(), "AccountName is null");
      Preconditions.checkNotNull(contract.getOwnerAddress(), "OwnerAddress is null");
      Preconditions.checkNotNull(contract.getType(), "Type is null");

      if (dbManager.getAccountStore().has(contract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException("Account has existed");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ContractValidateException(ex.getMessage());
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AccountCreateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
