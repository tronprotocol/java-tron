package org.tron.utils;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.BIUtil;

public class BUtilTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");


  @Test
  public void testIsLessThan() {
    BigInteger valueA = new BigInteger("92233720368547758079");
    BigInteger valueB = new BigInteger("1");
    logger.info("valueA is less than valueB = {}", BIUtil.isLessThan(valueA, valueB));
    assertEquals(false, BIUtil.isLessThan(valueA, valueB));
    BigInteger valueC = new BigInteger("11111111111111111111111111");
    BigInteger valueD = new BigInteger("99999999999999999999999999");
    logger.info("valueC is less than valueD = {}", BIUtil.isLessThan(valueC, valueD));
    assertEquals(true, BIUtil.isLessThan(valueC, valueD));
    BigInteger valueE = new BigInteger("-9223372036854775807");
    BigInteger valueF = new BigInteger("-9223372036854775807");
    logger.info("valueE is less than valueF = {}", BIUtil.isLessThan(valueE, valueF));
    assertEquals(false, BIUtil.isLessThan(valueA, valueB));
    BigInteger valueG = new BigInteger("-9223372036854775807");
    BigInteger valueH = new BigInteger("9223372036854775807");
    logger.info("valueE is less than valueF = {}", BIUtil.isLessThan(valueG, valueH));
    assertEquals(false, BIUtil.isLessThan(valueA, valueB));
  }

}
