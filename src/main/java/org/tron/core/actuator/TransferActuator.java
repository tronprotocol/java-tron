package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Transaction.Result.code;

public class TransferActuator extends AbstractActuator {


  TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {

    long fee = calcFee();
    try {
      TransferContract transferContract = null;
      transferContract = contract.unpack(TransferContract.class);
      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(),
          -transferContract.getAmount());
      dbManager.adjustBalance(transferContract.getToAddress().toByteArray(),
          transferContract.getAmount());

      dbManager.adjustBalance(transferContract.getOwnerAddress().toByteArray(), -calcFee());
      ret.setStatus(fee, code.SUCESS);

    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (BalanceInsufficientException e) {
      e.printStackTrace();
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!contract.is(TransferContract.class)) {
        throw new ContractValidateException(
            "contract type error,expected type [TransferContract],real type[" + contract
                .getClass() + "]");
      }

      TransferContract transferContract = this.contract.unpack(TransferContract.class);

      Preconditions.checkNotNull(transferContract.getOwnerAddress(), "OwnerAddress is null");
      Preconditions.checkNotNull(transferContract.getToAddress(), "ToAddress is null");
      Preconditions.checkNotNull(transferContract.getAmount(), "Amount is null");

      if (!dbManager.getAccountStore().has(transferContract.getOwnerAddress().toByteArray())) {
        throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
      }
      if (!dbManager.getAccountStore().has(transferContract.getToAddress().toByteArray())) {
        throw new ContractValidateException("Validate TransferContract error, no ToAccount.");
      }
      long amount = transferContract.getAmount();
      if (amount < 0) {
        throw new ContractValidateException("Amount is less than 0.");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
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
