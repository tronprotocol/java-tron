package org.tron.core.vm;

import java.math.BigInteger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Stack;

import static org.tron.core.Constant.DYNAMIC_ENERGY_FACTOR_DECIMAL;

public class EnergyCost {

  private static final long ZERO_TIER = 0;
  private static final long BASE_TIER = 2;
  private static final long VERY_LOW_TIER = 3;
  private static final long LOW_TIER = 5;
  private static final long MID_TIER = 8;
  private static final long HIGH_TIER = 10;
  private static final long EXT_TIER = 20;
  private static final long SPECIAL_TIER = 1;

  private static final long EXP_ENERGY = 10;
  private static final long EXP_BYTE_ENERGY = 10;
  private static final long SHA3 = 30;
  // 3MB
  private static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);
  private static final long MEMORY = 3;
  private static final long COPY_ENERGY = 3;
  private static final long SHA3_WORD = 6;
  private static final long SLOAD = 50;
  private static final long CLEAR_SSTORE = 5000;
  private static final long SET_SSTORE = 20000;
  private static final long RESET_SSTORE = 5000;
  private static final long LOG_DATA_ENERGY = 8;
  private static final long LOG_ENERGY = 375;
  private static final long LOG_TOPIC_ENERGY = 375;
  private static final long BALANCE = 20;
  private static final long FREEZE = 20000;
  private static final long NEW_ACCT_CALL = 25000;
  private static final long UNFREEZE = 20000;
  private static final long FREEZE_EXPIRE_TIME = 50;
  private static final long FREEZE_V2 = 10000;
  private static final long UNFREEZE_V2 = 10000;
  private static final long WITHDRAW_EXPIRE_UNFREEZE = 10000;
  private static final long CANCEL_ALL_UNFREEZE_V2 = 10000;
  private static final long DELEGATE_RESOURCE = 10000;
  private static final long UN_DELEGATE_RESOURCE = 10000;
  private static final long VOTE_WITNESS = 30000;
  private static final long WITHDRAW_REWARD = 20000;
  private static final long CREATE = 32000;
  private static final long CALL_ENERGY = 40;
  private static final long VT_CALL = 9000;
  private static final long STIPEND_CALL = 2300;
  private static final long EXT_CODE_COPY = 20;
  private static final long EXT_CODE_SIZE = 20;
  private static final long EXT_CODE_HASH = 400;
  private static final long SUICIDE = 0;
  private static final long STOP = 0;
  private static final long CREATE_DATA = 200;

  public static long getZeroTierCost(Program ignored) {
    return ZERO_TIER;
  }

  public static long getVeryLowTierCost(Program ignored) {
    return VERY_LOW_TIER;
  }

  public static long getLowTierCost(Program ignored) {
    return LOW_TIER;
  }

  public static long getMidTierCost(Program ignored) {
    return MID_TIER;
  }

  public static long getBaseTierCost(Program ignored) {
    return BASE_TIER;
  }

  public static long getExtTierCost(Program ignored) {
    return EXT_TIER;
  }

  public static long getHighTierCost(Program ignored) {
    return HIGH_TIER;
  }

  public static long getSpecialTierCost(Program ignored) {
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

  public static long getExtCodeSizeCost(Program ignored) {
    return EXT_CODE_SIZE;
  }

  public static long getSha3Cost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = SHA3 + calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, Op.SHA3);
    DataWord size = stack.get(stack.size() - 2);
    long chunkUsed = (size.longValueSafe() + 31) / 32;
    energyCost += chunkUsed * SHA3_WORD;
    return energyCost;
  }

  public static long getCodeCopyCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 3)),
        stack.get(stack.size() - 3).longValueSafe(), Op.CODECOPY);
    return energyCost;
  }

  public static long getReturnDataCopyCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 3)),
        stack.get(stack.size() - 3).longValueSafe(), Op.RETURNDATACOPY);
    return energyCost;
  }

  public static long getCallDataCopyCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 3)),
        stack.get(stack.size() - 3).longValueSafe(), Op.CALLDATACOPY);
    return energyCost;
  }

  public static long getExtCodeCopyCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = EXT_CODE_COPY + calcMemEnergy(oldMemSize,
        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 4)),
        stack.get(stack.size() - 4).longValueSafe(), Op.EXTCODECOPY);
    return energyCost;
  }

  public static long getExtCodeHashCost(Program ignored) {
    return EXT_CODE_HASH;
  }

  public static long getMloadCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    return calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), new DataWord(32)),
        0, Op.MLOAD);
  }

  public static long getMloadCost2(Program program) {
    return SPECIAL_TIER + getMloadCost(program);
  }

  public static long getMStoreCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    return calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), new DataWord(32)),
        0, Op.MSTORE);
  }

  public static long getMStoreCost2(Program program) {
    return SPECIAL_TIER + getMStoreCost(program);
  }

  public static long getMStore8Cost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    return calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), DataWord.ONE()),
        0, Op.MSTORE8);
  }

  public static long getMStore8Cost2(Program program) {
    return SPECIAL_TIER + getMStore8Cost(program);
  }

  public static long getSloadCost(Program ignored) {
    return SLOAD;
  }

  public static long getReturnCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = STOP + calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, Op.RETURN);
    return energyCost;
  }

  public static long getRevertCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    long energyCost = STOP + calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, Op.REVERT);
    return energyCost;
  }

  public static long getSstoreCost(Program program) {
    Stack stack = program.getStack();
    DataWord newValue = stack.get(stack.size() - 2);
    DataWord oldValue = program.storageLoad(stack.peek());

    if (oldValue == null && !newValue.isZero()) {
      // set a new not-zero value
      return SET_SSTORE;
    }
    if (oldValue != null && newValue.isZero()) {
      // set zero to an old value
      return CLEAR_SSTORE;
    }
    // include:
    // [1] oldValue == null && newValue == 0
    // [2] oldValue != null && newValue != 0
    return RESET_SSTORE;

  }

  public static long getLogCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    int opIntValue = program.getCurrentOpIntValue();
    int nTopics = opIntValue - Op.LOG0;
    BigInteger dataSize = stack.get(stack.size() - 2).value();
    BigInteger dataCost = dataSize
        .multiply(BigInteger.valueOf(LOG_DATA_ENERGY));
    if (program.getEnergyLimitLeft().value().compareTo(dataCost) < 0) {
      throw new Program.OutOfEnergyException(
          "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
          Op.getNameOf(opIntValue),
          dataCost.longValueExact(), program.getEnergyLimitLeft().longValueSafe());
    }
    long energyCost = LOG_ENERGY + LOG_TOPIC_ENERGY * nTopics
        + LOG_DATA_ENERGY * stack.get(stack.size() - 2).longValue()
        + calcMemEnergy(oldMemSize,
        memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, opIntValue);

    checkMemorySize(opIntValue, memNeeded(stack.peek(), stack.get(stack.size() - 2)));
    return energyCost;
  }

  public static long getSuicideCost(Program ignored) {
    return SUICIDE;
  }

  public static long getBalanceCost(Program ignored) {
    return BALANCE;
  }

  public static long getFreezeCost(Program program) {

    Stack stack = program.getStack();
    DataWord receiverAddressWord = stack.get(stack.size() - 3);
    if (isDeadAccount(program, receiverAddressWord)) {
      return FREEZE + NEW_ACCT_CALL;
    }
    return FREEZE;
  }

  public static long getUnfreezeCost(Program ignored) {
    return UNFREEZE;
  }

  public static long getFreezeExpireTimeCost(Program ignored) {
    return FREEZE_EXPIRE_TIME;
  }

  public static long getFreezeBalanceV2Cost(Program ignored) {
    return FREEZE_V2;
  }

  public static long getUnfreezeBalanceV2Cost(Program ignored) {
    return UNFREEZE_V2;
  }

  public static long getWithdrawExpireUnfreezeCost(Program ignored) {
    return WITHDRAW_EXPIRE_UNFREEZE;
  }

  public static long getCancelAllUnfreezeV2Cost(Program ignored) {
    return CANCEL_ALL_UNFREEZE_V2;
  }

  public static long getDelegateResourceCost(Program ignored) {
    return DELEGATE_RESOURCE;
  }

  public static long getUnDelegateResourceCost(Program ignored) {
    return UN_DELEGATE_RESOURCE;
  }

  public static long getVoteWitnessCost(Program program) {

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

    return VOTE_WITNESS + calcMemEnergy(oldMemSize,
        (amountArrayMemoryNeeded.compareTo(witnessArrayMemoryNeeded) > 0
            ? amountArrayMemoryNeeded : witnessArrayMemoryNeeded), 0, Op.VOTEWITNESS);
  }

  public static long getWithdrawRewardCost(Program ignored) {
    return WITHDRAW_REWARD;
  }

  public static long getCreateCost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    return CREATE + calcMemEnergy(oldMemSize,
        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)), 0, Op.CREATE);
  }

  public static long getCreate2Cost(Program program) {
    Stack stack = program.getStack();
    long oldMemSize = program.getMemSize();
    DataWord codeSize = stack.get(stack.size() - 3);
    long energyCost = CREATE;
    energyCost += calcMemEnergy(oldMemSize,
        memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)),
        0, Op.CREATE2);
    return energyCost + DataWord.sizeInWords(codeSize.intValueSafe()) * SHA3_WORD;
  }

  public static long getCallCost(Program program) {
    Stack stack = program.getStack();
    // here, contract call an other contract, or a library, and so on
    long energyCost = CALL_ENERGY;
    DataWord callAddressWord = stack.get(stack.size() - 2);
    DataWord value = stack.get(stack.size() - 3);
    int opOff = 4;
    //check to see if account does not exist and is not a precompiled contract
    if (!value.isZero()) {
      energyCost += VT_CALL;
      if (isDeadAccount(program, callAddressWord)) {
        energyCost += NEW_ACCT_CALL;
      }
    }
    return getCalculateCallCost(stack, program, energyCost, opOff);
  }

  public static long getStaticCallCost(Program program) {
    Stack stack = program.getStack();
    long energyCost = CALL_ENERGY;
    int opOff = 3;
    return getCalculateCallCost(stack, program, energyCost, opOff);
  }

  public static long getDelegateCallCost(Program program) {
    Stack stack = program.getStack();
    long energyCost = CALL_ENERGY;
    int opOff = 3;
    return getCalculateCallCost(stack, program, energyCost, opOff);
  }

  public static long getCallCodeCost(Program program) {
    Stack stack = program.getStack();
    long energyCost = CALL_ENERGY;
    DataWord value = stack.get(stack.size() - 3);
    int opOff = 4;
    if (!value.isZero()) {
      energyCost += VT_CALL;
    }
    return getCalculateCallCost(stack, program, energyCost, opOff);
  }

  public static long getCallTokenCost(Program program) {
    Stack stack = program.getStack();
    long energyCost = CALL_ENERGY;
    DataWord callAddressWord = stack.get(stack.size() - 2);
    DataWord value = stack.get(stack.size() - 3);
    int opOff = 5;
    //check to see if account does not exist and is not a precompiled contract
    if (!value.isZero()) {
      energyCost += VT_CALL;
      if (isDeadAccount(program, callAddressWord)) {
        energyCost += NEW_ACCT_CALL;
      }
    }
    return getCalculateCallCost(stack, program, energyCost, opOff);
  }

  public static long getCalculateCallCost(Stack stack, Program program,
                                          long energyCost, int opOff) {
    int op = program.getCurrentOpIntValue();
    long oldMemSize = program.getMemSize();
    DataWord callEnergyWord = stack.get(stack.size() - 1);
    // in offset+size
    BigInteger in = memNeeded(stack.get(stack.size() - opOff),
        stack.get(stack.size() - opOff - 1));
    // out offset+size
    BigInteger out = memNeeded(stack.get(stack.size() - opOff - 2),
        stack.get(stack.size() - opOff - 3));
    energyCost += calcMemEnergy(oldMemSize, in.max(out),
        0, op);

    if (VMConfig.allowDynamicEnergy()) {
      long factor = program.getContextContractFactor();
      if (factor > DYNAMIC_ENERGY_FACTOR_DECIMAL) {
        long penalty = energyCost * factor / DYNAMIC_ENERGY_FACTOR_DECIMAL - energyCost;
        if (penalty < 0) {
          penalty = 0;
        }
        program.setCallPenaltyEnergy(penalty);
        energyCost += penalty;
      }
    }

    if (energyCost > program.getEnergyLimitLeft().longValueSafe()) {
      throw new Program.OutOfEnergyException(
          "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d]",
          Op.getNameOf(op),
          energyCost, program.getEnergyLimitLeft().longValueSafe());
    }
    DataWord getEnergyLimitLeft = program.getEnergyLimitLeft().clone();
    getEnergyLimitLeft.sub(new DataWord(energyCost));

    DataWord adjustedCallEnergy = program.getCallEnergy(callEnergyWord, getEnergyLimitLeft);
    program.setAdjustedCallEnergy(adjustedCallEnergy);
    energyCost += adjustedCallEnergy.longValueSafe();
    return energyCost;
  }

  public static long getNewAcctCall() {
    return NEW_ACCT_CALL;
  }

  public static long getCreateData() {
    return CREATE_DATA;
  }


  private static long calcMemEnergy(long oldMemSize, BigInteger newMemSize,
                             long copySize, int op) {
    long energyCost = 0;

    checkMemorySize(op, newMemSize);

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

  private static void checkMemorySize(int op, BigInteger newMemSize) {
    if (newMemSize.compareTo(MEM_LIMIT) > 0) {
      throw Program.Exception.memoryOverflow(op);
    }
  }

  private static BigInteger memNeeded(DataWord offset, DataWord size) {
    return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
  }

  private static boolean isDeadAccount(Program program, DataWord address) {
    return program.getContractState().getAccount(address.toTronAddress()) == null;
  }
}
