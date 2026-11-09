package org.tron.core.vm.program;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.MessageCall;
import org.tron.core.vm.Op;
import org.tron.core.vm.PrecompiledContracts.PrecompiledContract;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;

@RunWith(Parameterized.class)
public class ProgramPrecompiledContractCleanupTest {

  private static final long CALL_ENERGY = 1000;
  private static final long REQUIRED_ENERGY = 20;
  private static final byte[] INPUT = new DataWord(7).getData();
  private static final byte[] OUTPUT = new DataWord(42).getData();

  private final boolean constantCall;
  private final boolean resourceOptimization;
  private Repository child;
  private Program program;
  private ProgramResult result;

  @Parameters(name = "constantCall={0}, resourceOptimization={1}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][]{
        {false, false}, {false, true}, {true, false}, {true, true}
    });
  }

  public ProgramPrecompiledContractCleanupTest(boolean constantCall, boolean resourceOptimization) {
    this.constantCall = constantCall;
    this.resourceOptimization = resourceOptimization;
  }

  @Before
  public void setUp() {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = resourceOptimization;
    VMConfig.setLocalSnapshot(snapshot);
    newExecutionContext();
  }

  @After
  public void tearDown() {
    VMConfig.clearLocalSnapshot();
  }

  @Test
  public void successfulReturnClearsReferencesAndCommitsChild() {
    PrecompiledContract contract = returning(true);

    program.callToPrecompiledAddress(message(), contract);

    assertReferencesCleared(contract);
    verify(child).commit();
    assertSame(result, program.getResult());
    assertNull(result.getException());
    assertEquals(DataWord.ONE(), program.getStack().pop());
    assertEquals(REQUIRED_ENERGY, result.getEnergyUsed());
    assertArrayEquals(OUTPUT,
        program.getReturnDataBufferData(DataWord.ZERO(), new DataWord(OUTPUT.length)));
    assertArrayEquals(OUTPUT, program.memoryChunk(64, OUTPUT.length));
  }

  @Test
  public void failedReturnClearsReferencesWithoutCommittingChild() {
    PrecompiledContract contract = returning(false);

    program.callToPrecompiledAddress(message(), contract);

    assertReferencesCleared(contract);
    verify(child, never()).commit();
    assertSame(result, program.getResult());
    assertNull(result.getException());
    assertEquals(DataWord.ZERO(), program.getStack().pop());
    assertEquals(CALL_ENERGY, result.getEnergyUsed());
  }

  @Test
  public void runtimeExceptionClearsReferencesAndPropagates() {
    IllegalStateException failure = new IllegalStateException("precompile failed");
    PrecompiledContract contract = new InspectingContract(context -> {
      throw failure;
    });

    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> program.callToPrecompiledAddress(message(), contract)));

    assertReferencesCleared(contract);
    verify(child, never()).commit();
    assertSame(result, program.getResult());
  }

  @Test
  public void timeoutClearsReferencesAndPropagates() {
    OutOfTimeException failure = new OutOfTimeException("precompile timed out");
    PrecompiledContract contract = new InspectingContract(context -> {
      throw failure;
    });

    assertSame(failure, assertThrows(OutOfTimeException.class,
        () -> program.callToPrecompiledAddress(message(), contract)));

    assertReferencesCleared(contract);
    verify(child, never()).commit();
    assertSame(result, program.getResult());
  }

  @Test
  public void failedReturnPreservesExceptionStoredInProgramResult() {
    IllegalStateException failure = new IllegalStateException("recorded precompile failure");
    PrecompiledContract contract = new InspectingContract(context -> {
      context.getResult().setException(failure);
      return Pair.of(false, new byte[0]);
    });

    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> program.callToPrecompiledAddress(message(), contract)));

    assertReferencesCleared(contract);
    verify(child, never()).commit();
    assertSame(result, program.getResult());
    assertSame(failure, result.getException());
    assertEquals(DataWord.ZERO(), program.getStack().pop());
  }

  @Test
  public void reusedPrecompileReceivesFreshExecutionReferences() {
    PrecompiledContract contract = returning(true);
    Repository firstChild = child;
    ProgramResult firstResult = result;
    program.callToPrecompiledAddress(message(), contract);
    assertReferencesCleared(contract);
    verify(firstChild).commit();

    newExecutionContext();
    assertNotSame(firstChild, child);
    assertNotSame(firstResult, result);
    program.callToPrecompiledAddress(message(), contract);

    assertReferencesCleared(contract);
    verify(child).commit();
    assertEquals(DataWord.ONE(), program.getStack().pop());
    assertEquals(REQUIRED_ENERGY, result.getEnergyUsed());
  }

  private void newExecutionContext() {
    Repository repository = mock(Repository.class);
    child = mock(Repository.class);
    when(repository.newRepositoryChild()).thenReturn(child);
    ProgramInvoke invoke = mock(ProgramInvoke.class);
    when(invoke.getDeposit()).thenReturn(repository);
    when(invoke.getContractAddress()).thenReturn(new DataWord(123));
    when(invoke.getEnergyLimit()).thenReturn(10000L);
    when(invoke.isConstantCall()).thenReturn(constantCall);
    program = new Program(new byte[0], new byte[21], invoke, mock(InternalTransaction.class));
    program.memorySave(0, INPUT);
    // Mirror the caller reserving the message's energy before invoking the precompile.
    program.spendEnergy(CALL_ENERGY, "precompile call");
    result = program.getResult();
  }

  private static MessageCall message() {
    return new MessageCall(Op.CALL, new DataWord(CALL_ENERGY), new DataWord(4),
        DataWord.ZERO(), DataWord.ZERO(), new DataWord(INPUT.length),
        new DataWord(64), new DataWord(OUTPUT.length), DataWord.ZERO(), false);
  }

  private PrecompiledContract returning(boolean success) {
    return new InspectingContract(context -> Pair.of(success, OUTPUT));
  }

  private static void assertReferencesCleared(PrecompiledContract contract) {
    assertNull(contract.getDeposit());
    assertNull(contract.getResult());
  }

  private class InspectingContract extends PrecompiledContract {

    private final Function<PrecompiledContract, Pair<Boolean, byte[]>> execution;

    private InspectingContract(Function<PrecompiledContract, Pair<Boolean, byte[]>> execution) {
      this.execution = execution;
    }

    @Override
    public long getEnergyForData(byte[] data) {
      return REQUIRED_ENERGY;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      assertSame(child, getDeposit());
      assertSame(result, getResult());
      assertEquals(constantCall, isConstantCall());
      assertArrayEquals(INPUT, data);
      return execution.apply(this);
    }
  }
}
