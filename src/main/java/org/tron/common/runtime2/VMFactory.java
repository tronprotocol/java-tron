package org.tron.common.runtime2;

import org.tron.common.runtime2.config.VMConfig;
import org.tron.common.runtime2.tvm.TVM;
import org.tron.common.storage.Deposit;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.protos.Protocol;

public class VMFactory {
  public static IVM createTVM(VMConfig config, TransactionTrace trace, BlockCapsule block, Deposit deposit) {
    TVM tvm = new TVM(trace, null, block, deposit);
    tvm.setVmConfig(config);
    return tvm;
  }

  public static IVM createTVM(VMConfig config, Protocol.Transaction trx, BlockCapsule block, Deposit deposit) {
    TVM tvm = new TVM(null, trx, block, deposit);
    tvm.setVmConfig(config);
    return tvm;
  }
}
