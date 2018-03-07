package org.tron.core.db;

import org.junit.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class BlockFilledSlotsTest {

  @Test
  public void testInit() {
    final int SLOT_NUMBER = 128;
    int[] block_filled_slots = new int[SLOT_NUMBER];
    int[] block_filled_slots2 = new int[SLOT_NUMBER];

    for (int i = 0; i < SLOT_NUMBER; i++) {
      block_filled_slots[i] = 1;
    }

    Arrays.fill(block_filled_slots2, 1);
    assertArrayEquals(block_filled_slots, block_filled_slots2);
  }

  @Test
  public void testApplyBlock() {
    final int SLOT_NUMBER = 128;
    int index = 0;
    int index2 = 0;
    int index3 = 0;

    for (int i = 0; i <= SLOT_NUMBER * 2; i++) {
      index = (index + 1 >= SLOT_NUMBER) ? 0 : index + 1;
      index2 = (index2 + 1) % SLOT_NUMBER;
      index3 = (i + 1) % SLOT_NUMBER;
      assertEquals(index, index2, index3);
    }

    index = 127;
    index = (index + 1 >= SLOT_NUMBER) ? 0 : index + 1;
    assertEquals(0, index);
  }

  @Test
  public void testCalculateFilledSlotsCount() {
    final int SLOT_NUMBER = 128;
    int[] block_filled_slots = new int[SLOT_NUMBER];
    int[] block_filled_slots2 = new int[SLOT_NUMBER];

    for (int i = 0; i < SLOT_NUMBER; i++) {
      block_filled_slots[i] = 1;
    }

    Arrays.fill(block_filled_slots2, 1);

    block_filled_slots[1] = 0;
    block_filled_slots[2] = 0;
    block_filled_slots[3] = 0;

    block_filled_slots2[1] = 0;
    block_filled_slots2[2] = 0;
    block_filled_slots2[3] = 0;

    int count = 0;
    for (int i = 0; i < SLOT_NUMBER; i++) {
      count += block_filled_slots[i];
    }

    int count2 = IntStream.of(block_filled_slots2).sum();

    assertEquals(count, count2);
  }
}