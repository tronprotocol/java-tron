package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.CrossContract;

@Slf4j(topic = "actuator")
public class CrossChainActuator extends AbstractActuator {

  public CrossChainActuator() {
    super(ContractType.CrossContract, CrossContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    try {
      CrossContract crossContract = any.unpack(CrossContract.class);
      chainBaseManager.getCrossStore().getReceiveCrossMsgUnEx(tx.getTransactionId());
      ret.setStatus(calcFee(), code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(calcFee(), code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!chainBaseManager.getDynamicPropertiesStore().allowCrossChain()) {
      return false;
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
