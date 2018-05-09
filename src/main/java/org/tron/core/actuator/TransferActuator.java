package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class TransferActuator extends AbstractActuator {

  TransferContract transferContract;
  byte[] ownerAddress;
  byte[] toAddress;
  long amount;

  TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
    try {
      transferContract = contract.unpack(TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.error(e.getMessage(), e);
    }
    amount = transferContract.getAmount();
    toAddress = transferContract.getToAddress().toByteArray();
    ownerAddress = transferContract.getOwnerAddress().toByteArray();
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {

    long fee = calcFee();
    try {

      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(), -calcFee());
      ret.setStatus(fee, code.SUCESS);
      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(),
          -amount);
      dbManager.adjustBalance(transferContract.getToAddress().toByteArray(),
          amount);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (transferContract == null) {
        throw new ContractValidateException(
            "contract type error,expected type [TransferContract],real type[" + contract
                .getClass() + "]");
      }
      if (!Wallet.addressValid(ownerAddress)) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }
      if (!Wallet.addressValid(toAddress)) {
        throw new ContractValidateException("Invalidate toAddress");
      }

      Preconditions.checkNotNull(transferContract.getAmount(), "Amount is null");

      if (Arrays.equals(toAddress, ownerAddress)) {
        throw new ContractValidateException("Cannot transfer trx to yourself.");
      }

      AccountCapsule ownerAccount = dbManager.getAccountStore()
          .get(transferContract.getOwnerAddress().toByteArray());

      if (ownerAccount == null) {
        throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
      }

      long balance = ownerAccount.getBalance();

      if (ownerAccount.getBalance() < calcFee()) {
        throw new ContractValidateException("Validate TransferContract error, insufficient fee.");
      }

      if (amount <= 0) {
        throw new ContractValidateException("Amount must greater than 0.");
      }

      if (balance < Math.addExact(amount, calcFee())) {
        throw new ContractValidateException("balance is not sufficient.");
      }

      // if account with to_address is not existed,  create it.
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(transferContract.getToAddress().toByteArray());
      if (toAccount == null) {
        long min = dbManager.getDynamicPropertiesStore().getNonExistentAccountTransferMin();
        if (amount < min) {
          throw new ContractValidateException(
              "For a non-existent account transfer, the minimum amount is 1 TRX");
        }
        toAccount = new AccountCapsule(ByteString.copyFrom(toAddress), AccountType.Normal,
            dbManager.getHeadBlockTimeStamp());
        dbManager.getAccountStore().put(toAddress, toAccount);
      } else {
        //check to account balance if overflow
        balance = Math.addExact(toAccount.getBalance(), amount);
      }
    } catch (Exception ex) {
      throw new ContractValidateException(ex.getMessage());
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
