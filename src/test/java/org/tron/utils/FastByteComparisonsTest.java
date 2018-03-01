package org.tron.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.FastByteComparisons;

public class FastByteComparisonsTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");


  @Test
  public void testEqual() {
    byte[] b1 = new byte[]{1, 2, 3, 4};
    byte[] b2 = new byte[]{1, 2, 3, 4};
    logger.info("b1 equal b2 is {}", FastByteComparisons.equal(b1, b2));

    assertEquals(true, FastByteComparisons.equal(b1, b2));

  }

  @Test
  public void testCompareTo() {
    byte[] b1 = new byte[]{1, 2, 3, 4};
    byte[] b2 = new byte[]{1, 2, 3, 4};
    //logger.info("b1 compareTo b2 is {}",
    //    FastByteComparisons.compareTo(b1, 1, b1.length, b2, 1, b2.length));
    logger.info("b1 compareTo b2 is {}",
        FastByteComparisons.compareTo(b1, 2, b1.length, b2, 1, b2.length));
    byte[] b3 = new byte[]{1, 3, 5, 7, 9};
    byte[] b4 = new byte[]{2, 5, 3, 7, 9, 8, 1};
    logger.info("b3 compareTo b4 is {}",
        FastByteComparisons.compareTo(b3, 2, b3.length, b4, 2, b4.length));
  }


}

