/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core.vm.program;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.Op;
import org.tron.core.vm.config.VMConfig;


@Slf4j(topic = "VM")
/**
 * Created by Anton Nashatyrev on 06.02.2017.
 */
public class ProgramPrecompile {

  private Set<Integer> jumpdest = new HashSet<>();

  public static ProgramPrecompile compile(byte[] ops) {
    ProgramPrecompile ret = new ProgramPrecompile();
    for (int i = 0; i < ops.length; ++i) {
      int op = ops[i] & 0xff;

      if (op == Op.JUMPDEST) {
        ret.jumpdest.add(i);
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
      return new DataWord(0).getData();
    }
  }

  public boolean hasJumpDest(int pc) {
    return jumpdest.contains(pc);
  }
}
