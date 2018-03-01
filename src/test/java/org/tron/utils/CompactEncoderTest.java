package org.tron.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.CompactEncoder;

public class CompactEncoderTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testPackNibbles() {
    byte[] test = new byte[]{1, 2, 3, 4, 5, 6, 7};
    byte[] expectedData = new byte[]{0x11, 0x23, 0x45, 0x67};
    assertArrayEquals("odd compact encode fail", expectedData, CompactEncoder.packNibbles(test));
    logger.info("CompactEncoder.packNibbles(test) ={}", CompactEncoder.packNibbles(test));
    byte[] testtwo = new byte[]{0, 1, 2, 3, 4, 5};
    byte[] expectedDataTwo = new byte[]{0x00, 0x01, 0x23, 0x45};
    assertArrayEquals("odd compact encode fail", expectedDataTwo,
        CompactEncoder.packNibbles(testtwo));
    logger.info("CompactEncoder.packNibbles(testtwo) ={}", CompactEncoder.packNibbles(testtwo));
  }


  @Test
  public void testHasTerminator() {
    byte[] test = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 10};
    assertEquals(false, CompactEncoder.hasTerminator(test));
    logger.info("CompactEncoder.packNibbles(test) ={}", CompactEncoder.hasTerminator(test));

    byte[] test1 = new byte[]{32, 33, 34, 35};
    assertEquals(true, CompactEncoder.hasTerminator(test1));
    logger.info("CompactEncoder.packNibbles(test) ={}", CompactEncoder.hasTerminator(test1));
  }

  @Test
  public void testUnpackToNibbles() {
    byte[] test = new byte[]{0x01, 0x02, 0x03, 0x04};
    byte[] excepted = new byte[]{0, 2, 0, 3, 0, 4};
    assertArrayEquals(excepted, CompactEncoder.unpackToNibbles(test));
    logger.info("CompactEncoder.unpackToNibbles(test) ={}", CompactEncoder.unpackToNibbles(test));
  }

  @Test
  public void testBinToNibbles() {
    byte[] test = "tron".getBytes();
    byte[] excepted = new byte[]{7, 4, 7, 2, 6, 15, 6, 14, 16};
    assertArrayEquals(excepted, CompactEncoder.binToNibbles(test));
    logger.info("CompactEncoder.binToNibblesNoTerminator(test) ={}",
        CompactEncoder.binToNibbles(test));

  }

  @Test
  public void testBinToNibblesNoTerminator() {
    byte[] test = "message".getBytes();
    byte[] excepted = new byte[]{6, 13, 6, 5, 7, 3, 7, 3, 6, 1, 6, 7, 6, 5};
    assertArrayEquals(excepted, CompactEncoder.binToNibblesNoTerminator(test));
    logger.info("CompactEncoder.binToNibblesNoTerminator(test) ={}",
        CompactEncoder.binToNibblesNoTerminator(test));

  }

}


