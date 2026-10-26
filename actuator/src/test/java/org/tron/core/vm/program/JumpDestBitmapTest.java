package org.tron.core.vm.program;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.tron.core.vm.Op;

public class JumpDestBitmapTest {

  @Test
  public void bitmapMatchesTheOldJumpDestSet() {
    assertSame(new byte[0]);
    assertSame(new byte[] {op(Op.JUMPDEST)});
    assertSame(new byte[] {op(Op.PUSH1), op(Op.JUMPDEST), op(Op.JUMPDEST)});
    assertSame(new byte[] {op(Op.PUSH1), op(Op.JUMPDEST)});
    assertSame(truncatedPush32());
    assertSame(everyPushWidth());
    assertSame(allJumpDests(300));

    ProgramPrecompile analysis = ProgramPrecompile.compile(new byte[] {op(Op.JUMPDEST)});
    assertEquals(false, analysis.hasJumpDest(-1));
    assertEquals(false, analysis.hasJumpDest(1));
    assertEquals(false, analysis.hasJumpDest(Integer.MIN_VALUE));
  }

  private static void assertSame(byte[] ops) {
    ProgramPrecompile analysis = ProgramPrecompile.compile(ops);
    Set<Integer> expected = oldJumpDests(ops);
    for (int pc = -2; pc <= ops.length + 2; pc++) {
      assertEquals(ops.length + " pc " + pc, expected.contains(pc), analysis.hasJumpDest(pc));
    }
  }

  private static Set<Integer> oldJumpDests(byte[] ops) {
    Set<Integer> jumpDest = new HashSet<>();
    for (int i = 0; i < ops.length; ++i) {
      int op = ops[i] & 0xff;
      if (op == Op.JUMPDEST) {
        jumpDest.add(i);
      }
      if (op >= Op.PUSH1 && op <= Op.PUSH32) {
        i += op - Op.PUSH1 + 1;
      }
    }
    return jumpDest;
  }

  private static byte[] truncatedPush32() {
    byte[] ops = new byte[10];
    ops[0] = op(Op.PUSH32);
    ops[3] = op(Op.JUMPDEST);
    return ops;
  }

  private static byte[] everyPushWidth() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int width = 1; width <= 32; width++) {
      for (int align = 0; align < 8; align++) {
        for (int pad = 0; pad < align; pad++) {
          out.write(0);
        }
        out.write(op(Op.PUSH1 + width - 1));
        for (int n = 0; n < width; n++) {
          out.write(n == width - 1 ? op(Op.JUMPDEST) : 0);
        }
        out.write(op(Op.JUMPDEST));
      }
    }
    return out.toByteArray();
  }

  private static byte op(int opcode) {
    return (byte) opcode;
  }

  private static byte[] allJumpDests(int length) {
    byte[] ops = new byte[length];
    for (int i = 0; i < ops.length; i++) {
      ops[i] = op(Op.JUMPDEST);
    }
    return ops;
  }
}
