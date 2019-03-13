package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.DeferredTransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.CancelDeferredTransactionContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class CancelDeferredTransactionContractActuator extends AbstractActuator {
  CancelDeferredTransactionContractActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule capsule) throws ContractExeException {
    long fee = calcFee();
    final CancelDeferredTransactionContract cancelDeferredTransactionContract;
    try {
      cancelDeferredTransactionContract = this.contract.unpack(CancelDeferredTransactionContract.class);
      dbManager.cancelDeferredTransaction(cancelDeferredTransactionContract.getTransactionId());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      capsule.setStatus(fee, code.FAILED);
    }

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
    if (!this.contract.is(CancelDeferredTransactionContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [CancelDeferredTransactionContract],real type[" + contract
              .getClass() + "]");
    }

    final CancelDeferredTransactionContract cancelDeferredTransactionContract;
    try {
      cancelDeferredTransactionContract = this.contract.unpack(CancelDeferredTransactionContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    ByteString trxId = cancelDeferredTransactionContract.getTransactionId();
    DeferredTransactionCapsule capsule = dbManager.getDeferredTransactionCache().getByTransactionId(trxId);
    if (Objects.isNull(capsule)) {
      throw new ContractValidateException("No deferred transaction!");
    }

    ByteString sendAddress = capsule.getSenderAddress();
    if (Objects.isNull(sendAddress)) {
      throw new ContractValidateException("send address is null!");
    }
    
    ByteString ownerAddress = cancelDeferredTransactionContract.getOwnerAddress();
    if (sendAddress.equals(ownerAddress) == false) {
      throw new ContractValidateException("not have right to cancel!");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(CancelDeferredTransactionContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return dbManager.getDynamicPropertiesStore().getCancelDeferredTransactionFee();
  }
}
