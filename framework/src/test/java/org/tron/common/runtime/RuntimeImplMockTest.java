package org.tron.common.runtime;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.tron.core.vm.program.Program;



@Slf4j
public class RuntimeImplMockTest {
  @After
  public void  clearMocks() {

  }

  @Test
  public void testSetResultCode1() throws Exception {
    RuntimeImpl runtime = new RuntimeImpl();
    ProgramResult programResult = new ProgramResult();
    Method privateMethod = RuntimeImpl.class.getDeclaredMethod(
        "setResultCode", ProgramResult.class);
    privateMethod.setAccessible(true);

    Program.BadJumpDestinationException badJumpDestinationException
        = new Program.BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", 0);
    programResult.setException(badJumpDestinationException);
    privateMethod.invoke(runtime, programResult);

    Program.OutOfTimeException outOfTimeException
        = new Program.OutOfTimeException("CPU timeout for 0x0a executing");
    programResult.setException(outOfTimeException);
    privateMethod.invoke(runtime, programResult);

    Program.PrecompiledContractException precompiledContractException
        = new Program.PrecompiledContractException("precompiled contract exception");
    programResult.setException(precompiledContractException);
    privateMethod.invoke(runtime, programResult);

    Program.StackTooSmallException stackTooSmallException
        = new Program.StackTooSmallException("Expected stack size %d but actual %d;", 100, 10);
    programResult.setException(stackTooSmallException);
    privateMethod.invoke(runtime, programResult);

    Program.JVMStackOverFlowException jvmStackOverFlowException
        = new Program.JVMStackOverFlowException();
    programResult.setException(jvmStackOverFlowException);
    privateMethod.invoke(runtime, programResult);
  }

}