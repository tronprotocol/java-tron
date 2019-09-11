package org.tron.common.runtime2.tvm.interpretor;

import static org.tron.common.crypto.Hash.sha3;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.VMConstant;
import org.tron.common.runtime2.tvm.interpretor.executors.CodeCopyOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.CodeSizeOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.OpExecutor;
import org.tron.common.utils.ByteUtil;


public enum Op {
  STOP(0x00, 0, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          //Non consume energy
          executor.setHReturn(ByteUtil.EMPTY_BYTE_ARRAY);
          executor.stop();
        }
      }
  ),
  ADD(0x01, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {
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
        public void exec(Op op, ContractExecutor executor) {

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
        public void exec(Op op, ContractExecutor executor) {

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
  LT(0x10, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  GT(0x11, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  SLT(0x12, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  SGT(0x13, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  EQ(0x14, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  ISZERO(0x15, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  AND(0x16, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.and(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  OR(0x17, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.or(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  XOR(0x18, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          word1.xor(word2);
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  NOT(0x19, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          word1.bnot();
          executor.stackPush(word1);
          executor.step();
        }
      }
  ),
  BYTE(0x1a, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  SHL(0x1b, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result = word2.shiftLeft(word1);

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SHR(0x1c, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result = word2.shiftRight(word1);

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SAR(0x1d, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());

          DataWord word1 = executor.stackPop();
          DataWord word2 = executor.stackPop();

          final DataWord result = word2.shiftRightSigned(word1);

          executor.stackPush(result);
          executor.step();
        }
      }
  ),
  SHA3(0x20, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
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
  ),
  ADDRESS(0x30, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord address = executor.getContractAddress();
          address = new DataWord(address.getLast20Bytes());

          executor.stackPush(address);
          executor.step();
        }
      }
  ),
  BALANCE(0x31, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.ExtTier.asInt(), op.name());
          DataWord address = executor.stackPop();
          DataWord balance = executor.getBalance(address);

          executor.stackPush(balance);
          executor.step();
        }
      }
  ),
  ORIGIN(0x32, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord originAddress = executor.getOriginAddress();
          originAddress = new DataWord(originAddress.getLast20Bytes());

          executor.stackPush(originAddress);
          executor.step();
        }
      }
  ),
  CALLER(0x33, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord callerAddress = executor.getCallerAddress();

          executor.stackPush(callerAddress);
          executor.step();
        }
      }
  ),
  CALLVALUE(0x34, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord callValue = executor.getCallValue();

          executor.stackPush(callValue);
          executor.step();
        }
      }
  ),
  CALLDATALOAD(0x35, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.VeryLowTier.asInt(), op.name());
          DataWord dataOffs = executor.stackPop();
          DataWord value = executor.getDataValue(dataOffs);

          executor.stackPush(value);
          executor.step();
        }
      }
  ),
  CALLDATASIZE(0x36, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord dataSize = executor.getDataSize();

          executor.stackPush(dataSize);
          executor.step();
        }
      }
  ),
  CALLDATACOPY(0x37, 3, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {

          DataWord memOffsetData = executor.stackPop();
          DataWord dataOffsetData = executor.stackPop();
          DataWord lengthData = executor.stackPop();

          executor.spendEnergy(
              calcMemEnergy(executor.getMemory().size(),
                  memNeeded(memOffsetData, lengthData),
                  lengthData.longValueSafe(), op),
              op.name()
          );

          byte[] msgData = executor.getDataCopy(dataOffsetData, lengthData);

          executor.memorySave(memOffsetData.intValueSafe(), msgData);
          executor.step();
        }
      }
  ),
  CODESIZE(0x38, 0, 1, CodeSizeOpExecutor.getInstance()),
  CODECOPY(0x39, 3, 0, CodeCopyOpExecutor.getInstance()),
  RETURNDATASIZE(0x3d, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord dataSize = executor.getReturnDataBufferSize();

          executor.stackPush(dataSize);
          executor.step();
        }
      }
  ),
  RETURNDATACOPY(0x3e, 3, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {

          DataWord memOffsetData = executor.stackPop();
          DataWord dataOffsetData = executor.stackPop();
          DataWord lengthData = executor.stackPop();

          executor.spendEnergy(
              calcMemEnergy(executor.getMemory().size(),
                  memNeeded(memOffsetData, lengthData),
                  lengthData.longValueSafe(), op),
              op.name()
          );

          byte[] msgData = executor.getReturnDataBufferData(dataOffsetData, lengthData);

          executor.memorySave(memOffsetData.intValueSafe(), msgData);
          executor.step();
        }
      }
  ),
  GASPRICE(0x3a, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());
          DataWord energyPrice = new DataWord(0);

          executor.stackPush(energyPrice);
          executor.step();
        }
      }
  ),
  EXTCODESIZE(0x3b, 1, 1, CodeSizeOpExecutor.getInstance()),
  EXTCODECOPY(0x3c, 4, 1, CodeCopyOpExecutor.getInstance()),
  EXTCODEHASH(0x3f, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Costs.EXT_CODE_HASH, op.name());
          DataWord address = executor.stackPop();
          byte[] codeHash = executor.getCodeHashAt(address);
          executor.stackPush(codeHash);
          executor.step();
        }
      }
  ),
  BLOCKHASH(0x40, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.ExtTier.asInt(), op.name());
          int blockIndex = executor.stackPop().intValueSafe();
          DataWord blockHash = executor.getBlockHash(blockIndex);

          executor.stackPush(blockHash);
          executor.step();
        }
      }
  ),
  COINBASE(0x41, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());

          DataWord target = executor.getCoinbase();

          executor.stackPush(target);
          executor.step();
        }
      }
  ),
  TIMESTAMP(0x42, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());

          DataWord target = executor.getTimestamp();

          executor.stackPush(target);
          executor.step();
        }
      }
  ),
  NUMBER(0x43, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());

          DataWord target = executor.getNumber();

          executor.stackPush(target);
          executor.step();
        }
      }
  ),
  DIFFICULTY(0x44, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());

          DataWord target = executor.getDifficulty();

          executor.stackPush(target);
          executor.step();
        }
      }
  ),
  GASLIMIT(0x45, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());

          executor.stackPush(new DataWord(0));
          executor.step();
        }
      }
  ),
  POP(0x50, 1, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(OpCode.Tier.BaseTier.asInt(), op.name());

          executor.stackPop();
          executor.step();
        }
      }
  ),
  MLOAD(0x51, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          DataWord addr = executor.stackPop();
          DataWord data = executor.memoryLoad(addr);

          executor.spendEnergy(
              calcMemEnergy(executor.getMemory().size(),
                  memNeeded(addr, new DataWord(32)), 0, op), op.name());

          executor.stackPush(data);
          executor.step();
        }
      }
  ),
  MSTORE(0x52, 2, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          DataWord addr = executor.stackPop();
          DataWord value = executor.stackPop();

          executor.spendEnergy(
              calcMemEnergy(executor.getMemory().size(),
                  memNeeded(addr, new DataWord(32)),
                  0, op), op.name());

          executor.memorySave(addr, value);
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
