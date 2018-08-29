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
import org.junit.Test;
import java.io.*;
import org.tron.protos.Protocol.Transaction;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.VM;
import org.tron.common.runtime.vm.program.Program;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeMockImpl;

@Slf4j
public class InterpreterTest {

  private ProgramInvokeMockImpl invoke;
  private Program program;

  @Test
  public void testVMException() {
    VM vm = new VM();
    invoke = new ProgramInvokeMockImpl();
    byte[] op = { 0x5b, 0x60, 0x00, 0x56 };
    // 0x5b      - JUMPTEST
    // 0x60 0x00 - PUSH 0x00
    // 0x56      - JUMP to 0
    Transaction trx = Transaction.getDefaultInstance();
    InternalTransaction interTrx = new InternalTransaction(trx);
    program = new Program(op, invoke, interTrx);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        vm.step(program);
      }
    } catch (Exception e) {
      result = true;
    }

    assertTrue(result);
  }
}
