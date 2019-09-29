package org.tron.common.runtime2.tvm.interpretor.executors;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime.vm.program.Stack;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Costs;
import org.tron.common.runtime2.tvm.interpretor.Op;

public class LogOpExecutor extends OpExecutor {

  private static LogOpExecutor INSTANCE = new LogOpExecutor();

  private LogOpExecutor() {
  }

  public static LogOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    int nTopics = op.val() - OpCode.LOG0.val();
    Stack stack = executor.getStack();
    DataWord address = executor.getContractAddress();
    DataWord memStart = stack.pop();
    DataWord dataSize = stack.pop();
    //is it nessary?
/*    BigInteger dataCost = dataSize.value()
        .multiply(BigInteger.valueOf(Costs.LOG_DATA_ENERGY));

    if (executor.getContractContext().getEnergyLimitLeft().value().compareTo(dataCost) < 0) {
      throw new org.tron.common.runtime.vm.program.Program.OutOfEnergyException(
          "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
          op.name(),
          dataCost.longValueExact(),
          executor.getContractContext().getEnergyLimitLeft().longValueSafe());
    }*/
    BigInteger memNeeded = memNeeded(memStart, dataSize);
    long energyCost = Costs.LOG_ENERGY
        + Costs.LOG_TOPIC_ENERGY * nTopics
        + Costs.LOG_DATA_ENERGY * dataSize.longValue()
        + calcMemEnergy(executor.getMemory().size(),
        memNeeded, 0, op);

    executor.spendEnergy(energyCost, op.name());
    checkMemorySize(op, memNeeded);

    List<DataWord> topics = new ArrayList<>();
    for (int i = 0; i < nTopics; ++i) {
      DataWord topic = stack.pop();
      topics.add(topic);
    }

    byte[] data = executor.memoryChunk(memStart.intValueSafe(), dataSize.intValueSafe());

    LogInfo logInfo =
        new LogInfo(address.getLast20Bytes(), topics, data);

    executor.getContractContext().getProgramResult().addLogInfo(logInfo);
    executor.step();

  }
}
