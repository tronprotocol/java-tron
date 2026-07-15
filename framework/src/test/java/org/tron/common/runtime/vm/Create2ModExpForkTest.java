package org.tron.common.runtime.vm;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.utils.ForkController;
import org.tron.core.Constant;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.PrecompiledContracts.PrecompiledContract;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.program.invoke.ProgramInvokeMockImpl;
import org.tron.core.vm.utils.MUtil;
import org.tron.protos.Protocol;


@Slf4j
public class Create2ModExpForkTest extends BaseTest {

  // mirrors the private Program.MAX_DEPTH
  private static final int MAX_CALL_DEPTH = 64;

  // mirrors PrecompiledContracts.ModExp.UPPER_BOUND
  private static final int MOD_EXP_UPPER_BOUND = 1024;

  // ModExp precompile address (0x05)
  private static final DataWord MOD_EXP_ADDR = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000005");

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath(), "--debug"}, TestConstants.TEST_CONF);
    CommonParameter.getInstance().setDebug(true);
  }

  @AfterClass
  public static void destroy() {
    ConfigLoader.disable = false;
    VMConfig.initVmHardFork(false);
    VMConfig.initAllowTvmCompatibleEvm(0);
    Args.clearParam();
  }

  @Before
  public void setUp() {
    ForkController.instance().init(chainBaseManager);
    deactivateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);
  }

  @Test
  public void checkCPUTimeForCreate2_isGatedByFork() {
    MUtil.checkCPUTimeForCreate2();

    activateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);

    OutOfTimeException ex =
        Assert.assertThrows(OutOfTimeException.class, MUtil::checkCPUTimeForCreate2);
    Assert.assertEquals("CPU timeout for create2 executing", ex.getMessage());
  }

  @Test
  public void checkCPUTimeForModExp_isGatedByFork() {
    MUtil.checkCPUTimeForModExp();

    activateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);

    OutOfTimeException ex =
        Assert.assertThrows(OutOfTimeException.class, MUtil::checkCPUTimeForModExp);
    Assert.assertEquals("CPU timeout for modExp executing", ex.getMessage());
  }

  @Test
  public void modExp_degenerateInput_throwsOnlyAfterFork() {
    PrecompiledContract modExp = PrecompiledContracts.getContractForAddress(MOD_EXP_ADDR);
    byte[] data = buildModExpInput(MOD_EXP_UPPER_BOUND + 1);

    Pair<Boolean, byte[]> out = modExp.execute(data);
    Assert.assertTrue(out.getLeft());

    activateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);

    OutOfTimeException ex =
        Assert.assertThrows(OutOfTimeException.class, () -> modExp.execute(data));
    Assert.assertEquals("CPU timeout for modExp executing", ex.getMessage());
  }

  @Test
  public void modExp_atUpperBound_doesNotThrowAfterFork() {
    activateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);

    PrecompiledContract modExp = PrecompiledContracts.getContractForAddress(MOD_EXP_ADDR);
    Pair<Boolean, byte[]> out = modExp.execute(buildModExpInput(MOD_EXP_UPPER_BOUND));
    Assert.assertTrue(out.getLeft());
  }

  @Test
  public void createContract2_atMaxDepth_legacyPath_throwsAfterFork()
      throws ContractValidateException {
    VMConfig.initAllowTvmCompatibleEvm(0);
    activateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);

    Program program = buildProgramAtMaxDepth();
    OutOfTimeException ex = Assert.assertThrows(OutOfTimeException.class,
        () -> program.createContract2(
            DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO()));
    Assert.assertEquals("CPU timeout for create2 executing", ex.getMessage());
  }

  @Test
  public void createContract2_atMaxDepth_compatibleEvmOn_doesNotThrow()
      throws ContractValidateException {
    VMConfig.initAllowTvmCompatibleEvm(1);
    activateFork(ForkBlockVersionEnum.VERSION_4_8_1_1);

    Program program = buildProgramAtMaxDepth();
    program.createContract2(DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO());
    Assert.assertEquals(DataWord.ZERO(), program.getStack().pop());
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private Program buildProgramAtMaxDepth() throws ContractValidateException {
    StoreFactory.init();
    StoreFactory storeFactory = StoreFactory.getInstance();
    storeFactory.setChainBaseManager(chainBaseManager);
    byte[] ops = new byte[] {0};
    ProgramInvokeMockImpl invoke = new ProgramInvokeMockImpl(storeFactory, ops, ops) {
      @Override
      public int getCallDeep() {
        return MAX_CALL_DEPTH;
      }
    };
    Program program = new Program(ops, ops, invoke,
        new InternalTransaction(Protocol.Transaction.getDefaultInstance(),
            InternalTransaction.TrxType.TRX_UNKNOWN_TYPE));
    program.setRootTransactionId(new byte[32]);
    return program;
  }

  private byte[] buildModExpInput(int expLen) {
    byte[] data = new byte[96];
    byte[] expLenWord = new DataWord(expLen).getData();
    System.arraycopy(expLenWord, 0, data, 32, 32);
    return data;
  }

  private void activateFork(ForkBlockVersionEnum forkVersion) {
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    chainBaseManager.getDynamicPropertiesStore().statsByVersion(forkVersion.getValue(), stats);
    long maintenanceTimeInterval =
        chainBaseManager.getDynamicPropertiesStore().getMaintenanceTimeInterval();
    long hardForkTime = ((forkVersion.getHardForkTime() - 1) / maintenanceTimeInterval + 1)
        * maintenanceTimeInterval;
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(hardForkTime + 1);
  }

  private void deactivateFork(ForkBlockVersionEnum forkVersion) {
    chainBaseManager.getDynamicPropertiesStore()
        .statsByVersion(forkVersion.getValue(), new byte[27]);
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0L);
  }
}
