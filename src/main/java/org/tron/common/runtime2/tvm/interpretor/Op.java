package org.tron.common.runtime2.tvm.interpretor;

import static org.tron.common.crypto.Hash.sha3;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.VMConstant;
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
          executor.spendEnergy(OpCode.Tier.LowTier.asInt(), op.name());

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
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.sub(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  DIV(0x04, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.LowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.div(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  SDIV(0x05, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.LowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.sDiv(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  MOD(0x06, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.LowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.mod(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  SMOD(0x07, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.LowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.sMod(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  ADDMOD(0x08, 3, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.MidTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();
          DataWord word3 = executor.stackPop();
          word1.addmod(word2, word3);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  MULMOD(0x09, 3, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.MidTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();
          DataWord word3 = executor.stackPop();
          word1.mulmod(word2, word3);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  EXP(0x0a, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();
          int bytesOccupied = word2.bytesOccupied();
          int energyCost = Costs.EXP_ENERGY + Costs.EXP_BYTE_ENERGY * bytesOccupied;

          executor.spendEnergy(energyCost, op.name());

          word1.exp(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  SIGNEXTEND(0x0b, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {

          executor.spendEnergy(OpCode.Tier.LowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          BigInteger k = word1.value();
          if (k.compareTo(VMConstant._32_) < 0) {
            DataWord word2 = executor.stackPop();

            word2.signExtend(k.byteValue());
            executor.stackPush(word2);
          }
          executor.step();
        }
      }
  ),
  LT(0X10, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          if (word1.value().compareTo(word2.value()) < 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  GT(0X11, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          if (word1.value().compareTo(word2.value()) > 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          executor.stackPush(word1);
          executor.step();

        }
      }
  ),
  SLT(0X12, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          if (word1.sValue().compareTo(word2.sValue()) < 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  SGT(0X13, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          if (word1.sValue().compareTo(word2.sValue()) > 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          executor.stackPush(word1);
          executor.step();

        }
      }
  ),
  EQ(0X14, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          if (word1.xor(word2).isZero()) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  ISZERO(0X15, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          if (word1.isZero()) {
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }

          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  AND(0X16, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.and(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  OR(0X17, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.or(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  XOR(0X18, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.xor(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  NOT(0X19, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          word1.bnot();
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  BYTE(0X1a, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result;
          if (word1.value().compareTo(VMConstant._32_) < 0) {
            byte tmp = word2.getData()[word1.intValue()];
            word2.and(DataWord.ZERO);
            word2.getData()[31] = tmp;
            result = word2;
          } else {
            result = new DataWord();
          }

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SHL(0X1b, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result = word2.shiftLeft(word1);

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SHR(0X1c, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result = word2.shiftRight(word1);

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SAR(0X1d, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result = word2.shiftRightSigned(word1);

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SHA3(0X20, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor, DataWord adjustedCallEnergy) {
          DataWord memOffsetData = executor.stackPop();
          DataWord lengthData = executor.stackPop();

          long energy = Costs.SHA3 + calcMemEnergy(executor.getMemSize(),
              memNeeded(memOffsetData, lengthData), 0, op);
          long chunkUsed = (lengthData.longValueSafe() + 31) / 32;
          energy += chunkUsed * Costs.SHA3_WORD;

          executor.spendEnergy(energy, op.name());

          byte[] buffer = executor
              .memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());

          byte[] encoded = sha3(buffer);
          DataWord word = new DataWord(encoded);

          executor.stackPush(word);
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
