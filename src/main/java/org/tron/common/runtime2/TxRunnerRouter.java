package org.tron.common.runtime2;

import org.tron.common.runtime2.actuator.ActuatorRunner;
import org.tron.common.runtime2.compatibility.PreviousRunner;
import org.tron.common.runtime2.config.VMConfig;
import org.tron.common.storage.Deposit;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


public class TxRunnerRouter {
  private TxRunnerRouter() {
  }

  private static class TxRunnerRouterInstance {
    private static final TxRunnerRouter instance = new TxRunnerRouter();
  }

  public static TxRunnerRouter getInstance() {
    return TxRunnerRouterInstance.instance;
  }

  public TxRunner routeWallet(TransactionCapsule trxCapsule, BlockCapsule block,
                              Deposit deposit, VMConfig config) {
    if (!config.isSwitchVm2()) {
      return PreviousRunner.createPreviouRunnerForWallet(trxCapsule, block, deposit);
    } else {
      switch (trxCapsule.getInstance().getRawData().getContract(0).getType().getNumber()) {
        case ContractType.TriggerSmartContract_VALUE:
        case ContractType.CreateSmartContract_VALUE:
          return VMFactory.createTVMForWallet(config, trxCapsule, block, deposit);
        default:
          return ActuatorRunner.createActuatorRunner(trxCapsule.getInstance(), deposit);

      }
    }
  }


  public TxRunner route(TransactionTrace trace, BlockCapsule block,
                        Deposit deposit, VMConfig config) {
    if (!config.isSwitchVm2()) {
      //no need to route ,run previous runtime
      return PreviousRunner.createPreviouRunner(trace, block, deposit, config);

    } else {
      switch (trace.getTrx().getInstance().getRawData().getContract(0).getType().getNumber()) {
        case ContractType.TriggerSmartContract_VALUE:
        case ContractType.CreateSmartContract_VALUE:
          return VMFactory.createTVM(config, trace.getTrx(), block, deposit);
        default:
          return ActuatorRunner.createActuatorRunner(trace.getTrx(), deposit);

      }
    }
  }

}
