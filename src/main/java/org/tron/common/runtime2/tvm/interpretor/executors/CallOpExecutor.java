package org.tron.common.runtime2.tvm.interpretor.executors;


import java.math.BigInteger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.MessageCall;
import org.tron.common.runtime.vm.PrecompiledContracts;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.interpretor.Costs;
import org.tron.common.runtime2.tvm.interpretor.Op;

public class CallOpExecutor extends OpExecutor {

  private static CallOpExecutor INSTANCE = new CallOpExecutor();

  private CallOpExecutor() {
  }

  public static CallOpExecutor getInstance() {
    return INSTANCE;
  }


  @Override
  public void exec(Op op, ContractExecutor executor) {
    long oldMemSize = executor.getMemory().size();
    long energyCost = Costs.CALL;
    DataWord callEnergyWord = executor.stackPop();
    DataWord codeAddress = executor.stackPop();
    DataWord value;
    if (op == Op.CALL || op == Op.CALLCODE || op == Op.CALLTOKEN) {
      value = executor.stackPop();
    } else {
      value = DataWord.ZERO.clone();
    }
    if (op == Op.CALL || op == Op.CALLTOKEN) {
      energyCost += Costs.NEW_ACCT_CALL;
    }
    if (!value.isZero()) {
      energyCost += Costs.VT_CALL;
    }
    DataWord tokenId = new DataWord(0);

    boolean isTokenTransferMsg = false;
    if (op == Op.CALLTOKEN) {
      tokenId = executor.stackPop();
      isTokenTransferMsg = true;
    }

    DataWord inDataOffs = executor.stackPop();
    DataWord inDataSize = executor.stackPop();
    DataWord outDataOffs = executor.stackPop();
    DataWord outDataSize = executor.stackPop();

    BigInteger in = memNeeded(inDataOffs, inDataSize); // in offset+size
    BigInteger out = memNeeded(outDataOffs, outDataSize);// out offset+size

    energyCost += calcMemEnergy(oldMemSize, in.max(out), 0, op);

    executor.spendEnergy(energyCost, op.name());

    executor.memoryExpand(outDataOffs, outDataSize);
    DataWord getEnergyLimitLeft = executor.getContractContext().getEnergyLimitLeft().clone();
    getEnergyLimitLeft.sub(new DataWord(energyCost));

    DataWord adjustedCallEnergy = executor.getCallEnergy(callEnergyWord, getEnergyLimitLeft);

    if (executor.getContractContext().isStatic()
        && (op == Op.CALL || op == Op.CALLTOKEN) && !value.isZero()) {
      throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
    }

    if (!value.isZero()) {
      adjustedCallEnergy.add(new DataWord(Costs.STIPEND_CALL));
    }

    MessageCall msg = new MessageCall(
        op, adjustedCallEnergy, codeAddress, value, inDataOffs, inDataSize,
        outDataOffs, outDataSize, tokenId, isTokenTransferMsg);

    PrecompiledContracts.PrecompiledContract contract =
        PrecompiledContracts.getContractForAddress(codeAddress);

    if (op != Op.CALLCODE && op != Op.DELEGATECALL) {
      executor.getContractContext().getProgramResult()
          .addTouchAccount(codeAddress.getLast20Bytes());
    }

    if (contract != null) {
      executor.callToPrecompiledAddress(msg, contract);
    } else {
      executor.callToAddress(msg);
    }

    executor.step();


  }
}
