package org.tron.common.runtime2.actuator;

import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime2.TxRunner;
import org.tron.common.storage.Deposit;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;

import java.util.List;

public class ActualRunner implements TxRunner {

  ProgramResult result = new ProgramResult();

  TransactionCapsule trxCap;

  Deposit deposit;

  private ActualRunner(TransactionCapsule trxCap, Deposit deposit) {
    this.trxCap = trxCap;
    this.deposit = deposit;
  }

  public static ActualRunner createActualRunner(TransactionCapsule trxCap, Deposit deposit) {
    return new ActualRunner(trxCap, deposit);
  }

  public static ActualRunner createActualRunner(Protocol.Transaction tx, Deposit deposit) {
    return new ActualRunner(new TransactionCapsule(tx), deposit);
  }


  @Override
  public void execute() throws ContractValidateException, ContractExeException {
    final List<Actuator> actuatorList = ActuatorFactory
            .createActuator(trxCap, deposit.getDbManager());

    for (Actuator act : actuatorList) {
      act.validate();
      act.execute(result.getRet());
    }
    deposit.commit();

  }

  @Override
  public ProgramResult getResult() {
    return result;
  }

  @Override
  public void finalization() {

  }
}
