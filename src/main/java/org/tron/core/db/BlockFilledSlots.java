package org.tron.core.db;

public class BlockFilledSlots {

  public static int SLOT_NUMBER = 128;

  private int[] blockFilledSlots = new int[SLOT_NUMBER];
  private int index = 0;

  public BlockFilledSlots() {
    init();
  }

  private void init() {
    for (int i = 0; i < SLOT_NUMBER; i++) {
      blockFilledSlots[i] = 1;
    }
  }

  public void applyBlock(boolean fillBlock) {
    blockFilledSlots[index] = fillBlock ? 1 : 0;
    index = (index + 1 >= SLOT_NUMBER) ? 0 : index + 1;
  }

  public int calculateFilledSlotsCount() {
    int count = 0;
    for (int i = 0; i < SLOT_NUMBER; i++) {
      count += blockFilledSlots[i];
    }
    return count;
  }

  public int[] getBlockFilledSlots() {
    return blockFilledSlots;
  }

}
