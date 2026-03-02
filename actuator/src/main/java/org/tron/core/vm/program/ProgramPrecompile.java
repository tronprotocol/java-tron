package org.tron.core.vm.program;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.Op;
import org.tron.core.vm.config.VMConfig;

@Slf4j(topic = "VM")
public class ProgramPrecompile {

  private final Set<Integer> jumpDest = new HashSet<>();

  public static ProgramPrecompile compile(byte[] ops) {
    ProgramPrecompile ret = new ProgramPrecompile();
    for (int i = 0; i < ops.length; ++i) {
      int op = ops[i] & 0xff;

      if (op == Op.JUMPDEST) {
        ret.jumpDest.add(i);
      }

      if (op >= Op.PUSH1 && op <= Op.PUSH32) {
        i += op - Op.PUSH1 + 1;
      }
    }
    return ret;
  }

  public static byte[] getCode(byte[] ops) {
    for (int i = 0; i < ops.length; ++i) {

      int op = ops[i] & 0xff;

      if (op == Op.RETURN && i + 1 < ops.length && ((ops[i + 1]) & 0xff) == Op.STOP) {
        byte[] ret;
        i++;
        ret = new byte[ops.length - i - 1];

        System.arraycopy(ops, i + 1, ret, 0, ops.length - i - 1);
        return ret;
      }

      if (op >= Op.PUSH1 && op <= Op.PUSH32) {
        i += op - Op.PUSH1 + 1;
      }
    }
    if (VMConfig.allowTvmConstantinople()) {
      return new byte[0];
    } else {
      return new byte[DataWord.WORD_SIZE];
    }
  }

  public boolean hasJumpDest(int pc) {
    return jumpDest.contains(pc);
  }
}
