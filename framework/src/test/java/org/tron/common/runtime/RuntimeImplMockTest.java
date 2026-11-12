package org.tron.common.runtime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.core.ChainBaseManager;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.program.Program;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

public class RuntimeImplMockTest {

  @Test
  public void nonRevertResultsDoNotReadChainProperties() throws Exception {
    try (MockedStatic<ChainBaseManager> manager = mockStatic(ChainBaseManager.class)) {
      assertResultCode(new ProgramResult(), contractResult.SUCCESS);
      ProgramResult error = new ProgramResult();
      error.setRuntimeError("runtime error");
      assertResultCode(error, contractResult.UNKNOWN);
      for (Object[] mapping : exceptionMappings()) {
        ProgramResult result = new ProgramResult();
        result.setException((RuntimeException) mapping[0]);
        assertResultCode(result, (contractResult) mapping[1]);
      }
      manager.verifyNoInteractions();
    }
  }

  @Test
  public void revertPriorityIsGatedByResourceOptimization() throws Exception {
    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    ChainBaseManager manager = mock(ChainBaseManager.class);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);

    try (MockedStatic<ChainBaseManager> managers = mockStatic(ChainBaseManager.class)) {
      managers.when(ChainBaseManager::getInstance).thenReturn(manager);
      for (boolean enabled : new boolean[]{false, true}) {
        when(properties.allowTvmResourceOptimization()).thenReturn(enabled);
        ProgramResult revert = new ProgramResult();
        revert.setRevert();
        assertResultCode(revert, contractResult.REVERT);

        for (Object[] mapping : exceptionMappings()) {
          ProgramResult result = new ProgramResult();
          result.setRevert();
          result.setException((RuntimeException) mapping[0]);
          contractResult exceptionCode = (contractResult) mapping[1];
          contractResult expected = enabled && exceptionCode != contractResult.UNKNOWN
              ? exceptionCode : contractResult.REVERT;
          assertResultCode(result, expected);
        }
      }
    }
  }

  private Object[][] exceptionMappings() {
    return new Object[][]{
        {new Program.IllegalOperationException("illegal"), contractResult.ILLEGAL_OPERATION},
        {new Program.OutOfEnergyException("energy"), contractResult.OUT_OF_ENERGY},
        {new Program.BadJumpDestinationException("jump"), contractResult.BAD_JUMP_DESTINATION},
        {new Program.OutOfTimeException("timeout"), contractResult.OUT_OF_TIME},
        {new Program.OutOfMemoryException("memory"), contractResult.OUT_OF_MEMORY},
        {new Program.PrecompiledContractException("precompile"),
            contractResult.PRECOMPILED_CONTRACT},
        {new Program.StackTooSmallException("stack"), contractResult.STACK_TOO_SMALL},
        {mock(Program.StackTooLargeException.class), contractResult.STACK_TOO_LARGE},
        {new Program.JVMStackOverFlowException(), contractResult.JVM_STACK_OVER_FLOW},
        {new Program.TransferException("transfer"), contractResult.TRANSFER_FAILED},
        {new Program.InvalidCodeException("invalid"), contractResult.INVALID_CODE},
        {new RuntimeException("unknown"), contractResult.UNKNOWN}
    };
  }

  private void assertResultCode(ProgramResult result, contractResult expected) throws Exception {
    Method method = RuntimeImpl.class.getDeclaredMethod("setResultCode", ProgramResult.class);
    method.setAccessible(true);
    method.invoke(new RuntimeImpl(), result);
    assertEquals(expected, result.getResultCode());
  }
}
