package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountUpdateContract;

@Slf4j
public class UpdateAccountActuator extends AbstractActuator {

  UpdateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule result) throws ContractExeException {
    long fee = calcFee();
    try {

      AccountUpdateContract accountUpdateContract = contract.unpack(AccountUpdateContract.class);
      AccountCapsule account =
          dbManager.getAccountStore().get(accountUpdateContract.getOwnerAddress().toByteArray());

      account.setAccountName(accountUpdateContract.getAccountName().toByteArray());
      dbManager.getAccountStore().put(accountUpdateContract.getOwnerAddress().toByteArray(),
          account);
      return true;
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractExeException(e.getMessage());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
    // todo validate freq.
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AccountUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
