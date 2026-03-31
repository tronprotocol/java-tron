package org.tron.common.runtime.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.vm.EnergyCost;
import org.tron.core.vm.JumpTable;
import org.tron.core.vm.Op;
import org.tron.core.vm.Operation;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Stack;

@Slf4j
public class VoteWitnessCost3Test extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  @BeforeClass
  public static void init() {
    CommonParameter.getInstance().setDebug(true);
    VMConfig.initAllowTvmVote(1);
    VMConfig.initAllowEnergyAdjustment(1);
  }

  @AfterClass
  public static void destroy() {
    ConfigLoader.disable = false;
    VMConfig.initAllowTvmVote(0);
    VMConfig.initAllowEnergyAdjustment(0);
    VMConfig.initAllowTvmOsaka(0);
    Args.clearParam();
  }

  private Program mockProgram(long witnessOffset, long witnessLength,
                              long amountOffset, long amountLength, int memSize) {
    Program program = mock(Program.class);
    Stack stack = new Stack();
    // Stack order: bottom -> top: witnessOffset, witnessLength, amountOffset, amountLength
    stack.push(new DataWord(witnessOffset));
    stack.push(new DataWord(witnessLength));
    stack.push(new DataWord(amountOffset));
    stack.push(new DataWord(amountLength));
    when(program.getStack()).thenReturn(stack);
    when(program.getMemSize()).thenReturn(memSize);
    return program;
  }

  private Program mockProgram(DataWord witnessOffset, DataWord witnessLength,
                              DataWord amountOffset, DataWord amountLength, int memSize) {
    Program program = mock(Program.class);
    Stack stack = new Stack();
    stack.push(witnessOffset);
    stack.push(witnessLength);
    stack.push(amountOffset);
    stack.push(amountLength);
    when(program.getStack()).thenReturn(stack);
    when(program.getMemSize()).thenReturn(memSize);
    return program;
  }

  @Test
  public void testNormalCase() {
    // 2 witnesses at offset 0, 2 amounts at offset 128
    Program program = mockProgram(0, 2, 128, 2, 0);
    long cost = EnergyCost.getVoteWitnessCost3(program);
    // amountArraySize = 2 * 32 + 32 = 96, memNeeded = 128 + 96 = 224
    // witnessArraySize = 2 * 32 + 32 = 96, memNeeded = 0 + 96 = 96
    // max = 224, memWords = (224 + 31) / 32 * 32 / 32 = 7
    // memEnergy = 3 * 7 + 7 * 7 / 512 = 21
    // total = 30000 + 21 = 30021
    assertEquals(30021, cost);
  }

  @Test
  public void testConsistentWithCost2ForSmallValues() {
    // For small values, cost3 should produce the same result as cost2
    long[][] testCases = {
        {0, 1, 64, 1, 0},     // 1 witness, 1 amount
        {0, 3, 128, 3, 0},    // 3 witnesses, 3 amounts
        {0, 5, 256, 5, 0},    // 5 witnesses, 5 amounts
        {64, 2, 192, 2, 0},   // non-zero offsets
        {0, 10, 512, 10, 0},  // 10 witnesses
    };

    for (long[] tc : testCases) {
      Program p2 = mockProgram(tc[0], tc[1], tc[2], tc[3], (int) tc[4]);
      Program p3 = mockProgram(tc[0], tc[1], tc[2], tc[3], (int) tc[4]);
      long cost2 = EnergyCost.getVoteWitnessCost2(p2);
      long cost3 = EnergyCost.getVoteWitnessCost3(p3);
      assertEquals("Mismatch for case: witnessOff=" + tc[0] + " witnessLen=" + tc[1]
          + " amountOff=" + tc[2] + " amountLen=" + tc[3], cost2, cost3);
    }
  }

  @Test
  public void testZeroLengthArrays() {
    // Both arrays have zero length, but cost3 always adds wordSize for dynamic array prefix
    Program program = mockProgram(0, 0, 0, 0, 0);
    long cost = EnergyCost.getVoteWitnessCost3(program);
    // arraySize = 0 * 32 + 32 = 32, memNeeded = 0 + 32 = 32
    // memWords = (32 + 31) / 32 * 32 / 32 = 1
    // memEnergy = 3 * 1 + 1 * 1 / 512 = 3
    assertEquals(30003, cost);
  }

  @Test
  public void testZeroLengthOneArray() {
    // witness array zero, amount array non-zero
    Program program = mockProgram(0, 0, 64, 1, 0);
    long cost = EnergyCost.getVoteWitnessCost3(program);
    // memWords = 128 / 32 = 4
    // memEnergy = 3 * 4 + 4 * 4 / 512 = 12
    assertEquals(30012, cost);
  }

  @Test
  public void testLargeArrayLengthOverflow() {
    // Use a very large value that would overflow in DataWord.mul() in cost2
    // DataWord max is 2^256-1, multiplying by 32 would overflow
    // In cost3, BigInteger handles this correctly and should trigger memoryOverflow
    String maxHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    DataWord largeLength = new DataWord(maxHex);
    DataWord zeroOffset = new DataWord(0);

    Program program = mockProgram(zeroOffset, new DataWord(1),
        zeroOffset, largeLength, 0);

    boolean overflowCaught = false;
    try {
      EnergyCost.getVoteWitnessCost3(program);
    } catch (Program.OutOfMemoryException e) {
      // cost3 should detect memory overflow via checkMemorySize
      overflowCaught = true;
    }
    assertTrue("cost3 should throw memoryOverflow for huge array length", overflowCaught);
  }

  @Test
  public void testLargeOffsetOverflow() {
    // Large offset + normal size should trigger memoryOverflow in cost3
    String largeHex = "00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    DataWord largeOffset = new DataWord(largeHex);

    Program program = mockProgram(largeOffset, new DataWord(1),
        new DataWord(0), new DataWord(1), 0);

    boolean overflowCaught = false;
    try {
      EnergyCost.getVoteWitnessCost3(program);
    } catch (Program.OutOfMemoryException e) {
      overflowCaught = true;
    }
    assertTrue("cost3 should throw memoryOverflow for huge offset", overflowCaught);
  }

  @Test
  public void testExistingMemorySize() {
    // When program already has memory allocated, additional cost is incremental
    Program p1 = mockProgram(0, 2, 128, 2, 0);
    long costFromZero = EnergyCost.getVoteWitnessCost3(p1);

    Program p2 = mockProgram(0, 2, 128, 2, 224);
    long costWithExistingMem = EnergyCost.getVoteWitnessCost3(p2);

    // With existing memory >= needed, no additional mem cost
    assertEquals(30000, costWithExistingMem);
    assertTrue(costFromZero > costWithExistingMem);
  }

  @Test
  public void testAmountArrayLargerThanWitnessArray() {
    // amount array needs more memory => amount determines cost
    Program program = mockProgram(0, 1, 0, 5, 0);
    long cost = EnergyCost.getVoteWitnessCost3(program);
    // witnessArraySize = 1 * 32 + 32 = 64, memNeeded = 0 + 64 = 64
    // amountArraySize = 5 * 32 + 32 = 192, memNeeded = 0 + 192 = 192
    // max = 192, memWords = (192 + 31) / 32 * 32 / 32 = 6
    // memEnergy = 3 * 6 + 6 * 6 / 512 = 18
    assertEquals(30018, cost);
  }

  @Test
  public void testWitnessArrayLargerThanAmountArray() {
    // witness array needs more memory => witness determines cost
    Program program = mockProgram(0, 5, 0, 1, 0);
    long cost = EnergyCost.getVoteWitnessCost3(program);
    // witnessArraySize = 5 * 32 + 32 = 192, memNeeded = 0 + 192 = 192
    // amountArraySize = 1 * 32 + 32 = 64, memNeeded = 0 + 64 = 64
    // max = 192
    assertEquals(30018, cost);
  }

  @Test
  public void testOperationRegistryWithoutOsaka() {
    VMConfig.initAllowTvmOsaka(0);
    JumpTable table = OperationRegistry.getTable();
    Operation voteOp = table.get(Op.VOTEWITNESS);
    assertTrue(voteOp.isEnabled());

    // Without osaka, should use cost2 (from adjustForFairEnergy since allowEnergyAdjustment=1)
    Program program = mockProgram(0, 2, 128, 2, 0);
    long cost = voteOp.getEnergyCost(program);
    long expectedCost2 = EnergyCost.getVoteWitnessCost2(
        mockProgram(0, 2, 128, 2, 0));
    assertEquals(expectedCost2, cost);
  }

  @Test
  public void testOperationRegistryWithOsaka() {
    VMConfig.initAllowTvmOsaka(1);
    try {
      JumpTable table = OperationRegistry.getTable();
      Operation voteOp = table.get(Op.VOTEWITNESS);
      assertTrue(voteOp.isEnabled());

      // With osaka, should use cost3
      Program program = mockProgram(0, 2, 128, 2, 0);
      long cost = voteOp.getEnergyCost(program);
      long expectedCost3 = EnergyCost.getVoteWitnessCost3(
          mockProgram(0, 2, 128, 2, 0));
      assertEquals(expectedCost3, cost);
    } finally {
      VMConfig.initAllowTvmOsaka(0);
    }
  }
}
