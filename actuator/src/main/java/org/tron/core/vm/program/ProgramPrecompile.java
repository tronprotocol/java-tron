package org.tron.core.vm.program;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.Op;
import org.tron.core.vm.config.VMConfig;

@Slf4j(topic = "VM")
public class ProgramPrecompile {

  // Bit 1 means compile() would have added this pc to the old HashSet.
  private final byte[] jumpDestBits;
  private final int codeLength;

  private ProgramPrecompile(int codeLength) {
    this.codeLength = codeLength;
    this.jumpDestBits = new byte[(codeLength + 7) >>> 3];
  }

  public static ProgramPrecompile compile(byte[] ops) {
    ProgramPrecompile ret = new ProgramPrecompile(ops.length);
    for (int i = 0; i < ops.length; ++i) {
      int op = ops[i] & 0xff;

      if (op == Op.JUMPDEST) {
        ret.setJumpDest(i);
      }

      if (op >= Op.PUSH1 && op <= Op.PUSH32) {
        i += op - Op.PUSH1 + 1;
      }
    }
    return ret;
  }

  private void setJumpDest(int pc) {
    jumpDestBits[pc >>> 3] |= (byte) (1 << (pc & 7));
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
    if (pc < 0 || pc >= codeLength) {
      return false;
    }
    return (jumpDestBits[pc >>> 3] & (1 << (pc & 7))) != 0;
  }
}
