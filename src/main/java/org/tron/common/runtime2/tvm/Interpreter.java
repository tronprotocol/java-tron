package org.tron.common.runtime2.tvm;

import com.sun.javafx.font.directwrite.DWFactory;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.*;
import org.tron.common.runtime.vm.program.Stack;
import org.tron.common.runtime2.config.VMConfig;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.tron.common.crypto.Hash.sha3;
import static org.tron.common.runtime.vm.OpCode.*;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

@Slf4j(topic = "VM2")
public class Interpreter {
  private static final BigInteger _32_ = BigInteger.valueOf(32);
  public static final String ADDRESS_LOG = "address: ";
  private static final String ENERGY_LOG_FORMATE = "{} Op:[{}]  Energy:[{}] Deep:[{}] Hint:[{}]";


  private static class InterpreterInstance {
    private static final Interpreter instance = new Interpreter();
  }

  public static Interpreter getInstance() {
    return InterpreterInstance.instance;
  }


  public void play(Program program, ProgramEnv env) {
    if (isNotEmpty(program.getOps())){
      while (!env.isStopped()) {
        this.step(program, env);
      }
    }
  }


  public void step(Program program, ProgramEnv env) {
    try {
      OpCode op = OpCode.code(env.getCurrentOp());
      if (op == null) {
        throw org.tron.common.runtime.vm.program.Program.Exception.invalidOpCode(env.getCurrentOp());
      }
      env.setLastOp(op.val());
      env.verifyStackSize(op.require());
      //check not exceeding stack limits
      env.verifyStackOverflow(op.require(), op.ret());
      //spend energy
      DataWord callEnergy = spendEnergyAndGetCallEnergy(op, env);
      //checkcpu limit
      env.checkCPUTimeLimit(op.name());
      //step
      exec(env, op, callEnergy);
      env.setPreviouslyExecutedOp(op.val());
    } catch (RuntimeException e) {
      logger.info("VM halted: [{}]", e.getMessage());
      if (!(e instanceof org.tron.common.runtime.vm.program.Program.TransferException)) {
        env.spendAllEnergy();
      }
      env.resetFutureRefund();
      env.stop();
      throw e;
    } finally {
      env.fullTrace();
    }
  }

