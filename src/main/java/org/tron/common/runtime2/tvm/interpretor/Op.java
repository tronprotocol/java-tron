package org.tron.common.runtime2.tvm.interpretor;

import static org.tron.common.crypto.Hash.sha3;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime2.tvm.ContractExecutor;
import org.tron.common.runtime2.tvm.VMConstant;
import org.tron.common.runtime2.tvm.interpretor.executors.CallOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.CodeCopyOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.CodeSizeOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.CreateOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.DupOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.LogOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.OpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.PushOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.RetOpExecutor;
import org.tron.common.runtime2.tvm.interpretor.executors.SwapOpExecutor;
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
  EXTCODECOPY(0x3c, 4, 0, CodeCopyOpExecutor.getInstance()),
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
  ),
  MSTORE8(0x53, 2, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          DataWord addr = executor.stackPop();
          DataWord value = executor.stackPop();
          byte[] byteVal = {value.getData()[31]};

          executor.spendEnergy(
              calcMemEnergy(executor.getMemory().size(),
                  memNeeded(addr, new DataWord(1)),
                  0, op), op.name());

          executor.memorySave(addr.intValueSafe(), byteVal);
          executor.step();
        }
      }
  ),
  SLOAD(0x54, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Costs.SLOAD, op.name());
          DataWord key = executor.stackPop();
          DataWord val = executor.storageLoad(key);

          if (val == null) {
            val = key.and(DataWord.ZERO);
          }

          executor.stackPush(val);
          executor.step();
        }
      }
  ),
  SSTORE(0x55, 2, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {

          if (executor.getContractContext().isStatic()) {
            throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
          }

          DataWord addr = executor.stackPop();
          DataWord newValue = executor.stackPop();
          DataWord oldValue = executor.storageLoad(addr);

          long energyCost = 0L;

          if (oldValue == null && !newValue.isZero()) {
            // set a new not-zero value
            energyCost = Costs.SET_SSTORE;
          } else if (oldValue != null && newValue.isZero()) {
            // set zero to an old value
            //no refund in tron
            //env.futureRefundEnergy(energyCosts.getREFUND_SSTORE());
            energyCost = Costs.CLEAR_SSTORE;
          } else {
            // include:
            // [1] oldValue == null && newValue == 0
            // [2] oldValue != null && newValue != 0
            energyCost = Costs.RESET_SSTORE;
          }
          executor.spendEnergy(energyCost, op.name());

          executor.storageSave(addr, newValue);
          executor.step();
        }
      }
  ),
  JUMP(0x56, 1, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Tier.MidTier.asInt(), op.name());

          DataWord pos = executor.stackPop();
          int nextPC = executor.verifyJumpDest(pos);

          executor.setPC(nextPC);
        }
      }
  ),
  JUMPI(0x57, 2, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Tier.HighTier.asInt(), op.name());

          DataWord pos = executor.stackPop();
          DataWord cond = executor.stackPop();

          if (!cond.isZero()) {
            int nextPC = executor.verifyJumpDest(pos);

            executor.setPC(nextPC);
          } else {
            executor.step();
          }
        }
      }
  ),
  PC(0x58, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {

          executor.spendEnergy(Tier.BaseTier.asInt(), op.name());

          int pc = executor.getPC();
          DataWord pcWord = new DataWord(pc);

          executor.stackPush(pcWord);
          executor.step();
        }
      }
  ),
  MSIZE(0x59, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {

          executor.spendEnergy(Tier.BaseTier.asInt(), op.name());

          int memSize = executor.getMemSize();
          DataWord wordMemSize = new DataWord(memSize);
          executor.stackPush(wordMemSize);
          executor.step();
        }
      }
  ),
  GAS(0x5a, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Tier.BaseTier.asInt(), op.name());
          DataWord energy = executor.getContractContext().getEnergyLimitLeft();

          executor.stackPush(energy);
          executor.step();
        }
      }
  ),
  JUMPDEST(0x5b, 0, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Tier.SpecialTier.asInt(), op.name());
          executor.step();
        }
      }
  ),
  PUSH1(0x60, 0, 1, PushOpExecutor.getInstance()),
  PUSH2(0x61, 0, 1, PushOpExecutor.getInstance()),
  PUSH3(0x62, 0, 1, PushOpExecutor.getInstance()),
  PUSH4(0x63, 0, 1, PushOpExecutor.getInstance()),
  PUSH5(0x64, 0, 1, PushOpExecutor.getInstance()),
  PUSH6(0x65, 0, 1, PushOpExecutor.getInstance()),
  PUSH7(0x66, 0, 1, PushOpExecutor.getInstance()),
  PUSH8(0x67, 0, 1, PushOpExecutor.getInstance()),
  PUSH9(0x68, 0, 1, PushOpExecutor.getInstance()),
  PUSH10(0x69, 0, 1, PushOpExecutor.getInstance()),
  PUSH11(0x6a, 0, 1, PushOpExecutor.getInstance()),
  PUSH12(0x6b, 0, 1, PushOpExecutor.getInstance()),
  PUSH13(0x6c, 0, 1, PushOpExecutor.getInstance()),
  PUSH14(0x6d, 0, 1, PushOpExecutor.getInstance()),
  PUSH15(0x6e, 0, 1, PushOpExecutor.getInstance()),
  PUSH16(0x6f, 0, 1, PushOpExecutor.getInstance()),
  PUSH17(0x70, 0, 1, PushOpExecutor.getInstance()),
  PUSH18(0x71, 0, 1, PushOpExecutor.getInstance()),
  PUSH19(0x72, 0, 1, PushOpExecutor.getInstance()),
  PUSH20(0x73, 0, 1, PushOpExecutor.getInstance()),
  PUSH21(0x74, 0, 1, PushOpExecutor.getInstance()),
  PUSH22(0x75, 0, 1, PushOpExecutor.getInstance()),
  PUSH23(0x76, 0, 1, PushOpExecutor.getInstance()),
  PUSH24(0x77, 0, 1, PushOpExecutor.getInstance()),
  PUSH25(0x78, 0, 1, PushOpExecutor.getInstance()),
  PUSH26(0x79, 0, 1, PushOpExecutor.getInstance()),
  PUSH27(0x7a, 0, 1, PushOpExecutor.getInstance()),
  PUSH28(0x7b, 0, 1, PushOpExecutor.getInstance()),
  PUSH29(0x7c, 0, 1, PushOpExecutor.getInstance()),
  PUSH30(0x7d, 0, 1, PushOpExecutor.getInstance()),
  PUSH31(0x7e, 0, 1, PushOpExecutor.getInstance()),
  PUSH32(0x7f, 0, 1, PushOpExecutor.getInstance()),

  DUP1(0x80, 1, 2, DupOpExecutor.getInstance()),
  DUP2(0x81, 2, 3, DupOpExecutor.getInstance()),
  DUP3(0x82, 3, 4, DupOpExecutor.getInstance()),
  DUP4(0x83, 4, 5, DupOpExecutor.getInstance()),
  DUP5(0x84, 5, 6, DupOpExecutor.getInstance()),
  DUP6(0x85, 6, 7, DupOpExecutor.getInstance()),
  DUP7(0x86, 7, 8, DupOpExecutor.getInstance()),
  DUP8(0x87, 8, 9, DupOpExecutor.getInstance()),
  DUP9(0x88, 9, 10, DupOpExecutor.getInstance()),
  DUP10(0x89, 10, 11, DupOpExecutor.getInstance()),
  DUP11(0x8a, 11, 12, DupOpExecutor.getInstance()),
  DUP12(0x8b, 12, 13, DupOpExecutor.getInstance()),
  DUP13(0x8c, 13, 14, DupOpExecutor.getInstance()),
  DUP14(0x8d, 14, 15, DupOpExecutor.getInstance()),
  DUP15(0x8e, 15, 16, DupOpExecutor.getInstance()),
  DUP16(0x8f, 16, 17, DupOpExecutor.getInstance()),

  SWAP1(0x90, 2, 2, SwapOpExecutor.getInstance()),
  SWAP2(0x91, 3, 3, SwapOpExecutor.getInstance()),
  SWAP3(0x92, 4, 4, SwapOpExecutor.getInstance()),
  SWAP4(0x93, 5, 5, SwapOpExecutor.getInstance()),
  SWAP5(0x94, 6, 6, SwapOpExecutor.getInstance()),
  SWAP6(0x95, 7, 7, SwapOpExecutor.getInstance()),
  SWAP7(0x96, 8, 8, SwapOpExecutor.getInstance()),
  SWAP8(0x97, 9, 9, SwapOpExecutor.getInstance()),
  SWAP9(0x98, 10, 10, SwapOpExecutor.getInstance()),
  SWAP10(0x99, 11, 11, SwapOpExecutor.getInstance()),
  SWAP11(0x9a, 12, 12, SwapOpExecutor.getInstance()),
  SWAP12(0x9b, 13, 13, SwapOpExecutor.getInstance()),
  SWAP13(0x9c, 14, 14, SwapOpExecutor.getInstance()),
  SWAP14(0x9d, 15, 15, SwapOpExecutor.getInstance()),
  SWAP15(0x9e, 16, 16, SwapOpExecutor.getInstance()),
  SWAP16(0x9f, 17, 17, SwapOpExecutor.getInstance()),

  LOG0(0xa0, 2, 0, LogOpExecutor.getInstance()),
  LOG1(0xa1, 3, 0, LogOpExecutor.getInstance()),
  LOG2(0xa2, 4, 0, LogOpExecutor.getInstance()),
  LOG3(0xa3, 5, 0, LogOpExecutor.getInstance()),
  LOG4(0xa4, 6, 0, LogOpExecutor.getInstance()),

  CALL(0xf1, 7, 1, CallOpExecutor.getInstance()),
  CALLCODE(0xf2, 7, 1, CallOpExecutor.getInstance()),
  CALLTOKEN(0xd0, 8, 1, CallOpExecutor.getInstance()),
  DELEGATECALL(0xf4, 6, 1, CallOpExecutor.getInstance()),
  STATICCALL(0xfa, 6, 1, CallOpExecutor.getInstance()),

  TOKENBALANCE(0xd1, 2, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Costs.BALANCE, op.name());
          DataWord tokenId = executor.stackPop();
          DataWord address = executor.stackPop();
          DataWord tokenBalance = executor.getTokenBalance(address, tokenId);
          executor.stackPush(tokenBalance);

          executor.step();
        }
      }
  ),
  CALLTOKENVALUE(0xd2, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Tier.BaseTier.asInt(), op.name());
          DataWord tokenValue = executor.getTokenValue();
          executor.stackPush(tokenValue);
          executor.step();

        }
      }
  ),
  CALLTOKENID(0xd3, 0, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Tier.BaseTier.asInt(), op.name());
          DataWord callTokenId = executor.getTokenId();
          executor.stackPush(callTokenId);
          executor.step();
        }
      }
  ),
  ISCONTRACT(0xd4, 1, 1,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          executor.spendEnergy(Costs.BALANCE, op.name());
          DataWord address = executor.stackPop();
          DataWord isContract = executor.isContract(address);

          executor.stackPush(isContract);
          executor.step();
        }
      }
  ),
  CREATE(0xf0, 3, 1, CreateOpExecutor.getInstance()),
  CREATE2(0xf5, 4, 1, CreateOpExecutor.getInstance()),
  RETURN(0xf3, 2, 0, RetOpExecutor.getInstance()),
  REVERT(0xfd, 2, 0, RetOpExecutor.getInstance()),
  SUICIDE(0xff, 1, 0,
      new OpExecutor() {
        @Override
        public void exec(Op op, ContractExecutor executor) {
          DataWord address = executor.stackPop();
          long energyCost = Costs.SUICIDE;
          if (executor.isDeadAccount(address)
              && !executor.getBalance(executor.getContractAddress()).isZero()) {
            energyCost += Costs.NEW_ACCT_SUICIDE;
          }

          if (executor.getContractContext().isStatic()) {
            throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
          }
          executor.suicide(address);
          executor.getContractContext().getProgramResult()
              .addTouchAccount(address.getLast20Bytes());

          executor.stop();
        }
      }
  )
  ;

  private final byte opcode;

    public int require() {
        return require;
    }

    public int ret() {
        return ret;
    }

    private final int require;
  private final int ret;

    public OpExecutor getOpExecutor() {
        return opExecutor;
    }

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

  public byte val() {
    return opcode;
  }

    private static final Op[] intToTypeMap = new Op[256];

    static {
        for (Op type : Op.values()) {
            intToTypeMap[type.opcode & 0xFF] = type;
        }
    }

    public static Op code(byte code) {
        return intToTypeMap[code & 0xFF];
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
