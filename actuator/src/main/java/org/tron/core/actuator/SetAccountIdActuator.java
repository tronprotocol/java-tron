package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.SetAccountIdContract;
import org.tron.core.store.AccountIdIndexStore;
import org.tron.core.store.AccountStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class SetAccountIdActuator extends AbstractActuator {

  SetAccountIdActuator(Any contract, AccountStore accountStore, AccountIdIndexStore accountIdIndexStore) {
    super(contract, accountStore, accountIdIndexStore);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    final SetAccountIdContract setAccountIdContract;
    final long fee = calcFee();
    try {
      setAccountIdContract = contract.unpack(SetAccountIdContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    byte[] ownerAddress = setAccountIdContract.getOwnerAddress().toByteArray();
    AccountCapsule account = accountStore.get(ownerAddress);

    account.setAccountId(setAccountIdContract.getAccountId().toByteArray());
    accountStore.put(ownerAddress, account);
    accountIdIndexStore.put(account);
    ret.setStatus(fee, code.SUCESS);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountIdIndexStore == null || accountStore == null) {
      throw new ContractValidateException("No account store or account id index store!");
    }
    if (!this.contract.is(SetAccountIdContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [SetAccountIdContract],real type[" + contract
              .getClass() + "]");
    }
    final SetAccountIdContract setAccountIdContract;
    try {
      setAccountIdContract = contract.unpack(SetAccountIdContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = setAccountIdContract.getOwnerAddress().toByteArray();
    byte[] accountId = setAccountIdContract.getAccountId().toByteArray();
    if (!TransactionUtil.validAccountId(accountId)) {
      throw new ContractValidateException("Invalid accountId");
    }
    if (!Commons.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    AccountCapsule account = accountStore.get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account has not existed");
    }
    if (account.getAccountId() != null && !account.getAccountId().isEmpty()) {
      throw new ContractValidateException("This account id already set");
    }
    if (accountIdIndexStore.has(accountId)) {
      throw new ContractValidateException("This id has existed");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(SetAccountIdContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}

