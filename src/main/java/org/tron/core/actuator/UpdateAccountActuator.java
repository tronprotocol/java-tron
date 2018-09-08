package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.utils.TransactionUtil;
import org.tron.core.db.AccountIdIndexStore;
import org.tron.core.db.AccountIndexStore;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UpdateAccountActuator extends AbstractActuator {

  UpdateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    final AccountUpdateContract accountUpdateContract;
    final long fee = calcFee();
    try {
      accountUpdateContract = contract.unpack(AccountUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    byte[] ownerAddress = accountUpdateContract.getOwnerAddress().toByteArray();
    AccountStore accountStore = dbManager.getAccountStore();
    AccountIndexStore accountIndexStore = dbManager.getAccountIndexStore();
    AccountCapsule account = accountStore.get(ownerAddress);

    account.setAccountName(accountUpdateContract.getAccountName().toByteArray());
    accountStore.put(ownerAddress, account);
    accountIndexStore.put(account);

    ret.setStatus(fee, code.SUCESS);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(AccountUpdateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [AccountUpdateContract],real type[" + contract
              .getClass() + "]");
    }
    final AccountUpdateContract accountUpdateContract;
    try {
      accountUpdateContract = contract.unpack(AccountUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = accountUpdateContract.getOwnerAddress().toByteArray();
    byte[] accountName = accountUpdateContract.getAccountName().toByteArray();
    if (!TransactionUtil.validAccountName(accountName)) {
      throw new ContractValidateException("Invalid accountName");
    }
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    AccountCapsule account = dbManager.getAccountStore().get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account has not existed");
    }

    if (account.getAccountName() != null && !account.getAccountName().isEmpty()
        && dbManager.getDynamicPropertiesStore().getAllowUpdateAccountName() == 0) {
      throw new ContractValidateException("This account name already exist");
    }

    if (dbManager.getAccountIndexStore().has(accountName)
        && dbManager.getDynamicPropertiesStore().getAllowUpdateAccountName() == 0) {
      throw new ContractValidateException("This name has existed");
    }

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
