package org.tron.core.db;

import java.util.Arrays;
import java.util.stream.IntStream;

public class BlockFilledSlots {

  public static int SLOT_NUMBER = 128;

  private int[] block_filled_slots = new int[SLOT_NUMBER];
  private int index = 0;

  public BlockFilledSlots() {
    init();
  }

  private void init() {
    Arrays.fill(block_filled_slots, 1);
  }

  public void applyBlock(boolean fillBlock) {
    block_filled_slots[index] = fillBlock ? 1 : 0;
    index = (index + 1) % SLOT_NUMBER;
  }

  public int calculateFilledSlotsCount() {
    return IntStream.of(block_filled_slots).sum();
  }

  public int[] getBlockFilledSlots() {
    return block_filled_slots;
  }

}
