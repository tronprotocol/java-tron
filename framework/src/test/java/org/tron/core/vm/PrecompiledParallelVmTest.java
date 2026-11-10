package org.tron.core.vm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.InternalTransaction.TrxType;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.core.actuator.VMActuator;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionContext;
import org.tron.core.vm.PrecompiledContracts.BatchValidateSign;
import org.tron.core.vm.PrecompiledContracts.PrecompiledContract;
import org.tron.core.vm.PrecompiledContracts.VerifyTransferProof;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

/**
 * Execute a terminal CALL through the real VM, actuator and runtime result mapping. Transaction
 * validation/storage and cryptographic workers are isolated; CPU checks and energy accounting are
 * not mocked. With no following opcode, a timeout must be caught by the transaction-end CPU check.
 */
@RunWith(Parameterized.class)
public class PrecompiledParallelVmTest {

  private static final long ENERGY_LIMIT = 1_000_000;
  private static final int CALL_ENERGY = 500_000;
  private static final int OUTPUT_OFFSET = 4096;
  private static final long ON_TIME_BUDGET_US = TimeUnit.SECONDS.toMicros(5);
  private static final long TIMEOUT_BUDGET_US = TimeUnit.MILLISECONDS.toMicros(150);
  private final boolean transferProof;
  private final Completion completion;
  private final NodeMode mode;

  private enum Completion {
    ON_TIME, LATCH_TIMEOUT, PUBLICATION_TIMEOUT
  }

  private enum NodeMode {
    ENERGY_ADJUSTMENT, ENERGY_ADJUSTMENT_OFF, DEBUG, SOLIDITY
  }

  @BeforeClass
  public static void warmUp() {
    OperationRegistry.init();
    // Initialize VerifyProof.UNCOMMITTED with the real Merkle primitive before mocking Sapling.
    PrecompiledContracts.getContractForAddress(DataWord.ZERO());
  }

  @Parameterized.Parameters(name = "{index}: transferProof={0}, {1}, {2}")
  public static Collection<Object[]> cases() {
    List<Object[]> cases = new ArrayList<>();
    for (boolean transferProof : new boolean[]{false, true}) {
      for (Completion completion : Completion.values()) {
        for (NodeMode mode : NodeMode.values()) {
          cases.add(new Object[]{transferProof, completion, mode});
        }
      }
    }
    return cases;
  }

  public PrecompiledParallelVmTest(boolean transferProof, Completion completion, NodeMode mode) {
    this.transferProof = transferProof;
    this.completion = completion;
    this.mode = mode;
  }

