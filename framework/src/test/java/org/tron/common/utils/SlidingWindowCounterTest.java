package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class SlidingWindowCounterTest {

  private SlidingWindowCounter counter;

  @Before
  public void setUp() {
    counter = new SlidingWindowCounter(3);
  }

  @Test
  public void testIncrease() {
    counter.increase();
    counter.increase();
    counter.increase();
    assertEquals(3, counter.totalCount());
    counter.resizeWindow(5);
    assertNotNull(counter.toString());
  }

  @Test
  public void testTotalAndAdvance() {
    counter.increase();
    counter.increase();
    counter.advance();
    counter.increase();
    int total = counter.totalAndAdvance();
    assertEquals(3, total);
    assertEquals(3, counter.totalCount());
  }

  @Test
  public void testTotalCount() {
    counter.increase();
    counter.increase();
    counter.advance();
    assertEquals(2, counter.totalCount());
  }

  @Test
  public void testCircularWindow() {
    for (int i = 0; i < 3; i++) {
      counter.increase();
    }
    counter.increase();
    counter.advance();
    assertEquals(4, counter.totalCount());

    counter.increase();
    counter.increase();
    counter.increase();
    int total = counter.totalAndAdvance();
    assertEquals(7, total);
    assertEquals(7, counter.totalCount());
  }
}
