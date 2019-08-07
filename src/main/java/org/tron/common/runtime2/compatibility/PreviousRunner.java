package org.tron.common.runtime2.compatibility;

import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.runtime2.TxRunner;
import org.tron.common.runtime2.config.VMConfig;
import org.tron.common.storage.Deposit;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.VMIllegalException;

public class PreviousRunner implements TxRunner {
  TransactionTrace transactionTrace;
  RuntimeImpl previousImpl;

  public static PreviousRunner createPreviouRunner(TransactionTrace trace, BlockCapsule block, Deposit deposit, VMConfig config) {
    PreviousRunner ret = new PreviousRunner(trace, block, deposit);
    ret.previousImpl.setEnableEventLinstener(config.isEventPluginLoaded());
    return ret;
  }

  public static PreviousRunner createPreviouRunnerForWallet(TransactionCapsule txc, BlockCapsule block, Deposit deposit) {
    PreviousRunner ret = new PreviousRunner(txc, block, deposit);
    ret.previousImpl.setStaticCall(true);
    return ret;
  }


  private PreviousRunner(TransactionTrace trace, BlockCapsule block, Deposit deposit) {
    this.transactionTrace = trace;
    previousImpl = new RuntimeImpl(trace, block, deposit, new ProgramInvokeFactoryImpl());
  }

  private PreviousRunner(TransactionCapsule tx, BlockCapsule block, Deposit deposit) {
    previousImpl = new RuntimeImpl(tx.getInstance(), block, deposit, new ProgramInvokeFactoryImpl());
  }

  @Override
  public void execute() throws ContractValidateException, ContractExeException, VMIllegalException {
    if (!previousImpl.isStaticCall() && transactionTrace != null) {
      transactionTrace.checkIsConstant();
    }
    /*  VM execute  */
    previousImpl.execute();
    previousImpl.go();
    //set runtimeError into result
    previousImpl.getResult().setRuntimeError(previousImpl.getRuntimeError());


  }

  @Override
  public ProgramResult getResult() {
    return previousImpl.getResult();
  }



  @Override
  public void finalization() {
    previousImpl.finalization();

  }
}
