package org.tron.common.runtime2.tvm.interpretor;

import java.util.Arrays;
import java.util.EnumSet;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.utils.ByteUtil;


public enum Op {
  STOP(0x00, 0, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          //Non consume energy
          executor.setHReturn(ByteUtil.EMPTY_BYTE_ARRAY);
          executor.stop();
        }
      }
  ),
  ADD(0x01, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();
          word1.add(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  MUL(0x02, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(5, op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.mul(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  SUB(0x03, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(3, op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.mul(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  );

  private final byte opcode;
  private final int require;
  private final int ret;
  private final OpExecutor opExecutor;
  private final EnumSet<CallFlags> callFlags;


  Op(int opcode, int require, int ret, OpExecutor opExecutor,
      CallFlags... callFlags) {
    this.opExecutor = opExecutor;
    this.opcode = (byte) opcode;
    this.require = require;
    this.ret = ret;
    this.callFlags = callFlags.length == 0 ? EnumSet.noneOf(CallFlags.class) :
        EnumSet.copyOf(Arrays.asList(callFlags));
  }

  private enum CallFlags {
    /**
     * Indicates that opcode is a call
     */
    Call,

    /**
     * Indicates that the code is executed in the context of the caller
     */
    Stateless,

    /**
     * Indicates that the opcode has value parameter (3rd on stack)
     */
    HasValue,

    /**
     * Indicates that any state modifications are disallowed during the call
     */
    Static,

    /**
     * Indicates that value and message sender are propagated from parent to child scope
     */
    Delegate
  }


  public enum Tier {
    ZeroTier(0),
    BaseTier(2),
    VeryLowTier(3),
    LowTier(5),
    MidTier(8),
    HighTier(10),
    ExtTier(20),
    SpecialTier(1), //TODO #POC9 is this correct?? "multiparam" from cpp
    InvalidTier(0);


    private final int level;

    private Tier(int level) {
      this.level = level;
    }

    public int asInt() {
      return level;
    }
  }


}
