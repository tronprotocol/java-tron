package org.tron.common.runtime.vm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.StringUtils;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.JumpTable;
import org.tron.core.vm.Op;
import org.tron.core.vm.Operation;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.VM;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvokeMockImpl;
import org.tron.protos.Protocol;

@Slf4j
public class OperationsTest {

  private ProgramInvokeMockImpl invoke;
  private Program program;
  private final JumpTable jumpTable = OperationRegistry.newTronV10OperationSet();
  private static ChainBaseManager chainBaseManager;
  private static String dbPath;
  private static TronApplicationContext context;

  @BeforeClass
  public static void init() {
    dbPath = "output_" + OperationsTest.class.getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    Manager manager = context.getBean(Manager.class);
    chainBaseManager = manager.getChainBaseManager();
    CommonParameter.getInstance().setDebug(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmLondon(1);
    VMConfig.initAllowTvmCompatibleEvm(1);
  }

  @AfterClass
  public static void destroy() {
    ConfigLoader.disable = false;
    VMConfig.initVmHardFork(false);
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
    VMConfig.initAllowTvmTransferTrc10(0);
    VMConfig.initAllowTvmConstantinople(0);
    VMConfig.initAllowTvmSolidity059(0);
    VMConfig.initAllowTvmIstanbul(0);
    VMConfig.initAllowTvmLondon(0);
    VMConfig.initAllowTvmCompatibleEvm(0);
  }

  @Test
  public void testStackUnderFlow() {
    for (int i = 0; i < 256; i++) {
      Operation op = jumpTable.get(i);
      if (op.isEnabled()) {
        Program context = buildEmptyContext(new byte[]{(byte) op.getOpcode()});
        VM.play(context, jumpTable);

        if (op.getRequire() != 0) {
          Assert.assertTrue(context.getResult().getException()
              instanceof Program.StackTooSmallException);
        }
      }
    }
  }

  @Test
  public void testStackOverFlow() {
    for (int i = 0; i < 256; i++) {
      Operation op = jumpTable.get(i);
      if (op.isEnabled()) {
        Program context = buildEmptyContext(new byte[]{(byte) op.getOpcode()});
        for (int j = 0; j < 1024; j++) {
          context.stackPushZero();
        }
        VM.play(context, jumpTable);

        if (op.getRet() - op.getRequire() > 0) {
          Assert.assertTrue(context.getResult().getException()
              instanceof Program.StackTooLargeException);
        }
      }
    }
  }

  @SneakyThrows
  private Program buildEmptyContext(byte[] ops) {
    StoreFactory.init();
    StoreFactory storeFactory = StoreFactory.getInstance();
    storeFactory.setChainBaseManager(chainBaseManager);
    Program context = new Program(
        ops, ops,
        new ProgramInvokeMockImpl(storeFactory, ops, ops),
        new InternalTransaction(
            Protocol.Transaction.getDefaultInstance(),
            InternalTransaction.TrxType.TRX_UNKNOWN_TYPE));
    context.setRootTransactionId(new byte[32]);
    return context;
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
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x02), program.getStack().pop());

