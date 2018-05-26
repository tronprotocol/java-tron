package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class CreateAccountActuator extends AbstractActuator {

  @Deprecated
    //Can not create account by api. Need send more than 1 trx , will create account if not exit.
  CreateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  @Deprecated
  //Can not create account by api. Need send more than 1 trx , will create account if not exit.
  public boolean execute(TransactionResultCapsule ret)
      throws ContractExeException {
    long fee = calcFee();
    try {
      AccountCreateContract accountCreateContract = contract.unpack(AccountCreateContract.class);
      AccountCapsule accountCapsule = new AccountCapsule(accountCreateContract,
          dbManager.getHeadBlockTimeStamp());
      dbManager.getAccountStore()
          .put(accountCreateContract.getOwnerAddress().toByteArray(), accountCapsule);
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  @Deprecated
  //Can not create account by api. Need send more than 1 trx , will create account if not exit.
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!contract.is(AccountCreateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [AccountCreateContract],real type[" + contract
              .getClass() + "]");
    }
    final AccountCreateContract contract;
    try {
      contract = this.contract.unpack(AccountCreateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (contract.getAccountName().isEmpty()) {
      throw new ContractValidateException("AccountName is null");
    }
    if (!Wallet.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    if (contract.getType() == null) {
      throw new ContractValidateException("Type is null");
    }

    if (dbManager.getAccountStore().has(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Account has existed");
    }

    return true;
  }

  @Override
  @Deprecated
  //Can not create account by api. Need send more than 1 trx , will create account if not exit.
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AccountCreateContract.class).getOwnerAddress();
  }

  @Override
  @Deprecated
  //Can not create account by api. Need send more than 1 trx , will create account if not exit.
  public long calcFee() {
    return 0;
  }
}
