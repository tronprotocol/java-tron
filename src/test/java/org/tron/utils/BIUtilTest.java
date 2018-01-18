package org.tron.utils;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class BIUtilTest {

  @Test
  public void testIsLessThan()
  {
    BigInteger bigInteger1 = BigInteger.valueOf(1000);
    BigInteger bigInteger2 = BigInteger.valueOf(1001);

    Assert.assertTrue(BIUtil.isLessThan(bigInteger1, bigInteger2));
    Assert.assertFalse(BIUtil.isLessThan(bigInteger1, bigInteger1));
    Assert.assertFalse(BIUtil.isLessThan(bigInteger2, bigInteger1));
  }

  @Test
  public void testIsGreaterThan()
  {
    BigInteger bigInteger1 = BigInteger.valueOf(1000);
    BigInteger bigInteger2 = BigInteger.valueOf(1001);

    Assert.assertTrue(BIUtil.isGreaterThan(bigInteger2, bigInteger1));
    Assert.assertFalse(BIUtil.isGreaterThan(bigInteger1, bigInteger1));
    Assert.assertFalse(BIUtil.isGreaterThan(bigInteger1, bigInteger2));
  }

  @Test
  public  void testIsEqualTo()
  {
    BigInteger bigInteger1 = BigInteger.valueOf(1000);
    BigInteger bigInteger2 = BigInteger.valueOf(1001);

    Assert.assertTrue(BIUtil.isEqualTo(bigInteger1, bigInteger1));
    Assert.assertFalse(BIUtil.isEqualTo(bigInteger1, bigInteger2));
  }
}
