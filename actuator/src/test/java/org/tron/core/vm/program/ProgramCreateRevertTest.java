package org.tron.core.vm.program;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.VM;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;

@RunWith(Parameterized.class)
public class ProgramCreateRevertTest {

  private static final long ENERGY_LIMIT = 2_000_000L;
  private static final long CHILD_ENERGY_LIMIT = 1_968_750L;
  // Four PUSH1 instructions cost 12; legacy MSTORE8 charges only memory expansion, costing 3.
  private static final long INIT_ENERGY = 15L;
  private static final byte[] EMPTY = new byte[0];
  private static final byte[] EF = {(byte) 0xef};
  private static final byte[] EE = {(byte) 0xee};
  private final boolean create2;
  private final boolean london;
  private final boolean enabled;
  private VMConfig.Snapshot snapshot;
  private Repository repository;
  private Repository childRepository;
  private ProgramInvoke invoke;
  private InternalTransaction root;
  private Program parent;

  @Parameterized.Parameters(name = "create2={0}, london={1}, resourceOptimization={2}")
  public static Collection<Object[]> parameters() {
    Collection<Object[]> cases = new ArrayList<>();
    for (boolean create2 : new boolean[]{false, true}) {
      for (boolean london : new boolean[]{false, true}) {
        for (boolean enabled : new boolean[]{false, true}) {
          cases.add(new Object[]{create2, london, enabled});
        }
      }
    }
    return cases;
  }

  public ProgramCreateRevertTest(boolean create2, boolean london, boolean enabled) {
    this.create2 = create2;
    this.london = london;
    this.enabled = enabled;
  }

  @Before
  public void setUp() {
    snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = enabled;
    snapshot.allowTvmLondon = london;
    snapshot.allowTvmCompatibleEvm = true;
    snapshot.allowTvmConstantinople = true;
    snapshot.allowTvmIstanbul = true;
    snapshot.allowTvmSolidity059 = true;
    snapshot.allowTvmTransferTrc10 = true;
    snapshot.allowMultiSign = true;
    snapshot.allowEnergyAdjustment = true;
    VMConfig.setLocalSnapshot(snapshot);
    repository = mock(Repository.class);
    childRepository = mock(Repository.class);
    when(repository.newRepositoryChild()).thenReturn(childRepository);
    invoke = mock(ProgramInvoke.class);
    when(invoke.getDeposit()).thenReturn(repository);
    when(invoke.getContractAddress()).thenReturn(new DataWord(1));
    when(invoke.getCallerAddress()).thenReturn(new DataWord(1));
    when(invoke.getOriginAddress()).thenReturn(new DataWord(1));
    when(invoke.getCallValue()).thenReturn(DataWord.ZERO());
    when(invoke.getPrevHash()).thenReturn(DataWord.ZERO());
    when(invoke.getCoinbase()).thenReturn(DataWord.ZERO());
    when(invoke.getTimestamp()).thenReturn(DataWord.ZERO());
    when(invoke.getNumber()).thenReturn(DataWord.ZERO());
    when(invoke.getDifficulty()).thenReturn(DataWord.ZERO());
    when(invoke.getEnergyLimit()).thenReturn(ENERGY_LIMIT);
    when(invoke.getVmShouldEndInUs()).thenReturn(Long.MAX_VALUE);
    root = mock(InternalTransaction.class);
    when(root.getHash()).thenReturn(new byte[32]);
    parent = newParent(false);
  }

  @After
  public void restoreConfig() {
    VMConfig.clearLocalSnapshot();
    OperationRegistry.prepareAndGetTable(false);
  }

  @Test
  public void efRevertPreservesDataAndEnergyAfterActivation() {
    execute(Hex.decode("60ef60005360016000fd"));
    if (london && !enabled) {
      assertOutcome(CHILD_ENERGY_LIMIT, EMPTY, false);
    } else {
      assertOutcome(INIT_ENERGY, EF, false);
    }
  }

  @Test
  public void nonEfRevertPreservesDataAndEnergyInBothModes() {
    execute(Hex.decode("60ee60005360016000fd"));
    assertOutcome(INIT_ENERGY, EE, false);
  }

  @Test
  public void efReturnStillRejectsDeploymentWithLondon() {
    execute(Hex.decode("60ef60005360016000f3"));
    assertOutcome(london ? CHILD_ENERGY_LIMIT : INIT_ENERGY + 200L, EMPTY, !london);
  }

  @Test
  public void nonEfReturnStillDeploysCode() {
    execute(Hex.decode("60ee60005360016000f3"));
    assertOutcome(INIT_ENERGY + 200L, EMPTY, true);
  }

  @Test
  public void existingChildExceptionStillPreventsRefundAndCommit() {
    RuntimeException exception = new Program.OutOfEnergyException("child energy exhausted");
    ProgramResult[] childResult = new ProgramResult[1];
    executeWithChild(child -> {
      childResult[0] = revertedResult(child);
      childResult[0].setException(exception);
    });
    if (london && !enabled) {
      assertTrue(childResult[0].getException() instanceof Program.InvalidCodeException);
    } else {
      assertSame(exception, childResult[0].getException());
    }
    assertOutcome(CHILD_ENERGY_LIMIT, EMPTY, false);
  }

