package org.tron.core.vm;

import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Stack;

import java.math.BigInteger;

public class NewEnergyCost {

  private static final long ZERO_TIER = 0;
  private static final long BASE_TIER = 2;
  private static final long VERY_LOW_TIER = 3;
  private static final long LOW_TIER = 5;
  private static final long MID_TIER = 8;
  private static final long HIGH_TIER = 10;
  private static final long EXT_TIER = 20;

  private static final long EXP_ENERGY = 10;
  private static final long EXP_BYTE_ENERGY = 10;
  private static final int SHA3 = 30;
  // 3MB
  private static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);
  private static final int MEMORY = 3;
  private static final int COPY_ENERGY = 3;
  private static final int SHA3_WORD = 6;
  private static final int SLOAD = 50;


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
}
