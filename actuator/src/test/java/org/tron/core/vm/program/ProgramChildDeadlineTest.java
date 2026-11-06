package org.tron.core.vm.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.vm.MessageCall;
import org.tron.core.vm.Op;
import org.tron.core.vm.VM;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.trace.OpActions;
import org.tron.protos.Protocol.AccountType;

@RunWith(Parameterized.class)
public class ProgramChildDeadlineTest {
  private final int opcode;
  private final boolean enabled;
  private final boolean expired;

  @Parameterized.Parameters(name = "opcode={0}, enabled={1}, expired={2}")
  public static Collection<Object[]> parameters() {
    Collection<Object[]> cases = new ArrayList<>();
    for (int opcode : new int[]{Op.CALL, Op.CALLTOKEN, Op.CALLCODE, Op.DELEGATECALL, Op.STATICCALL,
        Op.CREATE, Op.CREATE2}) {
      for (boolean enabled : new boolean[]{false, true}) {
        for (boolean expired : new boolean[]{false, true}) {
          cases.add(new Object[]{opcode, enabled, expired});
        }
      }
    }
    return cases;
  }

  public ProgramChildDeadlineTest(int opcode, boolean enabled, boolean expired) {
    this.opcode = opcode;
    this.enabled = enabled;
    this.expired = expired;
  }

  @After
  public void restoreConfig() {
    VMConfig.clearLocalSnapshot();
  }

  @Test
  public void checksDeadlineBeforeMergingOrCommittingChild() {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = enabled;
    snapshot.allowTvmConstantinople = true;
    snapshot.allowTvmIstanbul = true;
    snapshot.allowTvmTransferTrc10 = true;
    snapshot.allowMultiSign = true;
    VMConfig.setLocalSnapshot(snapshot);
    Repository repository = mock(Repository.class);
    Repository childRepository = mock(Repository.class);
    when(repository.newRepositoryChild()).thenReturn(childRepository);
    byte[] target = new byte[21];
    target[0] = 0x41;
    target[20] = 2;
    when(repository.getAccount(eq(target))).thenReturn(
        new AccountCapsule(ByteString.copyFrom(target), AccountType.Contract));
    when(repository.getCode(any(byte[].class))).thenReturn(new byte[]{0});
    ProgramInvoke invoke = mock(ProgramInvoke.class);
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
    when(invoke.getEnergyLimit()).thenReturn(10000L);
    InternalTransaction root = mock(InternalTransaction.class);
    when(root.getHash()).thenReturn(new byte[32]);
    DeadlineProgram parent = new DeadlineProgram(invoke, root);
    parent.setRootTransactionId(new byte[32]);
    parent.memorySave(0, new byte[]{0});
    InternalTransaction nested = mock(InternalTransaction.class);

    try (MockedStatic<VM> vm = mockStatic(VM.class)) {
      vm.when(() -> VM.play(any(Program.class), any())).thenAnswer(invocation -> {
        Program child = invocation.getArgument(0);
        child.setNonce(7);
        child.getResult().addInternalTransaction(nested);
        child.getTrace().addOp((byte) Op.STOP, 0, 1, DataWord.ONE(), new OpActions());
        return null;
      });
      Runnable execute = () -> {
        if (opcode == Op.CREATE) {
          parent.createContract(DataWord.ZERO(), DataWord.ZERO(), DataWord.ONE());
        } else if (opcode == Op.CREATE2) {
          parent.createContract2(DataWord.ZERO(), DataWord.ZERO(), DataWord.ONE(), DataWord.ONE());
        } else {
          parent.spendEnergy(1000, "reserve call energy");
          parent.callToAddress(new MessageCall(opcode, new DataWord(1000), new DataWord(target),
              DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO(), DataWord.ZERO(),
              opcode == Op.CALLTOKEN ? new DataWord(1_000_001) : DataWord.ZERO(),
              opcode == Op.CALLTOKEN));
        }
      };
      if (enabled && expired) {
        assertThrows(Program.OutOfTimeException.class, execute::run);
        verify(childRepository, never()).commit();
        assertEquals(1, parent.getNonce());
        assertFalse(parent.getResult().getInternalTransactions().contains(nested));
        assertTrue(parent.getTrace().getOps().isEmpty());
        assertTrue(parent.getStack().isEmpty());
      } else {
        execute.run();
        verify(childRepository).commit();
        assertEquals(7, parent.getNonce());
        assertTrue(parent.getResult().getInternalTransactions().contains(nested));
        assertEquals(1, parent.getTrace().getOps().size());
        assertFalse(parent.getStack().pop().isZero());
      }
      vm.verify(() -> VM.play(any(Program.class), any()));
      assertEquals(enabled ? 1 : 0, parent.deadlineChecks);
    }
  }

  private class DeadlineProgram extends Program {
    private int deadlineChecks;

    private DeadlineProgram(ProgramInvoke invoke, InternalTransaction root) {
      super(new byte[0], new byte[21], invoke, root);
    }

    @Override
    public void checkCPUTimeLimit(String opName) {
      deadlineChecks++;
      if (expired) {
        throw Program.Exception.notEnoughTime(opName);
      }
    }
  }
}
