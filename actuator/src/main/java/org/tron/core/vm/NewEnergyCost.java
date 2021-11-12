package org.tron.core.vm;

import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Stack;

import java.math.BigInteger;

import static org.tron.core.db.TransactionTrace.convertToTronAddress;

public class NewEnergyCost {

  private static final long ZERO_TIER = 0;
  private static final long BASE_TIER = 2;
  private static final long VERY_LOW_TIER = 3;
  private static final long LOW_TIER = 5;
  private static final long MID_TIER = 8;
  private static final long HIGH_TIER = 10;
  private static final long SPECIAL_TIER = 1;

  private static final long EXP_ENERGY = 10;
  private static final long EXP_BYTE_ENERGY = 10;
  private static final int SHA3 = 30;
  // 3MB
  private static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);
  private static final int MEMORY = 3;
  private static final int COPY_ENERGY = 3;
  private static final int SHA3_WORD = 6;
  private static final int SLOAD = 50;
  private static final int CLEAR_SSTORE = 5000;
  private static final int SET_SSTORE = 20000;
  private static final int RESET_SSTORE = 5000;
  private static final int REFUND_SSTORE = 15000;
  private static final int LOG_DATA_ENERGY = 8;
  private static final int LOG_ENERGY = 375;
  private static final int LOG_TOPIC_ENERGY = 375;
  private static final int BALANCE = 20;
  private static final int FREEZE = 20000;
  private static final int NEW_ACCT_CALL = 25000;
  private static final int UNFREEZE = 20000;
  private static final int FREEZE_EXPIRE_TIME = 50;
  private static final int VOTE_WITNESS = 30000;
  private static final int WITHDRAW_REWARD = 20000;
  private static final int CREATE = 32000;
  private static final int CALL_ENERGY = 40;
  private static final int VT_CALL = 9000;
  private static final int STIPEND_CALL = 2300;

  // call series opcode
  private static final int CALLTOKEN = 0xd0;
  private static final int CALL = 0xf1;
  private static final int CALLCODE = 0xf2;

  public static long getZeroTierCost(Program program) {
    return ZERO_TIER;
  }

  public static long getVeryLowTierCost(Program program) {
    return VERY_LOW_TIER;
  }

  public static long getLowTierCost(Program program) {
    return LOW_TIER;
  }

  public static long getMidTierCost(Program program) {
    return MID_TIER;
  }

  public static long getBaseTierCost(Program program) {
    return BASE_TIER;
  }

  public static long getExtTierCost(Program program) {
    return EXP_ENERGY;
  }

  public static long getHighTierrCost(Program program) {
    return HIGH_TIER;
  }

  public static long getSpecialTierCost(Program program) {
    return SPECIAL_TIER;
  }

  public static long getStipendCallCost() {
    return STIPEND_CALL;
  }

  public static long getExpCost(Program program) {
    Stack stack = program.getStack();
    DataWord exp = stack.get(stack.size() - 2);
    int bytesOccupied = exp.bytesOccupied();
    return EXP_ENERGY  + EXP_BYTE_ENERGY * bytesOccupied;
  }

  public static long getSha3Cost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = SHA3 + calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, "SHA3");
    DataWord size = stack.get(stack.size() - 2);
    long chunkUsed = (size.longValueSafe() + 31) / 32;
    energyCost += chunkUsed * SHA3_WORD;
    return energyCost;
  }

  public static long getSloadCost(Program program) {
    return SLOAD;
  }

  public static long getSstoreCost(Program program) {
    Stack stack = program.getStack();
    DataWord newValue = stack.get(stack.size() - 2);
    DataWord oldValue = program.storageLoad(stack.peek());
    long energyCost;
    if (oldValue == null && !newValue.isZero()) {
      // set a new not-zero value
      energyCost = SET_SSTORE;
    } else if (oldValue != null && newValue.isZero()) {
      // set zero to an old value
      program.futureRefundEnergy(REFUND_SSTORE);
      energyCost = CLEAR_SSTORE;
    } else {
      // include:
      // [1] oldValue == null && newValue == 0
      // [2] oldValue != null && newValue != 0
      energyCost = RESET_SSTORE;
    }
    return energyCost;
  }

  public static long getLogCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    int nTopics = program.getCurrentOp() - 0xa0;
    BigInteger dataSize = stack.get(stack.size() - 2).value();
    BigInteger dataCost = dataSize
        .multiply(BigInteger.valueOf(LOG_DATA_ENERGY));
    if (program.getEnergyLimitLeft().value().compareTo(dataCost) < 0) {
      throw new Program.OutOfEnergyException(
          "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
          "LOG" + nTopics,
          dataCost.longValueExact(), program.getEnergyLimitLeft().longValueSafe());
    }
    long energyCost = LOG_ENERGY
        + LOG_TOPIC_ENERGY * nTopics
        + LOG_DATA_ENERGY * stack.get(stack.size() - 2).longValue()
        + calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, "LOG" + nTopics);

    checkMemorySize("LOG" + nTopics, memNeeded(stack.peek(), stack.get(stack.size() - 2)));
    return energyCost;
  }

  public static long getBalanceCost(Program program) {
    return BALANCE;
  }

  public static long getFreezeCost(Program program) {
    long energyCost = FREEZE;
    Stack stack = program.getStack();
    DataWord receiverAddressWord = stack.get(stack.size() - 3);
    if (isDeadAccount(program, receiverAddressWord)) {
      energyCost += NEW_ACCT_CALL;
    }
    return energyCost;
  }

  public static long getUnfreezeCost(Program program) {
    return UNFREEZE;
  }

  public static long getFreezeExpireTimeCost(Program program) {
    return FREEZE_EXPIRE_TIME;
  }

  public static long getVoteWitnessCost(Program program) {
    long energyCost = VOTE_WITNESS;
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    DataWord amountArrayLength = stack.get(stack.size() - 1).clone();
    DataWord amountArrayOffset = stack.get(stack.size() - 2);
    DataWord witnessArrayLength = stack.get(stack.size() - 3).clone();
    DataWord witnessArrayOffset = stack.get(stack.size() - 4);

    DataWord wordSize = new DataWord(DataWord.WORD_SIZE);

    amountArrayLength.mul(wordSize);
    BigInteger amountArrayMemoryNeeded = memNeeded(amountArrayOffset, amountArrayLength);

    witnessArrayLength.mul(wordSize);
    BigInteger witnessArrayMemoryNeeded = memNeeded(witnessArrayOffset, witnessArrayLength);

    energyCost += calcMemEnergy(oldMemSize,
        (amountArrayMemoryNeeded.compareTo(witnessArrayMemoryNeeded) > 0
            ? amountArrayMemoryNeeded : witnessArrayMemoryNeeded),
        0, "VOTEWITNESS");
    return energyCost;
  }

  public static long getWithdrawRewardCost(Program program) {
    return WITHDRAW_REWARD;
  }

  public static long getCreateCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    return CREATE + calcMemEnergy(oldMemSize,
        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)),
        0, "create");
  }

  public static long getCreate2Cost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    DataWord codeSize = stack.get(stack.size() - 3);
    long energyCost = CREATE;
    energyCost += calcMemEnergy(oldMemSize,
        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)),
        0, "create2");
    energyCost += DataWord.sizeInWords(codeSize.intValueSafe()) * SHA3_WORD;
    return energyCost;
  }

  public static long getCallCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    // here, contract call an other contract, or a library, and so on
    long energyCost = CALL_ENERGY;
    DataWord callEnergyWord = stack.get(stack.size() - 1);
    DataWord callAddressWord = stack.get(stack.size() - 2);
    byte op = program.getCurrentOp();
    DataWord value = DataWord.ZERO;
    int opOff = 3;
    if (op == (byte) CALL || op == (byte) CALLTOKEN || op == (byte) CALLCODE) {
      value = stack.get(stack.size() - 3);
      opOff = 4;
    }
    //check to see if account does not exist and is not a precompiled contract
    if ((op == (byte) CALL || op == (byte) CALLTOKEN)
        && isDeadAccount(program, callAddressWord)
        && !value.isZero()) {
      energyCost += NEW_ACCT_CALL;
    }

    // TODO #POC9 Make sure this is converted to BigInteger (256num support)
    if (!value.isZero()) {
      energyCost += VT_CALL;
    }
    if (op == (byte) CALLTOKEN) {
      opOff++;
    }
    BigInteger in = memNeeded(stack.get(stack.size() - opOff),
        stack.get(stack.size() - opOff - 1)); // in offset+size
    BigInteger out = memNeeded(stack.get(stack.size() - opOff - 2),
        stack.get(stack.size() - opOff - 3)); // out offset+size
    energyCost += calcMemEnergy(oldMemSize, in.max(out),
        0, Op.getOpName(op & 0xff));
    checkMemorySize(Op.getOpName(op & 0xff), in.max(out));

    if (energyCost > program.getEnergyLimitLeft().longValueSafe()) {
      throw new Program.OutOfEnergyException(
          "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
          Op.getOpName(op & 0xff),
          energyCost, program.getEnergyLimitLeft().longValueSafe());
    }
    DataWord getEnergyLimitLeft = program.getEnergyLimitLeft().clone();
    getEnergyLimitLeft.sub(new DataWord(energyCost));

    DataWord adjustedCallEnergy = program.getCallEnergy(callEnergyWord, getEnergyLimitLeft);
    energyCost += adjustedCallEnergy.longValueSafe();
    return energyCost;
  }


  private static long calcMemEnergy(long oldMemSize, BigInteger newMemSize,
                             long copySize, String opName) {
    long energyCost = 0;

    checkMemorySize(opName, newMemSize);

    // memory SUN consume calc
    long memoryUsage = (newMemSize.longValueExact() + 31) / 32 * 32;
    if (memoryUsage > oldMemSize) {
      long memWords = (memoryUsage / 32);
      long memWordsOld = (oldMemSize / 32);
      long memEnergy = (MEMORY * memWords + memWords * memWords / 512)
          - (MEMORY * memWordsOld + memWordsOld * memWordsOld / 512);
      energyCost += memEnergy;
    }

    if (copySize > 0) {
      long copyEnergy = COPY_ENERGY * ((copySize + 31) / 32);
      energyCost += copyEnergy;
    }
    return energyCost;
  }

  private static void checkMemorySize(String opName, BigInteger newMemSize) {
    if (newMemSize.compareTo(MEM_LIMIT) > 0) {
      throw Program.Exception.memoryOverflow(opName);
    }
  }

  private static BigInteger memNeeded(DataWord offset, DataWord size) {
    return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
  }

  private static boolean isDeadAccount(Program program, DataWord address) {
    return program.getContractState().getAccount(convertToTronAddress(address.getLast20Bytes()))
        == null;
  }
}