    // test MUL
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x02};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(11, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // test SUB
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x03};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test DIV
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x04};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(11, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x02), program.getStack().pop());

    // test SDIV
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x05};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(11, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01),program.getStack().pop());

    // test MOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x06};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(11, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // test SMOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x07};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(11, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // test ADDMOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x60, 0x01, 0x08};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(17, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test MULMOD
    op = new byte[]{0x60, 0x02, 0x60, 0x01, 0x60, 0x01, 0x09};
    program = new Program(op, op, invoke, interTrx);;
    testOperations(program);
    Assert.assertEquals(17, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // test EXP
    op = new byte[]{0x60, 0x02, 0x60, 0x02, 0x0a};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(26, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x04), program.getStack().pop());

    // test SIGNEXTEND
    op = new byte[]{0x60, 0x02, 0x60, 0x02, 0x0b};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(11, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x02), program.getStack().pop());
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
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test GT = 0X11
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X11};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // test SLT = 0X12
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X12};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test SGT = 0X13
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X13};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // test EQ = 0X14
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0X14};
    program = new Program(op, op, invoke, interTrx);;
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test ISZERO = 0x15
    op = new byte[]{0x60, 0x01, 0x15};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(6, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test AND = 0x16
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x16};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test OR = 0x17
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x17};
    program = new Program(op, op, invoke, interTrx);;
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x03), program.getStack().pop());

    // test XOR = 0x18
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x18};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x03), program.getStack().pop());

    // test NOT = 0x19
    op = new byte[]{0x60, 0x00, 0x19};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(6, program.getResult().getEnergyUsed());
    Assert.assertEquals((byte) (-0x01), program.getStack().pop().getData()[31]);

    // test BYTE = 0x1a
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x1a};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test SHL = 0x1b
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x1b};
    program = new Program(op, op, invoke, interTrx);;
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x02), program.getStack().pop());

    // test SHR = 0x1c
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x1c};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // test SAR = 0x1d
    op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x1d};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

  }

  // test Cryptographic Operations and Environmental Information
  @Test
  public void testCryptographicAndEnvironmentalOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // test SHA3 = 0x20
    byte[] op = {0x60, 0x01, 0x60, 0x01, 0x20};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(45, program.getResult().getEnergyUsed());
    Assert.assertEquals("bc36789e7a1e281436464229828f817d6612f7b477d66591ff96a9e064bcc98a",
        program.getStack().pop().toHexString());

    // test ADDRESS = 0x30
    op = new byte[]{0x30};
    program = new Program(op, op, invoke, interTrx);;
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertArrayEquals(invoke.getContractAddress().getLast20Bytes(),
        program.getStack().pop().getLast20Bytes());

    // test ORIGIN = 0x32
    op = new byte[]{0x32};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(invoke.getOriginAddress(), program.getStack().pop());

    // test CALLER = 0x33
    op = new byte[]{0x33};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(invoke.getCallerAddress().getLast20Bytes()),
        program.getStack().pop());

    // CALLVALUE = 0x34
    op = new byte[]{0x34};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(invoke.getCallValue(), program.getStack().pop());

    // CALLDATALOAD = 0x35
    op = new byte[]{0x60, 0x01, 0x35};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(6, program.getResult().getEnergyUsed());
    Assert.assertEquals(invoke.getDataValue(new DataWord(0x01)), program.getStack().pop());

    // CALLDATASIZE = 0x36
    op = new byte[]{0x60, 0x01, 0x36};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(5, program.getResult().getEnergyUsed());
    Assert.assertEquals(invoke.getDataSize(), program.getStack().pop());

    // CALLDATACOPY = 0x37
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x60, 0x01, 0x37};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(15, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(invoke.getDataCopy(new DataWord(0x01),
            new DataWord(0x01))),
        new DataWord(program.getDataCopy(new DataWord(0x01), new DataWord(0x01))));

    // CODESIZE = 0x38
    op = new byte[]{0x38};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x01), program.getStack().pop());

    // CODECOPY = 0x39
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x60, 0x01, 0x39};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(15, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), new DataWord(
        program.getDataCopy(new DataWord(0x01), new DataWord(0x01))));

    // RETURNDATASIZE = 0x3d
    op = new byte[]{0x3d};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // RETURNDATACOPY = 0x3e
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x60, 0x01, 0x3e};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(50, program.getResult().getEnergyUsed());
    Assert.assertNull(
        program.getReturnDataBufferData(new DataWord(0x01), new DataWord(0x01)));

    // GASPRICE = 0x3a
    op = new byte[]{0x3a};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0), program.getStack().pop());
  }

  // test Block Information
  @Test
  public void testBlockInformationOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // BLOCKHASH = 0x40
    byte[] op = {0x40};
    program = new Program(op, op, invoke, interTrx);
    program.stackPush(new DataWord(33));
    testOperations(program);
    Assert.assertEquals(20, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0), program.getStack().pop());

    // COINBASE = 0x41
    op = new byte[]{0x41};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(invoke.getCoinbase().getLast20Bytes()),
        program.getStack().pop());

    // TIMESTAMP = 0x42
    op = new byte[]{0x42};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(invoke.getTimestamp(), program.getStack().pop());

    // NUMBER = 0x43
    op = new byte[]{0x43};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(invoke.getNumber(), program.getStack().pop());

    // DIFFICULTY = 0x44
    op = new byte[]{0x44};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0), program.getStack().pop());

    // GASLIMIT = 0x45
    op = new byte[]{0x45};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(2, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0), program.getStack().pop());

    // CHAINID = 0x46

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
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(5, program.getResult().getEnergyUsed());
    Assert.assertEquals(0, program.getStack().size());

    // MLOAD = 0x51
    op = new byte[]{0x60, 0x01, 0x51};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x00), program.getStack().pop());

    // MSTORE = 0x52
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x52};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(12, program.getResult().getEnergyUsed());
    Assert.assertEquals(64, program.getMemSize());

    // MSTORE8 = 0x53
    op = new byte[]{0x60, 0x01, 0x60, 0x01, 0x53};
    program = new Program(op, op, invoke, interTrx);;
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed(), 41);
    Assert.assertEquals(32, program.getMemSize());

    // JUMP = 0x56
    // JUMPI = 0x57
    // JUMPDEST = 0x5b
    op = compile(
        "PUSH1 0x01 PUSH1 0x05 JUMPI JUMPDEST PUSH1 0xCC");
    invoke = new ProgramInvokeMockImpl(op, op);
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    Assert.assertEquals(20, program.getResult().getEnergyUsed());
    Assert.assertEquals("00000000000000000000000000000000000000000000000000000000000000CC",
        Hex.toHexString(program.getStack().peek().getData()).toUpperCase());

    // PC = 0x58
    op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x58};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(8, program.getResult().getEnergyUsed());
    Assert.assertEquals(5, program.getPC());

    // MSIZE = 0x59
    op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x59};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(8, program.getResult().getEnergyUsed());
    Assert.assertEquals(0, program.getMemSize());

    // GAS = 0x5a
    op = new byte[]{0x60, 0x01, 0x60, 0x00, 0x5a};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(8, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x2a), program.getStack().pop());

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
    // Assert.assertEquals(program.getResult().getEnergyUsed(), 47);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));
    //
    // op = new byte[]{0x61, 0x01, 0x02};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getResult().getEnergyUsed(), 47);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(new byte[]{0x01, 0x02}));
    for (int i = 0; i <= 31; i++) {
      byte[] op = new byte[i + 2];
      op[0] = (byte) (0x60 + i);
      for (int j = 0; j <= i; j++) {
        op[j + 1] = 0x01;
      }
      program = new Program(op, op, invoke, interTrx);
      testOperations(program);
      Assert.assertEquals(3, program.getResult().getEnergyUsed());
      byte[] result = new byte[i + 1];
      for (int k = 0; k <= i; k++) {
        result[k] = 0x01;
      }
      Assert.assertEquals(new DataWord(result), program.getStack().pop());
    }

    // test dup(1-16)
    // byte[] op = {0x60, 0x01, (byte) 0x80};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getResult().getEnergyUsed(), 44);
    // Assert.assertEquals(program.getStack().size(), 2);
    //
    // op = new byte[]{0x60, 0x01, 0x60, 0x02, (byte) 0x80};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getResult().getEnergyUsed(), 41);
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
      program = new Program(op, op, invoke, interTrx);
      testOperations(program);
      Assert.assertEquals(3L * (i + 2), program.getResult().getEnergyUsed());
      Assert.assertEquals(new DataWord(i), program.getStack().pop());
    }

    // test swap(1-16)
    // byte[] op = {0x60, 0x01, 0x60, 0x02, (byte) 0x90};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getResult().getEnergyUsed(), 91);
    // Assert.assertEquals(program.getStack().pop(), new DataWord(0x01));
    //
    // op = new byte[]{0x60, 0x01, 0x60, 0x02, 0x60, 0x03, (byte) 0x91};
    // program = new Program(op, invoke, interTrx);
    // testOperations(program);
    // Assert.assertEquals(program.getResult().getEnergyUsed(), 88);
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
      program = new Program(op, op, invoke, interTrx);
      testOperations(program);
      Assert.assertEquals((3L * (i + 3)), program.getResult().getEnergyUsed());
      Assert.assertEquals(new DataWord(0x01), program.getStack().pop());
    }

    // test log(0-4)
    invoke.setEnergyLimit(5000);
    byte[] op = compile(
        "PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x00 LOG0");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    List<LogInfo> logInfoList = program.getResult().getLogInfoList();
    LogInfo logInfo = logInfoList.get(0);
    assertEquals("471fd3ad3e9eeadeec4608b92d16ce6b500704cc",
        Hex.toHexString(logInfo.getAddress()));
    assertEquals(0, logInfo.getTopics().size());
    assertEquals("0000000000000000000000000000000000000000000000000000000000001234",
        Hex.toHexString(logInfo
        .getData()));
    Assert.assertEquals(646, program.getResult().getEnergyUsed());

    op = compile(
        "PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH1 0x20 PUSH1 0x00 LOG1");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    logInfoList = program.getResult().getLogInfoList();
    logInfo = logInfoList.get(0);
    assertEquals("471fd3ad3e9eeadeec4608b92d16ce6b500704cc",
        Hex.toHexString(logInfo.getAddress()));
    assertEquals(1, logInfo.getTopics().size());
    assertEquals("0000000000000000000000000000000000000000000000000000000000001234",
        Hex.toHexString(logInfo
        .getData()));
    Assert.assertEquals(1024, program.getResult().getEnergyUsed());

    op = compile(
        "PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH2 0x6666 PUSH1 0x20 PUSH1 0x00 LOG2");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    logInfoList = program.getResult().getLogInfoList();
    logInfo = logInfoList.get(0);
    assertEquals("471fd3ad3e9eeadeec4608b92d16ce6b500704cc",
        Hex.toHexString(logInfo.getAddress()));
    assertEquals(2, logInfo.getTopics().size());
    assertEquals("0000000000000000000000000000000000000000000000000000000000001234",
        Hex.toHexString(logInfo
        .getData()));
    Assert.assertEquals(1402, program.getResult().getEnergyUsed());

    op = compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999"
        + " PUSH2 0x6666 PUSH2 0x3333 PUSH1 0x20 PUSH1 0x00 LOG3");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    logInfoList = program.getResult().getLogInfoList();
    logInfo = logInfoList.get(0);
    assertEquals("471fd3ad3e9eeadeec4608b92d16ce6b500704cc",
        Hex.toHexString(logInfo.getAddress()));
    assertEquals(3, logInfo.getTopics().size());
    assertEquals("0000000000000000000000000000000000000000000000000000000000001234",
        Hex.toHexString(logInfo
        .getData()));
    Assert.assertEquals(1780, program.getResult().getEnergyUsed());

    op = compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH2 0x9999 PUSH2"
        + " 0x6666 PUSH2 0x3333 PUSH2 0x5555 PUSH1 0x20 PUSH1 0x00 LOG4");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    logInfoList = program.getResult().getLogInfoList();
    logInfo = logInfoList.get(0);
    assertEquals("471fd3ad3e9eeadeec4608b92d16ce6b500704cc",
        Hex.toHexString(logInfo.getAddress()));
    assertEquals(4, logInfo.getTopics().size());
    assertEquals("0000000000000000000000000000000000000000000000000000000000001234",
        Hex.toHexString(logInfo.getData()));
    Assert.assertEquals(2158, program.getResult().getEnergyUsed());
  }

  @Test
  public void testOtherOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // STOP = 0x00
    byte[] op = {0x00};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(0, program.getResult().getEnergyUsed());
    Assert.assertTrue(program.isStopped());

    // return = 0xf3
    op = new byte[]{0x60, 0x01, 0x60, 0x01, (byte) 0xf3};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(1, program.getResult().getHReturn().length);
    Assert.assertTrue(program.isStopped());

    // revert = 0xfd
    op = new byte[]{0x60, 0x01, 0x60, 0x01, (byte) 0xfd};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(9, program.getResult().getEnergyUsed());
    Assert.assertEquals(1, program.getResult().getHReturn().length);
    Assert.assertTrue(program.isStopped());
    Assert.assertTrue(program.getResult().isRevert());
  }

  @Ignore
  @Test
  public void testComplexOperations() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    Protocol.Transaction trx = Protocol.Transaction.getDefaultInstance();
    InternalTransaction interTrx =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE);

    // test BALANCE = 0x31
    byte[] op = new byte[]{0x31};
    program = new Program(op, op, invoke, interTrx);
    program.stackPush(new DataWord("41471fd3ad3e9eeadeec4608b92d16ce6b500704cc"));
    testOperations(program);
    Assert.assertEquals(20, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0), program.getStack().pop());

    // SELFBALANCE = 0x47
    op = new byte[]{0x47};
    program = new Program(op, op, invoke, interTrx);
    testOperations(program);
    Assert.assertEquals(5, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0),program.getStack().pop());

    // SLOAD = 0x54, SSTORE = 0x55
    invoke.setEnergyLimit(20000);
    invoke.getDeposit().putStorageValue(Hex.decode(
            "41471fd3ad3e9eeadeec4608b92d16ce6b500704cc"), new DataWord(0xAA),
        new DataWord(0x01));
    invoke.getDeposit().putStorageValue(Hex.decode(
            "41471fd3ad3e9eeadeec4608b92d16ce6b500704cc"), new DataWord(0xCC),
        new DataWord(0x01));
    op = compile("PUSH1 0x22 PUSH1 0xAA SSTORE PUSH1 0x33 PUSH1 0xCC SSTORE PUSH1 0xCC SLOAD");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    Assert.assertEquals(10065, program.getResult().getEnergyUsed());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000033",
        Hex.toHexString(program.getStack().peek().getData()).toUpperCase());

    // EXTCODESIZE = 0x3b
    op = new byte[]{0x3b};
    program = new Program(op, op, invoke, interTrx);
    program.stackPush(new DataWord("471fd3ad3e9eeadeec4608b92d16ce6b500704cc"));
    testOperations(program);
    Assert.assertEquals(20, program.getResult().getEnergyUsed());
    Assert.assertEquals(new DataWord(0x62), program.getStack().pop());

    // EXTCODECOPY = 0x3c
    op = Hex.decode("60036007600073471FD3AD3E9EEADEEC4608B92D16CE6B500704CC3C123456");
    program = new Program(op, op, invoke, interTrx);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    testSingleOperation(program);
    Assert.assertEquals(38, program.getResult().getEnergyUsed());
    Assert.assertEquals("6000600000000000000000000000000000000000000000000000000000000000",
        Hex.toHexString(program.getMemory()).toUpperCase());

  }

  private void testOperations(Program program) {
    try {
      while (!program.isStopped()) {
        if (VMConfig.vmTrace()) {
          program.saveOpTrace();
        }
        try {
          Operation op = jumpTable.get(program.getCurrentOpIntValue());
          if (!op.isEnabled()) {
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
        program.setRuntimeFailure(new RuntimeException("Unknown Exception"));
      } else {
        program.setRuntimeFailure(e);
      }
    } catch (StackOverflowError soe) {
      logger.info("\n !!! StackOverflowError: update your java run command with -Xss !!!\n", soe);
      throw new Program.JVMStackOverFlowException();
    }
  }

  private void testSingleOperation(Program program) {
    try {
      try {
        Operation op = jumpTable.get(program.getCurrentOpIntValue());
        if (!op.isEnabled()) {
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
        program.stop();
        throw e;
      } finally {
        program.fullTrace();
      }
    } catch (Program.JVMStackOverFlowException | Program.OutOfTimeException e) {
      throw e;
    } catch (RuntimeException e) {
      if (StringUtils.isEmpty(e.getMessage())) {
        program.setRuntimeFailure(new RuntimeException("Unknown Exception"));
      } else {
        program.setRuntimeFailure(e);
      }
    } catch (StackOverflowError soe) {
      logger.info("\n !!! StackOverflowError: update your java run command with -Xss !!!\n", soe);
      throw new Program.JVMStackOverFlowException();
    }
  }

  private byte[] compile(String code) {
    return new BytecodeCompiler().compile(code);
  }

}
