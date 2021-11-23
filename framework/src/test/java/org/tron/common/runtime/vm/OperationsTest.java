package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.Op;
import org.tron.core.vm.Operation;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvokeMockImpl;
import org.tron.protos.Protocol;

@Slf4j
public class OperationsTest {

  private ProgramInvokeMockImpl invoke;
  private Program program;

  @BeforeClass
  public static void init() {
    CommonParameter.getInstance().setDebug(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmLondon(1);
    VMConfig.initAllowTvmCompatibleEvm(1);
    OperationRegistry.newBaseOperation();
    OperationRegistry.newAllowTvmTransferTrc10Operation();
    OperationRegistry.newAllowTvmConstantinopleOperation();
    OperationRegistry.newAllowTvmSolidity059Operation();
    OperationRegistry.newAllowTvmIstanbulOperation();
    OperationRegistry.newAllowTvmFreezeOperation();
    OperationRegistry.newAllowTvmVoteOperation();
    OperationRegistry.newAllowTvmLondonOperation();
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    VMConfig.initAllowTvmTransferTrc10(0);
    VMConfig.initAllowTvmConstantinople(0);
    VMConfig.initAllowTvmSolidity059(0);
    VMConfig.initAllowTvmIstanbul(0);
    VMConfig.initAllowTvmLondon(0);
    VMConfig.initAllowTvmCompatibleEvm(0);
  }

  // test Arithmetic Operations
  @Test
  public void testArithmeticOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // test ADD
    byte[] op = {0x60, 0x01, 0x60, 0x01, 0x01};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x02));

    // test MUL
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x02};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 39);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test SUB
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x03};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test DIV
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x04};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 39);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x02));

    // test SDIV
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x05};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 39);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test MOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x06};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 39);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test SMOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x07};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 39);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test ADDMOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x60, 0x01, 0x08};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 33);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test MULMOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x60, 0x01, 0x09};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 33);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test EXP
    op = new byte[]{0x60, 0x02, 0x60, 0x02, 0x0a};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 24);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x04));

    // test SIGNEXTEND
    op = new byte[]{0x60, 0x02, 0x60, 0x02, 0x0b};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 39);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x02));
  }

  // test Bitwise Logic & Comparison Operations
  @Test
  public void testLogicAndComparisonOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // test LT = 0x10
    byte[] op = {0x60, 0x01, 0x60, 0x02, 0x10};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test GT = 0X11
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X11};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test SLT = 0X12
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X12};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test SGT = 0X13
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X13};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // test EQ = 0X14
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X14};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test ISZERO = 0x15
    op = new byte[]{0x60, 0x01, 0x15};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 44);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test AND = 0x16
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x16};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test OR = 0x17
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x17};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x03));

    // test XOR = 0x18
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x18};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x03));

    // test NOT = 0x19
    op = new byte[]{0x60, 0x00, 0x19};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 44);
    Assert.assertEquals((program.getStack().pop().getData())[31], (byte)(-0x01));

    // test BYTE = 0x1a
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x1a};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test SHL = 0x1b
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x1b};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x02));

    // test SHR = 0x1c
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x1c};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // test SAR = 0x1d
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x1d};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

  }

  // test Cryptographic Operations and Environmental Information
  @Test
  public void testCryptographicAndEnvironmentalOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // test SHA3 = 0x20

    // test ADDRESS = 0x30
    byte[] op = {0x30};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), invoke.getContractAddress());

    // test BALANCE = 0x31

    // test ORIGIN = 0x32
    op = new byte[]{0x32};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), invoke.getOriginAddress());

    // test CALLER = 0x33
    op = new byte[]{0x33};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(),
        new DataWord(invoke.getCallerAddress().getLast20Bytes()));

    // CALLVALUE = 0x34
    op = new byte[]{0x34};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), invoke.getCallValue());

    // CALLDATALOAD = 0x35
    op = new byte[]{0x60, 0x01, 0x35};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 44);
    Assert.assertEquals(program.getStack().pop(), invoke.getDataValue(new DataWord(0x01)));

    // CALLDATASIZE = 0x36
    op = new byte[]{0x60, 0x01, 0x36};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 45);
    Assert.assertEquals(program.getStack().pop(), invoke.getDataSize());

    // CALLDATACOPY = 0x37
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x60, 0x01, 0x37};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 35);
    Assert.assertEquals(new DataWord(
        program.getDataCopy(new DataWord(0x01), new DataWord(0x01))),
        new DataWord(invoke.getDataCopy(new DataWord(0x01), new DataWord(0x01))));

    // CODESIZE = 0x38
    op = new byte[]{0x38};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    // CODECOPY = 0x39
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x60, 0x01, 0x39};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 35);
    Assert.assertEquals(new DataWord(
        program.getDataCopy(new DataWord(0x01), new DataWord(0x01))),
        new DataWord(0x00));

    // RETURNDATASIZE = 0x3d
    op = new byte[]{0x3d};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // RETURNDATACOPY = 0x3e
    // op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x60, 0x01, 0x3e};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 35);
    // Assert.assertNull(
    //     program.getReturnDataBufferData(new DataWord(0x01), new DataWord(0x01)));

    // GASPRICE = 0x3a
    op = new byte[]{0x3a};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0));
  }

  // test Block Information
  @Test
  public void testBlockInformationOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // BLOCKHASH = 0x40

    // COINBASE = 0x41
    byte[] op = {0x41};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(),
        new DataWord(invoke.getCoinbase().getLast20Bytes()));

    // TIMESTAMP = 0x42
    op = new byte[]{0x42};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), invoke.getTimestamp());

    // NUMBER = 0x43
    op = new byte[]{0x43};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), invoke.getNumber());

    // DIFFICULTY = 0x44
    op = new byte[]{0x44};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0));

    // GASLIMIT = 0x45
    op = new byte[]{0x45};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 48);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0));

    // CHAINID = 0x46

    // SELFBALANCE = 0x47

    // BASEFEE = 0x48

  }

  // test Memory, Storage and Flow Operations
  @Test
  public void testMemoryStorageAndFlowOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // POP = 0x50
    byte[] op = {0x60, 0x01, 0x50};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 45);
    Assert.assertEquals(program.getStack().size(), 0);

    // MLOAD = 0x51
    op = new byte[]{0x60, 0x01, 0x51};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getStack().pop(), new DataWord(0x00));

    // MSTORE = 0x52
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x52};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 38);
    Assert.assertEquals(program.getMemSize(), 64);

    // MSTORE8 = 0x53
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x53};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    Assert.assertEquals(program.getMemSize(), 32);

    // JUMP = 0x56
    // op = new byte[]{0x5b, 0x60, 0x00, 0x56};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 36);
    // Assert.assertEquals(program.getPC(), 4);

    // JUMPI = 0x57
    // op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x57};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 34);
    // Assert.assertEquals(program.getPC(), 4);

    // PC = 0x58
    op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x58};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 42);
    Assert.assertEquals(program.getPC(), 5);

    // MSIZE = 0x59
    op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x59};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 42);
    Assert.assertEquals(program.getMemSize(), 0);

    // GAS = 0x5a
    op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x5a};
    program = new Program(op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(program.getEnergylimitLeftLong(), 42);
    Assert.assertEquals(program.getStack().pop(), new DataWord(42));

    // JUMPDEST = 0x5b

  }

  // test push, dup, swap, log
  @Test
  public void testPushDupSwapAndLogOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // test push(1-32)
    // byte[] op = {0x60, 0x01};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 47);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));
    //
    // op = new byte[]{0x61, 0x01, 0x02};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 47);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(new byte[]{0x01, 0x02}));
    for (int i = 0; i <= 31; i++) {
      byte[] op = new byte[i + 2];
      op[0] = (byte) (0x60 + i);
      for (int j = 0; j <= i; j++) {
        op[j + 1] = 0x01;
      }
      program = new Program(op, invoke, interTrx);
      testOperations(program);
      Assert.assertEquals(program.getEnergylimitLeftLong(), 47);
      byte[] result = new byte[i + 1];
      for (int k = 0; k <= i; k++) {
        result[k] = 0x01;
      }
      Assert.assertEquals(program.getStack().pop(), new DataWord(result));
    }

    // test dup(1-16)
    // byte[] op = {0x60, 0x01, (byte) 0x80};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 44);
    // Assert.assertEquals(program.getStack().size(), 2);
    //
    // op = new byte[]{0x60, 0x01, 0x60, 0x02, (byte) 0x80};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 41);
    // Assert.assertEquals(program.getStack().size(), 3);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(0x02));

    invoke.setEnergyLimit(100);
    for (int i = 0; i <= 15; i++) {
      byte[] op = new byte[i * 2 + 3];
      op[op.length - 1] = (byte) (0x80 + i);
      for (int j = 0; j <= i; j++) {
        op[2 * j] = 0x60;
        op[2 * j + 1] = (byte) i;
      }
      program = new Program(op, invoke, interTrx);
      testOperations(program);
      Assert.assertEquals(program.getEnergylimitLeftLong(), 100 - 3L * i - 6);
      Assert.assertEquals(program.getStack().pop(), new DataWord(i));
    }

    // test swap(1-16)
    // byte[] op = {0x60, 0x01, 0x60, 0x02, (byte) 0x90};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 91);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));
    //
    // op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x60, 0x03, (byte) 0x91};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getEnergylimitLeftLong(), 88);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));

    for (int i = 0; i <= 15; i++) {
      byte[] op = new byte[i * 2 + 5];
      op[op.length - 1] = (byte) (0x90 + i);
      op[0] = 0x60;
      op[1] = 0x01;
      for (int j = 0; j <= i; j++) {
        op[2 * (j + 1)] = 0x60;
        op[2 * (j + 1) + 1] = (byte) i;
      }
      program = new Program(op, invoke, interTrx);
      testOperations(program);
      Assert.assertEquals(program.getEnergylimitLeftLong(), 100 - 3L * i - 9);
      Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));
    }

    // test log(0-4)
  }

  public void testOperations(Program program) {
    try {
      while (!program.isStopped()) {
        if (VMConfig.vmTrace()) {
          program.saveOpTrace();
        }
        try {
          Operation op = OperationRegistry.get(program.getCurrentOpIntValue());
          if (op == null) {
            throw Program.Exception.invalidOpCode(program.getCurrentOp());
          }
          program.setLastOp((byte) op.getOpcode());
          program.verifyStackSize(op.getRequire());
          //Check not exceeding stack limits
          program.verifyStackOverflow(op.getRequire(), op.getRet());

          program.spendEnergy(op.getEnergyCost(program), Op.getNameOf(op.getOpcode()));
          program.checkCPUTimeLimit(Op.getNameOf(op.getOpcode()));
          op.execute(program);
          program.setPreviouslyExecutedOp((byte) op.getOpcode());
        } catch (RuntimeException e) {
          logger.info("VM halted: [{}]", e.getMessage());
          if (!(e instanceof Program.TransferException)) {
            program.spendAllEnergy();
          }
          program.resetFutureRefund();
          program.stop();
          throw e;
        } finally {
          program.fullTrace();
        }
      }

    } catch (Program.JVMStackOverFlowException | Program.OutOfTimeException e) {
      throw e;
    } catch (RuntimeException e) {
      if (StringUtils.isEmpty(e.getMessage())) {
        logger.warn("Unknown Exception occurred, tx id: {}",
            Hex.toHexString(program.getRootTransactionId()), e);
        program.setRuntimeFailure(new RuntimeException("Unknown Exception"));
      } else {
        program.setRuntimeFailure(e);
      }
    } catch (StackOverflowError soe) {
      logger.info("\n !!! StackOverflowError: update your java run command with -Xss !!!\n", soe);
      throw new Program.JVMStackOverFlowException();
    }
  }

}
