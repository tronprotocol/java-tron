package org.tron.common.runtime;

import java.util.List;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.Actuator2;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.actuator.ActuatorRuntime;
import org.tron.core.actuator.VMActuator;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionContext;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class ActuatorRuntimeImpl implements ActuatorRuntime {

  Manager dbManger = null;

  List<Actuator> actuatorList = null;

  Actuator2 actuator2 = null;

  public ActuatorRuntimeImpl(Manager manager) {
    this.dbManger = manager;
  }

  @Override
  public void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException {
    ContractType contractType = context.getTrxCap().getInstance().getRawData().getContract(0)
        .getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
      case ContractType.CreateSmartContract_VALUE:
        actuator2 = new VMActuator(context.isStatic());
        break;
      default:
        actuatorList = ActuatorFactory.createActuator(context.getTrxCap(), dbManger);
    }
    if (actuator2 != null) {
      actuator2.execute(context);
    } else {
      for (Actuator act : actuatorList) {
        act.validate();
        act.execute(context.getProgramResult().getRet());
      }
    }

  }
}