  private void exec(ProgramEnv env, OpCode op, DataWord adjustedCallEnergy) {
    EnergyCost energyCosts = EnergyCost.getInstance();
    Program program = env.getProgram();
    String hint = "";
    Stack stack = env.getStack();
    // Execute operation
    switch (op) {
      /**
       * Stop and Arithmetic Operations
       */
      case STOP: {
        program.getProgramResult().setHReturn(EMPTY_BYTE_ARRAY);
        env.stop();
      }
      break;
      case ADD: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " + " + word2.value();
        }

        word1.add(word2);
        env.stackPush(word1);
        env.step();

      }
      break;
      case MUL: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " * " + word2.value();
        }

        word1.mul(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case SUB: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " - " + word2.value();
        }

        word1.sub(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case DIV: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " / " + word2.value();
        }

        word1.div(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case SDIV: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.sValue() + " / " + word2.sValue();
        }

        word1.sDiv(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case MOD: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " % " + word2.value();
        }

        word1.mod(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case SMOD: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.sValue() + " #% " + word2.sValue();
        }

        word1.sMod(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case EXP: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " ** " + word2.value();
        }

        word1.exp(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case SIGNEXTEND: {
        DataWord word1 = env.stackPop();
        BigInteger k = word1.value();

        if (k.compareTo(_32_) < 0) {
          DataWord word2 = env.stackPop();
          if (logger.isDebugEnabled()) {
            hint = word1 + "  " + word2.value();
          }
          word2.signExtend(k.byteValue());
          env.stackPush(word2);
        }
        env.step();
      }
      break;
      case NOT: {
        DataWord word1 = env.stackPop();
        word1.bnot();

        if (logger.isDebugEnabled()) {
          hint = "" + word1.value();
        }

        env.stackPush(word1);
        env.step();
      }
      break;
      case LT: {
        // TODO: can be improved by not using BigInteger
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " < " + word2.value();
        }

        if (word1.value().compareTo(word2.value()) < 0) {
          word1.and(DataWord.ZERO);
          word1.getData()[31] = 1;
        } else {
          word1.and(DataWord.ZERO);
        }
        env.stackPush(word1);
        env.step();
      }
      break;
      case SLT: {
        // TODO: can be improved by not using BigInteger
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.sValue() + " < " + word2.sValue();
        }

        if (word1.sValue().compareTo(word2.sValue()) < 0) {
          word1.and(DataWord.ZERO);
          word1.getData()[31] = 1;
        } else {
          word1.and(DataWord.ZERO);
        }
        env.stackPush(word1);
        env.step();
      }
      break;
      case SGT: {
        // TODO: can be improved by not using BigInteger
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.sValue() + " > " + word2.sValue();
        }

        if (word1.sValue().compareTo(word2.sValue()) > 0) {
          word1.and(DataWord.ZERO);
          word1.getData()[31] = 1;
        } else {
          word1.and(DataWord.ZERO);
        }
        env.stackPush(word1);
        env.step();
      }
      break;
      case GT: {
        // TODO: can be improved by not using BigInteger
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " > " + word2.value();
        }

        if (word1.value().compareTo(word2.value()) > 0) {
          word1.and(DataWord.ZERO);
          word1.getData()[31] = 1;
        } else {
          word1.and(DataWord.ZERO);
        }
        env.stackPush(word1);
        env.step();
      }
      break;
      case EQ: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " == " + word2.value();
        }

        if (word1.xor(word2).isZero()) {
          word1.and(DataWord.ZERO);
          word1.getData()[31] = 1;
        } else {
          word1.and(DataWord.ZERO);
        }
        env.stackPush(word1);
        env.step();
      }
      break;
      case ISZERO: {
        DataWord word1 = env.stackPop();
        if (word1.isZero()) {
          word1.getData()[31] = 1;
        } else {
          word1.and(DataWord.ZERO);
        }

        if (logger.isDebugEnabled()) {
          hint = "" + word1.value();
        }

        env.stackPush(word1);
        env.step();
      }
      break;

      /**
       * Bitwise Logic Operations
       */
      case AND: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " && " + word2.value();
        }

        word1.and(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case OR: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " || " + word2.value();
        }

        word1.or(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case XOR: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = word1.value() + " ^ " + word2.value();
        }

        word1.xor(word2);
        env.stackPush(word1);
        env.step();
      }
      break;
      case BYTE: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();
        final DataWord result;
        if (word1.value().compareTo(_32_) < 0) {
          byte tmp = word2.getData()[word1.intValue()];
          word2.and(DataWord.ZERO);
          word2.getData()[31] = tmp;
          result = word2;
        } else {
          result = new DataWord();
        }

        if (logger.isDebugEnabled()) {
          hint = "" + result.value();
        }

        env.stackPush(result);
        env.step();
      }
      break;
      case SHL: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();
        final DataWord result = word2.shiftLeft(word1);

        if (logger.isInfoEnabled()) {
          hint = "" + result.value();
        }

        env.stackPush(result);
        env.step();
      }
      break;
      case SHR: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();
        final DataWord result = word2.shiftRight(word1);

        if (logger.isInfoEnabled()) {
          hint = "" + result.value();
        }

        env.stackPush(result);
        env.step();
      }
      break;
      case SAR: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();
        final DataWord result = word2.shiftRightSigned(word1);

        if (logger.isInfoEnabled()) {
          hint = "" + result.value();
        }

        env.stackPush(result);
        env.step();
      }
      break;
      case ADDMOD: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();
        DataWord word3 = env.stackPop();
        word1.addmod(word2, word3);
        env.stackPush(word1);
        env.step();
      }
      break;
      case MULMOD: {
        DataWord word1 = env.stackPop();
        DataWord word2 = env.stackPop();
        DataWord word3 = env.stackPop();
        word1.mulmod(word2, word3);
        env.stackPush(word1);
        env.step();
      }
      break;

      /**
       * SHA3
       */
      case SHA3: {
        DataWord memOffsetData = env.stackPop();
        DataWord lengthData = env.stackPop();
        byte[] buffer = env
                .memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());

        byte[] encoded = sha3(buffer);
        DataWord word = new DataWord(encoded);

        if (logger.isDebugEnabled()) {
          hint = word.toString();
        }

        env.stackPush(word);
        env.step();
      }
      break;

      /**
       * Environmental Information
       */
      case ADDRESS: {
        DataWord address = env.getContractAddress();
        address = new DataWord(address.getLast20Bytes());

        if (logger.isDebugEnabled()) {
          hint = ADDRESS_LOG + Hex.toHexString(address.getLast20Bytes());
        }

        env.stackPush(address);
        env.step();
      }
      break;
      case BALANCE: {
        DataWord address = env.stackPop();
        DataWord balance = env.getBalance(address);

        if (logger.isDebugEnabled()) {
          hint = ADDRESS_LOG
                  + Hex.toHexString(address.getLast20Bytes())
                  + " balance: " + balance.toString();
        }

        env.stackPush(balance);
        env.step();
      }
      break;
      case ISCONTRACT: {
        DataWord address = env.stackPop();
        DataWord isContract = env.isContract(address);

        env.stackPush(isContract);
        env.step();
      }
      break;
      case ORIGIN: {
        DataWord originAddress = env.getOriginAddress();


        originAddress = new DataWord(originAddress.getLast20Bytes());


        if (logger.isDebugEnabled()) {
          hint = ADDRESS_LOG + Hex.toHexString(originAddress.getLast20Bytes());
        }

        env.stackPush(originAddress);
        env.step();
      }
      break;
      case CALLER: {
        DataWord callerAddress = env.getCallerAddress();
        /**
         since we use 21 bytes address instead of 20 as etherum, we need to make sure
         the address length in vm is matching with 20
         */
        callerAddress = new DataWord(callerAddress.getLast20Bytes());
        if (logger.isDebugEnabled()) {
          hint = ADDRESS_LOG + Hex.toHexString(callerAddress.getLast20Bytes());
        }

        env.stackPush(callerAddress);
        env.step();
      }
      break;
      case CALLVALUE: {
        DataWord callValue = env.getCallValue();

        if (logger.isDebugEnabled()) {
          hint = "value: " + callValue;
        }

        env.stackPush(callValue);
        env.step();
      }
      break;
      case CALLTOKENVALUE:
        DataWord tokenValue = env.getTokenValue();

        if (logger.isDebugEnabled()) {
          hint = "tokenValue: " + tokenValue;
        }

        env.stackPush(tokenValue);
        env.step();
        break;
      case CALLTOKENID:
        DataWord _tokenId = env.getTokenId();

        if (logger.isDebugEnabled()) {
          hint = "tokenId: " + _tokenId;
        }

        env.stackPush(_tokenId);
        env.step();
        break;
      case CALLDATALOAD: {
        DataWord dataOffs = env.stackPop();
        DataWord value = env.getDataValue(dataOffs);

        if (logger.isDebugEnabled()) {
          hint = "data: " + value;
        }

        env.stackPush(value);
        env.step();
      }
      break;
      case CALLDATASIZE: {
        DataWord dataSize = env.getDataSize();

        if (logger.isDebugEnabled()) {
          hint = "size: " + dataSize.value();
        }

        env.stackPush(dataSize);
        env.step();
      }
      break;
      case CALLDATACOPY: {
        DataWord memOffsetData = env.stackPop();
        DataWord dataOffsetData = env.stackPop();
        DataWord lengthData = env.stackPop();

        byte[] msgData = env.getDataCopy(dataOffsetData, lengthData);

        if (logger.isDebugEnabled()) {
          hint = "data: " + Hex.toHexString(msgData);
        }

        env.memorySave(memOffsetData.intValueSafe(), msgData);
        env.step();
      }
      break;
      case RETURNDATASIZE: {
        DataWord dataSize = env.getReturnDataBufferSize();

        if (logger.isDebugEnabled()) {
          hint = "size: " + dataSize.value();
        }

        env.stackPush(dataSize);
        env.step();
      }
      break;
      case RETURNDATACOPY: {
        DataWord memOffsetData = env.stackPop();
        DataWord dataOffsetData = env.stackPop();
        DataWord lengthData = env.stackPop();

        byte[] msgData = env.getReturnDataBufferData(dataOffsetData, lengthData);

        if (msgData == null) {
          throw new org.tron.common.runtime.vm.program.Program.ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData,
                  env.getReturnDataBufferSize().longValueSafe());
        }

        if (logger.isDebugEnabled()) {
          hint = "data: " + Hex.toHexString(msgData);
        }

        env.memorySave(memOffsetData.intValueSafe(), msgData);
        env.step();
      }
      break;
      case CODESIZE:
      case EXTCODESIZE: {

        int length;
        if (op == OpCode.CODESIZE) {
          length = program.getCode().length;
        } else {
          DataWord address = env.stackPop();
          length = env.getCodeAt(address).length;
        }
        DataWord codeLength = new DataWord(length);

        if (logger.isDebugEnabled()) {
          hint = "size: " + length;
        }

        env.stackPush(codeLength);
        env.step();
        break;
      }
      case CODECOPY:
      case EXTCODECOPY: {

        byte[] fullCode = EMPTY_BYTE_ARRAY;
        if (op == OpCode.CODECOPY) {
          fullCode = program.getCode();
        }

        if (op == OpCode.EXTCODECOPY) {
          DataWord address = env.stackPop();
          fullCode = env.getCodeAt(address);
        }

        int memOffset = env.stackPop().intValueSafe();
        int codeOffset = env.stackPop().intValueSafe();
        int lengthData = env.stackPop().intValueSafe();

        int sizeToBeCopied =
                (long) codeOffset + lengthData > fullCode.length
                        ? (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset)
                        : lengthData;

        byte[] codeCopy = new byte[lengthData];

        if (codeOffset < fullCode.length) {
          System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);
        }

        if (logger.isDebugEnabled()) {
          hint = "code: " + Hex.toHexString(codeCopy);
        }

        env.memorySave(memOffset, codeCopy);
        env.step();
        break;
      }
      case EXTCODEHASH: {
        DataWord address = env.stackPop();
        byte[] codeHash = env.getCodeHashAt(address);
        env.stackPush(codeHash);
        env.step();
      }
      break;
      case GASPRICE: {
        DataWord energyPrice = new DataWord(0);

        if (logger.isDebugEnabled()) {
          hint = "price: " + energyPrice.toString();
        }

        env.stackPush(energyPrice);
        env.step();
      }
      break;

      /**
       * Block Information
       */
      case BLOCKHASH: {

        int blockIndex = env.stackPop().intValueSafe();

        DataWord blockHash = env.getBlockHash(blockIndex);

        if (logger.isDebugEnabled()) {
          hint = "blockHash: " + blockHash;
        }

        env.stackPush(blockHash);
        env.step();
      }
      break;
      case COINBASE: {
        DataWord coinbase = env.getBlockInfo().getCoinbase();

        if (logger.isDebugEnabled()) {
          hint = "coinbase: " + Hex.toHexString(coinbase.getLast20Bytes());
        }

        env.stackPush(coinbase);
        env.step();
      }
      break;
      case TIMESTAMP: {
        DataWord timestamp = env.getBlockInfo().getTimestamp();

        if (logger.isDebugEnabled()) {
          hint = "timestamp: " + timestamp.value();
        }

        env.stackPush(timestamp);
        env.step();
      }
      break;
      case NUMBER: {
        DataWord number = env.getBlockInfo().getNumber();

        if (logger.isDebugEnabled()) {
          hint = "number: " + number.value();
        }

        env.stackPush(number);
        env.step();
      }
      break;
      case DIFFICULTY: {
        DataWord difficulty = env.getBlockInfo().getDifficulty();

        if (logger.isDebugEnabled()) {
          hint = "difficulty: " + difficulty;
        }

        env.stackPush(difficulty);
        env.step();
      }
      break;
      case GASLIMIT: {
        // todo: this energylimit is the block's energy limit
        DataWord energyLimit = new DataWord(0);

        if (logger.isDebugEnabled()) {
          hint = "energylimit: " + energyLimit;
        }

        env.stackPush(energyLimit);
        env.step();
      }
      break;
      case POP: {
        env.stackPop();
        env.step();
      }
      break;
      case DUP1:
      case DUP2:
      case DUP3:
      case DUP4:
      case DUP5:
      case DUP6:
      case DUP7:
      case DUP8:
      case DUP9:
      case DUP10:
      case DUP11:
      case DUP12:
      case DUP13:
      case DUP14:
      case DUP15:
      case DUP16: {

        int n = op.val() - OpCode.DUP1.val() + 1;
        DataWord word_1 = stack.get(stack.size() - n);
        env.stackPush(word_1.clone());
        env.step();

        break;
      }
      case SWAP1:
      case SWAP2:
      case SWAP3:
      case SWAP4:
      case SWAP5:
      case SWAP6:
      case SWAP7:
      case SWAP8:
      case SWAP9:
      case SWAP10:
      case SWAP11:
      case SWAP12:
      case SWAP13:
      case SWAP14:
      case SWAP15:
      case SWAP16: {

        int n = op.val() - OpCode.SWAP1.val() + 2;
        stack.swap(stack.size() - 1, stack.size() - n);
        env.step();
        break;
      }
      case LOG0:
      case LOG1:
      case LOG2:
      case LOG3:
      case LOG4: {

        if (program.isStatic()) {
          throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
        }
        DataWord address = env.getContractAddress();

        DataWord memStart = stack.pop();
        DataWord memOffset = stack.pop();

        int nTopics = op.val() - OpCode.LOG0.val();

        List<DataWord> topics = new ArrayList<>();
        for (int i = 0; i < nTopics; ++i) {
          DataWord topic = stack.pop();
          topics.add(topic);
        }

        byte[] data = env.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());

        LogInfo logInfo =
                new LogInfo(address.getLast20Bytes(), topics, data);

        if (logger.isDebugEnabled()) {
          hint = logInfo.toString();
        }

        program.getProgramResult().addLogInfo(logInfo);
        env.step();
        break;
      }
      case MLOAD: {
        DataWord addr = env.stackPop();
        DataWord data = env.memoryLoad(addr);

        if (logger.isDebugEnabled()) {
          hint = "data: " + data;
        }

        env.stackPush(data);
        env.step();
      }
      break;
      case MSTORE: {
        DataWord addr = env.stackPop();
        DataWord value = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = "addr: " + addr + " value: " + value;
        }

        env.memorySave(addr, value);
        env.step();
      }
      break;
      case MSTORE8: {
        DataWord addr = env.stackPop();
        DataWord value = env.stackPop();
        byte[] byteVal = {value.getData()[31]};
        env.memorySave(addr.intValueSafe(), byteVal);
        env.step();
      }
      break;
      case SLOAD: {
        DataWord key = env.stackPop();
        DataWord val = env.storageLoad(key);

        if (logger.isDebugEnabled()) {
          hint = "key: " + key + " value: " + val;
        }

        if (val == null) {
          val = key.and(DataWord.ZERO);
        }

        env.stackPush(val);
        env.step();
      }
      break;
      case SSTORE: {
        if (program.isStatic()) {
          throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
        }

        DataWord addr = env.stackPop();
        DataWord value = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint =
                  "[" + env.getContractAddress().toPrefixString() + "] key: " + addr + " value: "
                          + value;
        }

        env.storageSave(addr, value);
        env.step();
      }
      break;
      case JUMP: {
        DataWord pos = env.stackPop();
        int nextPC = env.verifyJumpDest(pos);

        if (logger.isDebugEnabled()) {
          hint = "~> " + nextPC;
        }

        env.setPC(nextPC);

      }
      break;
      case JUMPI: {
        DataWord pos = env.stackPop();
        DataWord cond = env.stackPop();

        if (!cond.isZero()) {
          int nextPC = env.verifyJumpDest(pos);

          if (logger.isDebugEnabled()) {
            hint = "~> " + nextPC;
          }

          env.setPC(nextPC);
        } else {
          env.step();
        }

      }
      break;
      case PC: {
        int pc = env.getPC();
        DataWord pcWord = new DataWord(pc);

        if (logger.isDebugEnabled()) {
          hint = pcWord.toString();
        }

        env.stackPush(pcWord);
        env.step();
      }
      break;
      case MSIZE: {
        int memSize = env.getMemSize();
        DataWord wordMemSize = new DataWord(memSize);

        if (logger.isDebugEnabled()) {
          hint = "" + memSize;
        }

        env.stackPush(wordMemSize);
        env.step();
      }
      break;
      case GAS: {
        DataWord energy = program.getEnergyLimitLeft();
        if (logger.isDebugEnabled()) {
          hint = "" + energy;
        }

        env.stackPush(energy);
        env.step();
      }
      break;

      case PUSH1:
      case PUSH2:
      case PUSH3:
      case PUSH4:
      case PUSH5:
      case PUSH6:
      case PUSH7:
      case PUSH8:
      case PUSH9:
      case PUSH10:
      case PUSH11:
      case PUSH12:
      case PUSH13:
      case PUSH14:
      case PUSH15:
      case PUSH16:
      case PUSH17:
      case PUSH18:
      case PUSH19:
      case PUSH20:
      case PUSH21:
      case PUSH22:
      case PUSH23:
      case PUSH24:
      case PUSH25:
      case PUSH26:
      case PUSH27:
      case PUSH28:
      case PUSH29:
      case PUSH30:
      case PUSH31:
      case PUSH32: {
        env.step();
        int nPush = op.val() - PUSH1.val() + 1;

        byte[] data = env.sweep(nPush);

        if (logger.isDebugEnabled()) {
          hint = "" + Hex.toHexString(data);
        }

        env.stackPush(data);
        break;
      }
      case JUMPDEST: {
        env.step();
      }
      break;
      case CREATE: {
        if (program.isStatic()) {
          throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
        }
        DataWord value = env.stackPop();
        DataWord inOffset = env.stackPop();
        DataWord inSize = env.stackPop();
        env.createContract(value, inOffset, inSize);

        env.step();
      }
      break;
      case CREATE2: {
        if (program.isStatic()) {
          throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
        }
        DataWord value = env.stackPop();
        DataWord inOffset = env.stackPop();
        DataWord inSize = env.stackPop();
        DataWord salt = env.stackPop();
        env.createContract(value, inOffset, inSize, salt, true);
        env.step();
      }
      break;
      case TOKENBALANCE: {
        DataWord tokenId = env.stackPop();
        DataWord address = env.stackPop();
        DataWord tokenBalance = env.getTokenBalance(address, tokenId);
        env.stackPush(tokenBalance);

        env.step();
      }
      break;
      case CALL:
      case CALLCODE:
      case CALLTOKEN:
      case DELEGATECALL:
      case STATICCALL: {
        env.stackPop(); // use adjustedCallEnergy instead of requested
        DataWord codeAddress = env.stackPop();

        DataWord value;
        if (op.callHasValue()) {
          value = env.stackPop();
        } else {
          value = DataWord.ZERO.clone();
        }

        if (program.isStatic() && (op == CALL || op == CALLTOKEN) && !value.isZero()) {
          throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
        }

        if (!value.isZero()) {
          adjustedCallEnergy.add(new DataWord(energyCosts.getSTIPEND_CALL()));
        }

        DataWord tokenId = new DataWord(0);
        boolean isTokenTransferMsg = false;
        if (op == CALLTOKEN) {
          tokenId = env.stackPop();
          isTokenTransferMsg = true;
        }

        DataWord inDataOffs = env.stackPop();
        DataWord inDataSize = env.stackPop();

        DataWord outDataOffs = env.stackPop();
        DataWord outDataSize = env.stackPop();

        if (logger.isDebugEnabled()) {
          hint = "addr: " + Hex.toHexString(codeAddress.getLast20Bytes())
                  + " energy: " + adjustedCallEnergy.shortHex()
                  + " inOff: " + inDataOffs.shortHex()
                  + " inSize: " + inDataSize.shortHex();
          logger.debug(ENERGY_LOG_FORMATE, String.format("%5s", "[" + env.getPC() + "]"),
                  String.format("%-12s", op.name()),
                  program.getEnergyLimitLeft().value(),
                  env.getCallDeep(), hint);
        }

        env.memoryExpand(outDataOffs, outDataSize);

        MessageCall msg = new MessageCall(
                op, adjustedCallEnergy, codeAddress, value, inDataOffs, inDataSize,
                outDataOffs, outDataSize, tokenId, isTokenTransferMsg);

        PrecompiledContracts.PrecompiledContract contract =
                PrecompiledContracts.getContractForAddress(codeAddress);

        if (!op.callIsStateless()) {
          program.getProgramResult().addTouchAccount(codeAddress.getLast20Bytes());
        }

        if (contract != null) {
          env.callToPrecompiledAddress(msg, contract);
        } else {
          env.callToAddress(msg);
        }

        env.step();
        break;
      }
      case RETURN:
      case REVERT: {
        DataWord offset = env.stackPop();
        DataWord size = env.stackPop();

        byte[] hReturn = env.memoryChunk(offset.intValueSafe(), size.intValueSafe());
        program.getProgramResult().setHReturn(hReturn);

        if (logger.isDebugEnabled()) {
          hint = "data: " + Hex.toHexString(hReturn)
                  + " offset: " + offset.value()
                  + " size: " + size.value();
        }

        env.step();
        env.stop();

        if (op == REVERT) {
          program.getProgramResult().setRevert();
        }
        break;
      }
      case SUICIDE: {
        if (program.isStatic()) {
          throw new org.tron.common.runtime.vm.program.Program.StaticCallModificationException();
        }

        DataWord address = env.stackPop();
        env.suicide(address);
        program.getProgramResult().addTouchAccount(address.getLast20Bytes());

        if (logger.isDebugEnabled()) {
          hint = ADDRESS_LOG + Hex.toHexString(env.getContractAddress().getLast20Bytes());
        }

        env.stop();
      }
      break;
      default:
        break;
    }
  }


  private long calcMemEnergy(EnergyCost energyCosts, long oldMemSize, BigInteger newMemSize,
                             long copySize, OpCode op) {
    long energyCost = 0;

    checkMemorySize(op, newMemSize);

    // memory drop consume calc
    long memoryUsage = (newMemSize.longValueExact() + 31) / 32 * 32;
    if (memoryUsage > oldMemSize) {
      long memWords = (memoryUsage / 32);
      long memWordsOld = (oldMemSize / 32);
      //TODO #POC9 c_quadCoeffDiv = 512, this should be a constant, not magic number
      long memEnergy = (energyCosts.getMEMORY() * memWords + memWords * memWords / 512)
              - (energyCosts.getMEMORY() * memWordsOld + memWordsOld * memWordsOld / 512);
      energyCost += memEnergy;
    }

    if (copySize > 0) {
      long copyEnergy = energyCosts.getCOPY_ENERGY() * ((copySize + 31) / 32);
      energyCost += copyEnergy;
    }
    return energyCost;
  }

  /**
   * Utility to calculate new total memory size needed for an operation. <br/> Basically just offset
   * + size, unless size is 0, in which case the result is also 0.
   *
   * @param offset starting position of the memory
   * @param size   number of bytes needed
   * @return offset + size, unless size is 0. In that case memNeeded is also 0.
   */
  private static BigInteger memNeeded(DataWord offset, DataWord size) {
    return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
  }

  private void checkMemorySize(OpCode op, BigInteger newMemSize) {
    if (newMemSize.compareTo(VMConfig.MEM_LIMIT) > 0) {
      throw org.tron.common.runtime.vm.program.Program.Exception.memoryOverflow(op);
    }
  }


  private DataWord spendEnergyAndGetCallEnergy(OpCode op, ProgramEnv env) {
    DataWord adjustedCallEnergy = null;
    long oldMemSize = env.getMemory().size();
    long energyCost = op.getTier().asInt();
    EnergyCost energyCosts = EnergyCost.getInstance();
    Stack stack = env.getStack();
    Program program = env.getProgram();

    // Calculate fees and spend energy
    switch (op) {
      case STOP:
        energyCost = energyCosts.getSTOP();
        break;
      case SUICIDE:
        energyCost = energyCosts.getSUICIDE();
        DataWord suicideAddressWord = stack.get(stack.size() - 1);
        if (env.isDeadAccount(suicideAddressWord)
                && !env.getBalance(env.getContractAddress()).isZero()) {
          energyCost += energyCosts.getNEW_ACCT_SUICIDE();
        }
        break;
      case SSTORE:
        // todo: check the reset to 0, refund or not
        DataWord newValue = stack.get(stack.size() - 2);
        DataWord oldValue = env.storageLoad(stack.peek());
        if (oldValue == null && !newValue.isZero()) {
          // set a new not-zero value
          energyCost = energyCosts.getSET_SSTORE();
        } else if (oldValue != null && newValue.isZero()) {
          // set zero to an old value
          env.futureRefundEnergy(energyCosts.getREFUND_SSTORE());
          energyCost = energyCosts.getCLEAR_SSTORE();
        } else {
          // include:
          // [1] oldValue == null && newValue == 0
          // [2] oldValue != null && newValue != 0
          energyCost = energyCosts.getRESET_SSTORE();
        }
        break;
      case SLOAD:
        energyCost = energyCosts.getSLOAD();
        break;
      case TOKENBALANCE:
      case BALANCE:
      case ISCONTRACT:
        energyCost = energyCosts.getBALANCE();
        break;

      // These all operate on memory and therefore potentially expand it:
      case MSTORE:
        energyCost = calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), new DataWord(32)),
                0, op);
        break;
      case MSTORE8:
        energyCost = calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), new DataWord(1)),
                0, op);
        break;
      case MLOAD:
        energyCost = calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), new DataWord(32)),
                0, op);
        break;
      case RETURN:
      case REVERT:
        energyCost = energyCosts.getSTOP() + calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, op);
        break;
      case SHA3:
        energyCost = energyCosts.getSHA3() + calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, op);
        DataWord size = stack.get(stack.size() - 2);
        long chunkUsed = (size.longValueSafe() + 31) / 32;
        energyCost += chunkUsed * energyCosts.getSHA3_WORD();
        break;
      case CALLDATACOPY:
      case RETURNDATACOPY:
        energyCost = calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), stack.get(stack.size() - 3)),
                stack.get(stack.size() - 3).longValueSafe(), op);
        break;
      case CODECOPY:
        energyCost = calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), stack.get(stack.size() - 3)),
                stack.get(stack.size() - 3).longValueSafe(), op);
        break;
      case EXTCODESIZE:
        energyCost = energyCosts.getEXT_CODE_SIZE();
        break;
      case EXTCODECOPY:
        energyCost = energyCosts.getEXT_CODE_COPY() + calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 4)),
                stack.get(stack.size() - 4).longValueSafe(), op);
        break;
      case EXTCODEHASH:
        energyCost = energyCosts.getEXT_CODE_HASH();
        break;
      case CALL:
      case CALLCODE:
      case DELEGATECALL:
      case STATICCALL:
      case CALLTOKEN:
        // here, contract call an other contract, or a library, and so on
        energyCost = energyCosts.getCALL();
        DataWord callEnergyWord = stack.get(stack.size() - 1);
        DataWord callAddressWord = stack.get(stack.size() - 2);
        DataWord value = op.callHasValue() ? stack.get(stack.size() - 3) : DataWord.ZERO.clone();

        //check to see if account does not exist and is not a precompiled contract
        if (op == CALL || op == CALLTOKEN) {
          if (env.isDeadAccount(callAddressWord) && !value.isZero()) {
            energyCost += energyCosts.getNEW_ACCT_CALL();
          }
        }

        // TODO #POC9 Make sure this is converted to BigInteger (256num support)
        if (!value.isZero()) {
          energyCost += energyCosts.getVT_CALL();
        }

        int opOff = op.callHasValue() ? 4 : 3;
        if (op == CALLTOKEN) {
          opOff++;
        }
        BigInteger in = memNeeded(stack.get(stack.size() - opOff),
                stack.get(stack.size() - opOff - 1)); // in offset+size
        BigInteger out = memNeeded(stack.get(stack.size() - opOff - 2),
                stack.get(stack.size() - opOff - 3)); // out offset+size
        energyCost += calcMemEnergy(energyCosts, oldMemSize, in.max(out), 0, op);
        checkMemorySize(op, in.max(out));

        if (energyCost > program.getEnergyLimitLeft().longValueSafe()) {
          throw new org.tron.common.runtime.vm.program.Program.OutOfEnergyException(
                  "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
                  op.name(),
                  energyCost, program.getEnergyLimitLeft().longValueSafe());
        }
        DataWord getEnergyLimitLeft = program.getEnergyLimitLeft().clone();
        getEnergyLimitLeft.sub(new DataWord(energyCost));

        adjustedCallEnergy = env.getCallEnergy(op, callEnergyWord, getEnergyLimitLeft);
        energyCost += adjustedCallEnergy.longValueSafe();
        break;
      case CREATE:
        energyCost = energyCosts.getCREATE() + calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)), 0, op);
        break;
      case CREATE2:
        DataWord codeSize = stack.get(stack.size() - 3);
        energyCost = energyCosts.getCREATE();
        energyCost += calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)), 0, op);
        energyCost += DataWord.sizeInWords(codeSize.intValueSafe()) * energyCosts.getSHA3_WORD();

        break;
      case LOG0:
      case LOG1:
      case LOG2:
      case LOG3:
      case LOG4:
        int nTopics = op.val() - OpCode.LOG0.val();
        BigInteger dataSize = stack.get(stack.size() - 2).value();
        BigInteger dataCost = dataSize
                .multiply(BigInteger.valueOf(energyCosts.getLOG_DATA_ENERGY()));
        if (program.getEnergyLimitLeft().value().compareTo(dataCost) < 0) {
          throw new org.tron.common.runtime.vm.program.Program.OutOfEnergyException(
                  "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
                  op.name(),
                  dataCost.longValueExact(), program.getEnergyLimitLeft().longValueSafe());
        }
        energyCost = energyCosts.getLOG_ENERGY()
                + energyCosts.getLOG_TOPIC_ENERGY() * nTopics
                + energyCosts.getLOG_DATA_ENERGY() * stack.get(stack.size() - 2).longValue()
                + calcMemEnergy(energyCosts, oldMemSize,
                memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, op);

        checkMemorySize(op, memNeeded(stack.peek(), stack.get(stack.size() - 2)));
        break;
      case EXP:

        DataWord exp = stack.get(stack.size() - 2);
        int bytesOccupied = exp.bytesOccupied();
        energyCost =
                (long) energyCosts.getEXP_ENERGY() + energyCosts.getEXP_BYTE_ENERGY() * bytesOccupied;
        break;
      default:
        break;
    }

    env.spendEnergy(energyCost, op.name());
    return adjustedCallEnergy;
  }


}