  @Test(timeout = 15_000)
  public void terminalCallPreservesTransactionResultAndEnergy() throws Exception {
    CommonParameter parameter = CommonParameter.getInstance();
    boolean previousDebug = parameter.isDebug();
    boolean previousSolidity = parameter.isSolidityNode();
    // A local snapshot isolates this test's flags without changing the HEAD configuration.
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowEnergyAdjustment = mode != NodeMode.ENERGY_ADJUSTMENT_OFF;
    snapshot.allowTvmOsaka = true;
    snapshot.allowTvmSelfdestructRestriction = true;
    snapshot.allowTvmSolidity059 = true;
    snapshot.allowShieldedTRC20Transaction = true;
    VMConfig.setLocalSnapshot(snapshot);
    parameter.setDebug(mode == NodeMode.DEBUG);
    parameter.setSolidityNode(mode == NodeMode.SOLIDITY);
    try (MockedStatic<JLibrustzcash> sapling = mockStatic(JLibrustzcash.class)) {
      sapling.when(() -> JLibrustzcash.librustzcashSaplingCheckSpendNew(any())).thenReturn(true);
      sapling.when(() -> JLibrustzcash.librustzcashSaplingCheckOutputNew(any())).thenReturn(true);
      sapling.when(() -> JLibrustzcash.librustzcashSaplingFinalCheckNew(any())).thenReturn(true);
      // Allow cold mocks and scheduling pauses in successful calls without slowing timeout cases.
      long budgetUs = completion == Completion.ON_TIME ? ON_TIME_BUDGET_US : TIMEOUT_BUDGET_US;
      Deadline deadline = new Deadline(budgetUs);
      ControlledTasks tasks = new ControlledTasks(completion, deadline);
      PrecompiledContract contract = transferProof
          ? new VerifyTransferProof() : new BatchValidateSign();
      byte[] input = transferProof ? transferInput() : signatureInput();
      // Initialize mocks before the first CPU deadline is read.
      Repository repository = mock(Repository.class);
      when(repository.newRepositoryChild()).thenReturn(repository);
      ProgramInvoke invoke = mock(ProgramInvoke.class);
      when(invoke.getContractAddress()).thenReturn(new DataWord(123));
      when(invoke.getCallerAddress()).thenReturn(new DataWord(123));
      when(invoke.getDeposit()).thenReturn(repository);
      when(invoke.getEnergyLimit()).thenReturn(ENERGY_LIMIT);
      when(invoke.getVmShouldEndInUs()).thenAnswer(ignored -> deadline.get());
      Program program = new Program(new byte[]{(byte) Op.CALL}, new byte[20], invoke,
          new InternalTransaction(Transaction.getDefaultInstance(), TrxType.TRX_UNKNOWN_TYPE));
      program.setRootTransactionId(new byte[32]);
      program.memorySave(0, input);
      // CALL arguments are supplied directly so the only executed opcode is the precompile CALL.
      program.stackPush(new DataWord(128));
      program.stackPush(new DataWord(OUTPUT_OFFSET));
      program.stackPush(new DataWord(input.length));
      program.stackPush(DataWord.ZERO());
      program.stackPush(DataWord.ZERO());
      DataWord address = new DataWord(transferProof ? 0x1000002 : 9);
      program.stackPush(address);
      program.stackPush(new DataWord(CALL_ENERGY));
      // CALL costs 40 plus memory expansion from 11/65 to 132 words (397/227),
      // Then the precompile costs 1500/200000. Pin the totals independently of the implementation.
      long normalEnergy = transferProof ? 200_267 : 1_937;
      Transaction transaction = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
          .addContract(Transaction.Contract.newBuilder()
              .setType(ContractType.TriggerSmartContract)))
          .build();
      TransactionContext context = new TransactionContext(null,
          new TransactionCapsule(transaction), null, false, false);

      try (MockedStatic<PrecompiledContracts> precompiles =
          mockStatic(PrecompiledContracts.class, CALLS_REAL_METHODS);
          MockedConstruction<VMActuator> actuators = mockConstruction(VMActuator.class,
              withSettings().defaultAnswer(CALLS_REAL_METHODS), (actuator, construction) -> {
                // Bypass DB-backed validation only; execute() and its real VM are retained.
                doNothing().when(actuator).validate(any());
                setField(actuator, "program", program);
                setField(actuator, "rootRepository", repository);
                actuator.setTrxType(TrxType.TRX_CONTRACT_CALL_TYPE);
              });
          MockedConstruction<FutureTask> submissions = mockConstruction(FutureTask.class,
              (future, construction) -> controlSubmission(future, construction, tasks))) {
        precompiles.when(() -> PrecompiledContracts.getContractForAddress(address))
            .thenReturn(contract);
        RuntimeImpl runtime = new RuntimeImpl();
        runtime.execute(context);
        assertEquals(1, actuators.constructed().size());
        assertEquals(1, program.getPC());
        assertTrue(program.isStopped());
        assertEquals(Op.CALL, program.getLastOp() & 0xff);
        // These checks prevent a test passing through malformed-input or insufficient-energy exits.
        assertEquals(transferProof ? 3 : 1, tasks.futures.size());
        assertEquals(tasks.futures.size(), submissions.constructed().size());
        assertEquals(DataWord.ONE(), program.getStack().peek());
        ProgramResult result = runtime.getResult();
        boolean outOfTime = completion != Completion.ON_TIME && mode == NodeMode.ENERGY_ADJUSTMENT;
        if (outOfTime) {
          assertTrue(result.getException() instanceof Program.OutOfTimeException);
          assertEquals(contractResult.OUT_OF_TIME, result.getResultCode());
          assertEquals(ENERGY_LIMIT, result.getEnergyUsed());
          assertTrue(result.getRuntimeError().contains("TX_LAST_OP"));
          assertTrue(tasks.futures.stream().allMatch(Future::isCancelled));
        } else {
          assertNull(result.getException());
          assertEquals(contractResult.SUCCESS, result.getResultCode());
          assertEquals(normalEnergy, result.getEnergyUsed());
          boolean dropped = !transferProof && completion == Completion.LATCH_TIMEOUT;
          byte[] expected = new byte[transferProof ? 96 : 32];
          if (!dropped) {
            expected[transferProof ? 31 : 0] = 1;
          }
          byte[] actual = program.getReturnDataBufferData(DataWord.ZERO(),
              program.getReturnDataBufferSize());
          assertArrayEquals(expected, actual);
          assertEquals(dropped, tasks.futures.get(0).isCancelled());
        }
        if (completion == Completion.PUBLICATION_TIMEOUT && mode == NodeMode.ENERGY_ADJUSTMENT) {
          assertEquals(1, tasks.timedReads);
          assertEquals(0, tasks.unboundedReads);
        }
      }
    } finally {
      parameter.setDebug(previousDebug);
      parameter.setSolidityNode(previousSolidity);
      VMConfig.clearLocalSnapshot();
    }
  }

  /**
   * Intercept the default ThreadPoolExecutor's FutureTask construction on the submitting thread.
   * Its mocked run() is a no-op on pool threads; result reads use the controlled task below.
   */
  private static void controlSubmission(FutureTask<?> future,
      MockedConstruction.Context construction, ControlledTasks tasks) throws Exception {
    Future<?> result = tasks.submit((Callable<?>) construction.arguments().get(0));
    doAnswer(ignored -> result.get()).when(future).get();
    doAnswer(call -> result.get(call.getArgument(0), call.getArgument(1)))
        .when(future).get(anyLong(), any(TimeUnit.class));
    when(future.cancel(anyBoolean())).thenAnswer(call -> result.cancel(call.getArgument(0)));
    when(future.isCancelled()).thenAnswer(ignored -> result.isCancelled());
    when(future.isDone()).thenAnswer(ignored -> result.isDone());
  }

  private static void setField(Object object, String name, Object value) throws Exception {
    Field field = VMActuator.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(object, value);
  }

  private static byte[] signatureInput() {
    byte[] data = new byte[352];
    byte[] hash = new byte[32];
    hash[0] = 42;
    ECKey key = ECKey.fromPrivate(new byte[]{1});
    byte[] signature = key.sign(hash).toByteArray();
    System.arraycopy(hash, 0, data, 0, 32);
    putWord(data, 32, 96);
    putWord(data, 64, 288);
    putWord(data, 96, 1);
    putWord(data, 128, 32);
    putWord(data, 160, signature.length);
    System.arraycopy(signature, 0, data, 192, signature.length);
    putWord(data, 288, 1);
    System.arraycopy(new DataWord(key.getAddress()).getData(), 0, data, 320, 32);
    return data;
  }

  private static byte[] transferInput() {
    byte[] data = new byte[2080];
    putWord(data, 0, 1312);
    putWord(data, 32, 1664);
    putWord(data, 64, 1760);
    putWord(data, 1312, 1);
    putWord(data, 1664, 1);
    putWord(data, 1760, 1);
    return data;
  }

  private static void putWord(byte[] data, int offset, int value) {
    System.arraycopy(new DataWord(value).getData(), 0, data, offset, 32);
  }

  /** Start the fixed invocation deadline on its first read, after setup and class loading. */
  private static final class Deadline {
    private final long budgetUs;
    private long deadlineUs;

    private Deadline(long budgetUs) {
      this.budgetUs = budgetUs;
    }

    private long get() {
      if (deadlineUs == 0) {
        deadlineUs = System.nanoTime() / 1_000 + budgetUs;
      }
      return deadlineUs;
    }

    private void awaitExpiry() throws InterruptedException {
      long remaining = (get() + 1) * 1_000 - System.nanoTime();
      while (remaining > 0) {
        new CountDownLatch(1).await(remaining, TimeUnit.NANOSECONDS);
        remaining = (get() + 1) * 1_000 - System.nanoTime();
      }
    }
  }

  /**
   * Controlled worker scheduling: defer work past the latch deadline, or finish its callable (and
   * count down the real latch) but hold Future publication. No pool replacement or fixed sleeps.
   * Callables run on the test thread so Sapling primitives can be isolated from native proof setup.
   */
  private static final class ControlledTasks {
    private final Completion completion;
    private final Deadline deadline;
    private final List<Future<?>> futures = new ArrayList<>();
    private int timedReads;
    private int unboundedReads;

    private ControlledTasks(Completion completion, Deadline deadline) {
      this.completion = completion;
      this.deadline = deadline;
    }

    private <T> Future<T> submit(Callable<T> task) {
      ControlledFuture<T> future = new ControlledFuture<>(task);
      if (completion != Completion.LATCH_TIMEOUT) {
        future.finishCallable();
      }
      futures.add(future);
      return future;
    }

    private final class ControlledFuture<T> implements Future<T> {
      private final Callable<T> task;
      private T value;
      private Exception failure;
      private boolean finished;
      private boolean published;
      private boolean cancelled;

      private ControlledFuture(Callable<T> task) {
        this.task = task;
      }

      private void finishCallable() {
        if (!finished) {
          try {
            value = task.call();
          } catch (Exception e) {
            failure = e;
          }
          finished = true;
          published = completion == Completion.ON_TIME;
        }
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
        unboundedReads++;
        if (completion != Completion.ON_TIME) {
          deadline.awaitExpiry();
        }
        return publish();
      }

      @Override
      public T get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        timedReads++;
        if (completion != Completion.ON_TIME) {
          new CountDownLatch(1).await(timeout, unit);
          throw new TimeoutException("worker result held past the invocation deadline");
        }
        return publish();
      }

      private T publish() throws ExecutionException {
        if (cancelled) {
          throw new CancellationException();
        }
        finishCallable();
        if (failure != null) {
          throw new ExecutionException(failure);
        }
        published = true;
        return value;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
          return false;
        }
        cancelled = true;
        return true;
      }

      @Override
      public boolean isCancelled() {
        return cancelled;
      }

      @Override
      public boolean isDone() {
        return cancelled || published;
      }
    }

  }
}
