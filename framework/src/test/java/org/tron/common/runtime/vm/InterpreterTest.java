/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.runtime.vm;

import static org.junit.Assert.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.InternalTransaction.TrxType;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.JumpTable;
import org.tron.core.vm.Op;
import org.tron.core.vm.Operation;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvokeMockImpl;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class InterpreterTest {

  private ProgramInvokeMockImpl invoke;
  private Program program;
  private final JumpTable jumpTable = OperationRegistry.newBaseOperationSet();

  @BeforeClass
  public static void init() {
    CommonParameter.getInstance().setDebug(true);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void testVMException() throws ContractValidateException {
    byte[] op = {0x5b, 0x60, 0x00, 0x56};
    // 0x5b      - JUMPTEST
    // 0x60 0x00 - PUSH 0x00
    // 0x56      - JUMP to 0
    Transaction trx = Transaction.getDefaultInstance();
    InternalTransaction interTrx = new InternalTransaction(trx, TrxType.TRX_UNKNOWN_TYPE);
    invoke = new ProgramInvokeMockImpl(op, op);
    program = new Program(op, op, invoke, interTrx);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        Operation operation = jumpTable.get(program.getCurrentOpIntValue());
        if (operation == null) {
          throw Program.Exception.invalidOpCode(program.getCurrentOp());
        }
        program.setLastOp((byte) operation.getOpcode());
        program.verifyStackSize(operation.getRequire());
        //Check not exceeding stack limits
        program.verifyStackOverflow(operation.getRequire(), operation.getRet());

        program.spendEnergy(operation.getEnergyCost(program),
            Op.getNameOf(operation.getOpcode()));
        program.checkCPUTimeLimit(Op.getNameOf(operation.getOpcode()));
        operation.execute(program);
        program.setPreviouslyExecutedOp((byte) operation.getOpcode());
      }
    } catch (Program.OutOfEnergyException e) {
      result = true;
    }

    assertTrue(result);
  }

  @Test
  public void JumpSingleOperation() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    byte[] op = {0x56};
    // 0x56      - JUMP
    Transaction trx = Transaction.getDefaultInstance();
    InternalTransaction interTrx = new InternalTransaction(trx, TrxType.TRX_UNKNOWN_TYPE);
    invoke = new ProgramInvokeMockImpl(op, op);
    program = new Program(op, op, invoke, interTrx);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        Operation operation = jumpTable.get(program.getCurrentOpIntValue());
        if (operation == null) {
          throw Program.Exception.invalidOpCode(program.getCurrentOp());
        }
        program.setLastOp((byte) operation.getOpcode());
        program.verifyStackSize(operation.getRequire());
        //Check not exceeding stack limits
        program.verifyStackOverflow(operation.getRequire(), operation.getRet());

        program.spendEnergy(operation.getEnergyCost(program),
            Op.getNameOf(operation.getOpcode()));
        program.checkCPUTimeLimit(Op.getNameOf(operation.getOpcode()));
        operation.execute(program);
        program.setPreviouslyExecutedOp((byte) operation.getOpcode());
      }
    } catch (Program.StackTooSmallException e) {
      // except to get stack too small exception for Jump
      result = true;
    }

    assertTrue(result);
  }

  @Test
  public void JumpToInvalidDestination() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    byte[] op = {0x60, 0x20, 0x56};
    // 0x60      - PUSH1
    // 0x20      - 20
    // 0x56      - JUMP
    Transaction trx = Transaction.getDefaultInstance();
    InternalTransaction interTrx = new InternalTransaction(trx, TrxType.TRX_UNKNOWN_TYPE);
    invoke = new ProgramInvokeMockImpl(op, op);
    program = new Program(op, op, invoke, interTrx);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        Operation operation = jumpTable.get(program.getCurrentOpIntValue());
        if (operation == null) {
          throw Program.Exception.invalidOpCode(program.getCurrentOp());
        }
        program.setLastOp((byte) operation.getOpcode());
        program.verifyStackSize(operation.getRequire());
        //Check not exceeding stack limits
        program.verifyStackOverflow(operation.getRequire(), operation.getRet());

        program.spendEnergy(operation.getEnergyCost(program),
            Op.getNameOf(operation.getOpcode()));
        program.checkCPUTimeLimit(Op.getNameOf(operation.getOpcode()));
        operation.execute(program);
        program.setPreviouslyExecutedOp((byte) operation.getOpcode());
      }
    } catch (Program.BadJumpDestinationException e) {
      // except to get BadJumpDestinationException for Jump
      Assert.assertTrue(e.getMessage().contains("Operation with pc isn't 'JUMPDEST': PC[32];"));
      result = true;
    }

    assertTrue(result);
  }

  @Test
  public void JumpToLargeNumberDestination() throws ContractValidateException {
    invoke = new ProgramInvokeMockImpl();
    byte[] op = {0x64, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x56};
    // 0x60              - PUSH5
    // 0x7F7F7F7F7F      - 547599908735
    // 0x56              - JUMP
    Transaction trx = Transaction.getDefaultInstance();
    InternalTransaction interTrx = new InternalTransaction(trx, TrxType.TRX_UNKNOWN_TYPE);
    invoke = new ProgramInvokeMockImpl(op, op);
    program = new Program(op, op, invoke, interTrx);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        Operation operation = jumpTable.get(program.getCurrentOpIntValue());
        if (operation == null) {
          throw Program.Exception.invalidOpCode(program.getCurrentOp());
        }
        program.setLastOp((byte) operation.getOpcode());
        program.verifyStackSize(operation.getRequire());
        //Check not exceeding stack limits
        program.verifyStackOverflow(operation.getRequire(), operation.getRet());

        program.spendEnergy(operation.getEnergyCost(program),
            Op.getNameOf(operation.getOpcode()));
        program.checkCPUTimeLimit(Op.getNameOf(operation.getOpcode()));
        operation.execute(program);
        program.setPreviouslyExecutedOp((byte) operation.getOpcode());
      }
    } catch (Program.BadJumpDestinationException e) {
      // except to get BadJumpDestinationException for Jump
      Assert.assertTrue(e.getMessage().contains("Operation with pc isn't 'JUMPDEST': PC[-1];"));
      result = true;
    }

    assertTrue(result);
  }
}
