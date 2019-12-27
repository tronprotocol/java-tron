package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.CrossContract;

@Slf4j(topic = "actuator")
public class CrossChainActuator extends AbstractActuator {

  public CrossChainActuator() {
    super(ContractType.CrossContract, CrossContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
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
