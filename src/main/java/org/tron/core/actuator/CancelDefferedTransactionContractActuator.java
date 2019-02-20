package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.DeferredTransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.CancelDefferedTransactionContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class CancelDefferedTransactionContractActuator extends AbstractActuator {
  CancelDefferedTransactionContractActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule capsule) throws ContractExeException {
    long fee = calcFee();
    final CancelDefferedTransactionContract cancelDefferedTransactionContract;
    // todo calculate fee
    try {
      cancelDefferedTransactionContract = this.contract.unpack(CancelDefferedTransactionContract.class);
      dbManager.cancelDeferredTransaction(cancelDefferedTransactionContract.getTransactionId());
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
    if (!this.contract.is(CancelDefferedTransactionContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [CancelDefferedTransactionContract],real type[" + contract
              .getClass() + "]");
    }

    final CancelDefferedTransactionContract cancelDefferedTransactionContract;
    try {
      cancelDefferedTransactionContract = this.contract.unpack(CancelDefferedTransactionContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    ByteString trxId = cancelDefferedTransactionContract.getTransactionId();
    DeferredTransactionCapsule capsule = dbManager.getDeferredTransactionStore().getByTransactionId(trxId);
    if (Objects.isNull(capsule)) {
      throw new ContractValidateException("No deferred transaction!");
    }

    ByteString sendAddress = capsule.getSenderAddress();
    ByteString ownerAddress = cancelDefferedTransactionContract.getOwnerAddress();
    if (sendAddress.equals(ownerAddress) == false) {
      throw new ContractValidateException("not have right to cancel!");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(CancelDefferedTransactionContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return ChainConstant.TRANSFER_FEE;
  }
}