  @Test
  public void revertedChildEffectsAreRejectedAndNonceIsRetained() {
    InternalTransaction nested = mock(InternalTransaction.class);
    executeWithChild(child -> {
      ProgramResult result = revertedResult(child);
      result.addLogInfo(new LogInfo(new byte[21], Collections.emptyList(), new byte[]{1}));
      result.addDeleteAccount(new DataWord(2));
      result.addInternalTransaction(nested);
      child.setNonce(7);
    });
    assertTrue(parent.getResult().getLogInfoList().isEmpty());
    assertTrue(parent.getResult().getDeleteAccounts().isEmpty());
    assertEquals(2, parent.getResult().getInternalTransactions().size());
    assertTrue(parent.getResult().getInternalTransactions().get(0).isRejected());
    assertSame(nested, parent.getResult().getInternalTransactions().get(1));
    verify(nested).reject();
    assertEquals(7, parent.getNonce());
    assertTrue(parent.getStack().pop().isZero());
    verify(childRepository, never()).commit();
    verify(childRepository, never()).saveCode(any(byte[].class), any(byte[].class));
  }

  @Test
  public void expiredParentDeadlineTakesPriorityOverChildRevert() {
    parent = newParent(true);
    InternalTransaction nested = mock(InternalTransaction.class);
    Runnable execute = () -> executeWithChild(child -> {
      revertedResult(child).addInternalTransaction(nested);
      child.setNonce(7);
    });
    if (enabled) {
      assertThrows(Program.OutOfTimeException.class, execute::run);
      assertTrue(parent.getStack().isEmpty());
      assertEquals(1, parent.getNonce());
      assertEquals(1, parent.getResult().getInternalTransactions().size());
      assertEquals(CHILD_ENERGY_LIMIT, parent.getResult().getEnergyUsed());
      assertEquals(0, parent.getReturnDataBufferSize().intValue());
      verify(childRepository, never()).commit();
    } else {
      execute.run();
      assertEquals(7, parent.getNonce());
      assertEquals(london ? CHILD_ENERGY_LIMIT : INIT_ENERGY,
          parent.getResult().getEnergyUsed());
      assertTrue(parent.getStack().pop().isZero());
    }
  }

  @Test
  public void exceptionalReturnBufferRetainsExistingOsakaBoundary() throws Exception {
    Field returnData = Program.class.getDeclaredField("returnDataBuffer");
    returnData.setAccessible(true);
    for (boolean osaka : new boolean[]{false, true}) {
      snapshot.allowTvmOsaka = osaka;
      parent = newParent(false);
      returnData.set(parent, new byte[]{(byte) 0xaa});
      executeWithChild(this::revertedResult);
      byte[] expected = EF;
      if (london && !enabled) {
        // CREATE2 historically preserves old returndata before Osaka on exceptional failure.
        expected = create2 && !osaka ? new byte[]{(byte) 0xaa} : EMPTY;
      }
      assertArrayEquals(expected, parent.getReturnDataBufferData(DataWord.ZERO(),
          parent.getReturnDataBufferSize()));
      verify(childRepository, never()).commit();
    }
  }

  private Program newParent(boolean expired) {
    // The continuation stores RETURNDATASIZE, making the child result observable in parent state.
    Program program = new Program(Hex.decode("3d60005500"), new byte[21], invoke, root) {
      @Override
      public void checkCPUTimeLimit(String opName) {
        if (expired) {
          throw Program.Exception.notEnoughTime(opName);
        }
        super.checkCPUTimeLimit(opName);
      }
    };
    program.setRootTransactionId(new byte[32]);
    // Contract version 1 keeps 1/64 of the Energy in the parent, even on child failure.
    program.setContractVersion(1);
    return program;
  }

  private void execute(byte[] initCode) {
    parent.memorySave(0, initCode);
    OperationRegistry.prepareAndGetTable(false);
    if (create2) {
      parent.createContract2(DataWord.ZERO(), DataWord.ZERO(), new DataWord(initCode.length),
          DataWord.ONE());
    } else {
      parent.createContract(DataWord.ZERO(), DataWord.ZERO(), new DataWord(initCode.length));
    }
  }

  private void executeWithChild(Consumer<Program> action) {
    try (MockedStatic<VM> vm = mockStatic(VM.class)) {
      vm.when(() -> VM.play(any(Program.class), any())).thenAnswer(invocation -> {
        action.accept(invocation.getArgument(0));
        return null;
      });
      execute(Hex.decode("60ef60005360016000fd"));
      vm.verify(() -> VM.play(any(Program.class), any()));
    }
  }

  private ProgramResult revertedResult(Program child) {
    ProgramResult result = child.getResult();
    result.setRevert();
    result.setHReturn(EF);
    result.spendEnergy(INIT_ENERGY);
    return result;
  }

  private void assertOutcome(long energyUsed, byte[] returnData, boolean deployed) {
    // These costs cover createContract*(), excluding the parent CREATE opcode's upfront cost.
    assertEquals(energyUsed, parent.getResult().getEnergyUsed());
    assertArrayEquals(returnData, parent.getReturnDataBufferData(DataWord.ZERO(),
        parent.getReturnDataBufferSize()));
    assertEquals(!deployed, parent.getStack().pop().isZero());
    assertNull(parent.getResult().getException());
    assertFalse(parent.getResult().isRevert());
    assertEquals(1, parent.getNonce());
    assertEquals(1, parent.getResult().getInternalTransactions().size());
    assertEquals(!deployed, parent.getResult().getInternalTransactions().get(0).isRejected());
    if (deployed) {
      verify(childRepository).commit();
    } else {
      verify(childRepository, never()).commit();
    }

    VM.play(parent, OperationRegistry.prepareAndGetTable(false));
    assertNull(parent.getResult().getException());
    verify(repository).putStorageValue(any(byte[].class), eq(DataWord.ZERO()),
        eq(new DataWord(returnData.length)));
  }
}
