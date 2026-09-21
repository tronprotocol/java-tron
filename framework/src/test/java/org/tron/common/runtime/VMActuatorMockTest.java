package org.tron.common.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;

import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.actuator.VMActuator;
import org.tron.core.db.TransactionContext;
import org.tron.core.vm.JumpTable;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.VM;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.repository.Repository;

public class VMActuatorMockTest {

  @BeforeClass
  public static void init() {
    // Warm up the registry before VM execution timing starts.
    OperationRegistry.init();
  }

  @Test
  public void constantCallUsesDedicatedJumpTable() throws Exception {
    try (MockedStatic<VM> vmMock = Mockito.mockStatic(VM.class)) {
      Program program = Mockito.mock(Program.class);
      Mockito.when(program.getResult()).thenReturn(new ProgramResult());

      VMActuator actuator = new VMActuator(true);
      Field f = VMActuator.class.getDeclaredField("program");
      f.setAccessible(true);
      f.set(actuator, program);

      TransactionContext context = Mockito.mock(TransactionContext.class);
      Mockito.when(context.getProgramResult()).thenReturn(new ProgramResult());

      actuator.execute(context);

      JumpTable transactionTable = OperationRegistry.getTable(false);
      JumpTable constantCallTable = OperationRegistry.getTable(true);
      vmMock.verify(() -> VM.play(any(), same(constantCallTable)));
      vmMock.verify(() -> VM.play(any(), argThat(table -> table != transactionTable)));
    }
  }

  @Test
  public void nonConstantCallUsesSharedJumpTable() throws Exception {
    try (MockedStatic<VM> vmMock = Mockito.mockStatic(VM.class)) {
      Program program = Mockito.mock(Program.class);
      Mockito.when(program.getResult()).thenReturn(new ProgramResult());

      VMActuator actuator = new VMActuator(false);
      Field f = VMActuator.class.getDeclaredField("program");
      f.setAccessible(true);
      f.set(actuator, program);

      Field repositoryField = VMActuator.class.getDeclaredField("rootRepository");
      repositoryField.setAccessible(true);
      repositoryField.set(actuator, Mockito.mock(Repository.class));

      TransactionContext context = Mockito.mock(TransactionContext.class);
      Mockito.when(context.getProgramResult()).thenReturn(new ProgramResult());

      actuator.execute(context);

      // The non-constant call must receive the shared consensus table itself.
      JumpTable shared = OperationRegistry.getTable(false);
      vmMock.verify(() -> VM.play(any(), same(shared)));
    }
  }

  private void runCatchPathTest(Throwable thrownByVm, boolean osakaOn, int expectedSize)
      throws Exception {
    boolean prevOsaka = VMConfig.allowTvmOsaka();
    VMConfig.initAllowTvmOsaka(osakaOn ? 1 : 0);
    try (MockedStatic<VM> vmMock = Mockito.mockStatic(VM.class)) {
      Program program = Mockito.mock(Program.class);
      ProgramResult result = new ProgramResult();
      result.addLogInfo(new LogInfo(new byte[20], Collections.emptyList(), new byte[0]));
      result.addDeleteAccount(new DataWord(1));
      Mockito.when(program.getResult()).thenReturn(result);

      vmMock.when(() -> VM.play(any(), any())).thenThrow(thrownByVm);

      VMActuator actuator = new VMActuator(false);
      Field f = VMActuator.class.getDeclaredField("program");
      f.setAccessible(true);
      f.set(actuator, program);

      TransactionContext context = Mockito.mock(TransactionContext.class);
      Mockito.when(context.getProgramResult()).thenReturn(new ProgramResult());

      actuator.execute(context);

      Assert.assertEquals(expectedSize, result.getLogInfoList().size());
      Assert.assertEquals(expectedSize, result.getDeleteAccounts().size());
    } finally {
      VMConfig.initAllowTvmOsaka(prevOsaka ? 1 : 0);
    }
  }

  @Test
  public void osakaClearsLogOnOutOfTime() throws Exception {
    runCatchPathTest(new Program.OutOfTimeException("timeout"), true, 0);
  }

  @Test
  public void osakaClearsLogOnJvmStackOverflow() throws Exception {
    runCatchPathTest(new Program.JVMStackOverFlowException(), true, 0);
  }

  @Test
  public void osakaClearsLogOnThrowable() throws Exception {
    runCatchPathTest(new RuntimeException("boom"), true, 0);
  }

  @Test
  public void preOsakaKeepsLogOnOutOfTime() throws Exception {
    runCatchPathTest(new Program.OutOfTimeException("timeout"), false, 1);
  }

  @Test
  public void preOsakaKeepsLogOnJvmStackOverflow() throws Exception {
    runCatchPathTest(new Program.JVMStackOverFlowException(), false, 1);
  }

  @Test
  public void preOsakaKeepsLogOnThrowable() throws Exception {
    runCatchPathTest(new RuntimeException("boom"), false, 1);
  }
}
