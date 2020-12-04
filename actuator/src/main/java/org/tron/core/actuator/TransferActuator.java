package org.tron.core.actuator;

import static org.tron.core.config.args.Parameter.ChainConstant.TRANSFER_FEE;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountBalanceCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountBalanceStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.TransferContract;

@Slf4j(topic = "actuator")
public class TransferActuator extends AbstractActuator {

  public TransferActuator() {
    super(ContractType.TransferContract, TransferContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule)object;
    if (Objects.isNull(ret)){
      throw new RuntimeException("TransactionResultCapsule is null");
    }

    long fee = calcFee();
    AccountBalanceStore accountBalanceStore = chainBaseManager.getAccountBalanceStore();
    try {
      TransferContract transferContract = any.unpack(TransferContract.class);
      long amount = transferContract.getAmount();
      byte[] toAddress = transferContract.getToAddress().toByteArray();
      byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();

      // if account with to_address does not exist, create it first.
      AccountBalanceCapsule toAccountBalance = accountBalanceStore.get(toAddress);
      DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
      checkToAccount(toAddress, dynamicStore);
      if (toAccountBalance == null) {
        boolean withDefaultPermission =
                dynamicStore.getAllowMultiSign() == 1;
        toAccountBalance = new AccountBalanceCapsule(
                ByteString.copyFrom(toAddress),
                AccountType.Normal,
                withDefaultPermission);
        accountBalanceStore.put(toAddress, toAccountBalance);

        fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      }
      Commons.adjustBalance(accountBalanceStore, ownerAddress, -fee);
      Commons.adjustBalance(accountBalanceStore, accountBalanceStore.getBlackhole().createDbKey(), fee);
      ret.setStatus(fee, code.SUCESS);
      Commons.adjustBalance(accountBalanceStore, ownerAddress, -amount);
      Commons.adjustBalance(accountBalanceStore, toAddress, amount);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  private void checkToAccount(byte[] toAddress, DynamicPropertiesStore dynamicStore) {
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AccountCapsule toAccount = accountStore.get(toAddress);
    if (toAccount == null) {
      boolean withDefaultPermission =
              dynamicStore.getAllowMultiSign() == 1;
      toAccount = new AccountCapsule(ByteString.copyFrom(toAddress), AccountType.Normal,
              dynamicStore.getLatestBlockHeaderTimestamp(), withDefaultPermission, dynamicStore);
      accountStore.put(toAddress, toAccount);
    }
  }


  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException("No contract!");
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or dynamic store!");
    }
    AccountBalanceStore accountBalanceStore = chainBaseManager.getAccountBalanceStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    if (!this.any.is(TransferContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [TransferContract], real type [" + contract
              .getClass() + "]");
    }
    long fee = calcFee();
    final TransferContract transferContract;
    try {
      transferContract = any.unpack(TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] toAddress = transferContract.getToAddress().toByteArray();
    byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();
    long amount = transferContract.getAmount();

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress!");
    }
    if (!DecodeUtil.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress!");
    }

    if (Arrays.equals(toAddress, ownerAddress)) {
      throw new ContractValidateException("Cannot transfer TRX to yourself.");
    }

    AccountBalanceCapsule ownerAccount = accountBalanceStore.get(ownerAddress);

    if (ownerAccount == null) {
      throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
    }

    long balance = ownerAccount.getBalance();

    if (amount <= 0) {
      throw new ContractValidateException("Amount must be greater than 0.");
    }

    try {
      AccountBalanceCapsule toAccount = accountBalanceStore.get(toAddress);
      if (toAccount == null) {
        fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      }
      //after ForbidTransferToContract proposal, send trx to smartContract by actuator is not allowed.
      if (dynamicStore.getForbidTransferToContract() == 1
          && toAccount != null
          && toAccount.getType() == AccountType.Contract) {

        throw new ContractValidateException("Cannot transfer TRX to a smartContract.");

      }

      if (balance < Math.addExact(amount, fee)) {
        throw new ContractValidateException(
            "Validate TransferContract error, balance is not sufficient.");
      }

      if (toAccount != null) {
        Math.addExact(toAccount.getBalance(), amount);
      }
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(TransferContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return TRANSFER_FEE;
  }

}