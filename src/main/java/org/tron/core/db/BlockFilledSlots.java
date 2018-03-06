package org.tron.core.db;

public class BlockFilledSlots {

  public static int SLOT_NUMBER = 128;

  private int[] block_filled_slots = new int[SLOT_NUMBER];
  private int index = 0;

  public BlockFilledSlots() {
    init();
  }

  private void init() {
    for (int i = 0; i < SLOT_NUMBER; i++) {
      block_filled_slots[i] = 1;
    }
  }

  public void applyBlock(boolean fillBlock) {
    block_filled_slots[index] = fillBlock ? 1 : 0;
    index = (index + 1 >= SLOT_NUMBER) ? 0 : index + 1;
  }

  public int calculateFilledSlotsCount() {
    int count = 0;
    for (int i = 0; i < SLOT_NUMBER; i++) {
      count += block_filled_slots[i];
    }
    return count;
  }

  public int[] getBlockFilledSlots() {
    return block_filled_slots;
  }

}
