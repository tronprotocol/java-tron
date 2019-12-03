package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AccountContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;

@Slf4j(topic = "actuator")
public class ExampleActuator extends AbstractActuator {

  public ExampleActuator() {
    super(null, null);
    logger.info("ExampleActuator construct successfully.");
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    logger.info("ExampleActuator execute successfully.");
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    logger.info("ExampleActuator validate successfully.");
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
