package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.utils.TransactionUtil;
import org.tron.core.db.AccountIndexStore;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UpdateAccountActuator extends AbstractActuator {

  AccountUpdateContract accountUpdateContract;
  byte[] accountName;
  byte[] ownerAddress;
  long fee;

  UpdateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
    try {
      accountUpdateContract = contract.unpack(AccountUpdateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.error(e.getMessage(), e);
    }
    accountName = accountUpdateContract.getAccountName().toByteArray();
    ownerAddress = accountUpdateContract.getOwnerAddress().toByteArray();
    fee = calcFee();
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountIndexStore accountIndexStore = dbManager.getAccountIndexStore();
    AccountCapsule account = accountStore.get(ownerAddress);

    account.setAccountName(accountName);
    accountStore.put(ownerAddress, account);
    accountIndexStore.put(account);

    ret.setStatus(fee, code.SUCESS);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (accountUpdateContract == null) {
      throw new ContractValidateException(
          "contract type error,expected type [AccountUpdateContract],real type[" + contract
              .getClass() + "]");
    }

    if (!TransactionUtil.validAccountName(accountName)) {
      throw new ContractValidateException("Invalidate accountName");
    }
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalidate ownerAddress");
    }

    AccountCapsule account = dbManager.getAccountStore().get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account has not existed");
    }
    if (account.getAccountName() != null && !account.getAccountName().isEmpty()) {
      throw new ContractValidateException("This account name already exist");
    }
    if (dbManager.getAccountIndexStore().has(accountName)) {
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
