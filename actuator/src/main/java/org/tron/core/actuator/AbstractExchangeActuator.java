package org.tron.core.actuator;

import com.google.protobuf.GeneratedMessageV3;
import org.tron.common.math.StrictMathWrapper;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public abstract class AbstractExchangeActuator extends AbstractActuator {

  public AbstractExchangeActuator(ContractType type, Class<? extends GeneratedMessageV3> clazz) {
    super(type, clazz);
  }

  protected boolean allowHarden() {
    return chainBaseManager.getDynamicPropertiesStore().allowHardenExchangeCalculation();
  }

  public long subtractExact(long x, long y) {
    return allowHarden() ? StrictMathWrapper.subtractExact(x, y) : x - y;
  }

  public long addExact(long x, long y) {
    return allowHarden() ? StrictMathWrapper.addExact(x, y) : x + y;
  }
}
