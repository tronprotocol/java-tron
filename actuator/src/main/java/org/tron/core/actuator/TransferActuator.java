package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.args.Parameter.ChainConstant;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class TransferActuator extends AbstractActuator {

  TransferActuator(Any contract, AccountStore accountStore, AssetIssueStore assetIssueStore, DynamicPropertiesStore dynamicStore) {
    super(contract, accountStore, assetIssueStore, dynamicStore);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      TransferContract transferContract = contract.unpack(TransferContract.class);
      long amount = transferContract.getAmount();
      byte[] toAddress = transferContract.getToAddress().toByteArray();
      byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();

      // if account with to_address does not exist, create it first.
      AccountCapsule toAccount = accountStore.get(toAddress);
      if (toAccount == null) {
        boolean withDefaultPermission =
            dynamicStore.getAllowMultiSign() == 1;
        toAccount = new AccountCapsule(ByteString.copyFrom(toAddress), AccountType.Normal,
            dynamicStore.getLatestBlockHeaderTimestamp(), withDefaultPermission, dynamicStore, assetIssueStore);
        accountStore.put(toAddress, toAccount);

        fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      }
      Commons.adjustBalance(accountStore, ownerAddress, -fee);
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);
      ret.setStatus(fee, code.SUCESS);
      Commons.adjustBalance(accountStore, ownerAddress, -amount);
      Commons.adjustBalance(accountStore, toAddress, amount);
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

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (accountStore == null || dynamicStore == null) {
      throw new ContractValidateException("No account store or dynamic store!");
    }
    if (!this.contract.is(TransferContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [TransferContract],real type[" + contract
              .getClass() + "]");
    }
    long fee = calcFee();
    final TransferContract transferContract;
    try {
      transferContract = contract.unpack(TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] toAddress = transferContract.getToAddress().toByteArray();
    byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();
    long amount = transferContract.getAmount();

    if (!Commons.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (!Commons.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }

    if (Arrays.equals(toAddress, ownerAddress)) {
      throw new ContractValidateException("Cannot transfer trx to yourself.");
    }

    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
    }

    long balance = ownerAccount.getBalance();

    if (amount <= 0) {
      throw new ContractValidateException("Amount must greater than 0.");
    }

    try {
      AccountCapsule toAccount = accountStore.get(toAddress);
      if (toAccount == null) {
        fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      }

      if (balance < Math.addExact(amount, fee)) {
        throw new ContractValidateException(
            "Validate TransferContract error, balance is not sufficient.");
      }

      if (toAccount != null) {
        long toAddressBalance = Math.addExact(toAccount.getBalance(), amount);
      }
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(TransferContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return ChainConstant.TRANSFER_FEE;
  }

}