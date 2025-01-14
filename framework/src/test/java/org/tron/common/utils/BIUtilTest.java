package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import org.junit.Test;

public class BIUtilTest {

  @Test
  public void testIsLessThan() {
    BigInteger valueA = BigInteger.valueOf(1);
    BigInteger valueB = BigInteger.valueOf(2);
    assertTrue(BIUtil.isLessThan(valueA, valueB));

    valueA = BigInteger.valueOf(3);
    valueB = BigInteger.valueOf(3);
    assertFalse(BIUtil.isLessThan(valueA, valueB));

    valueA = BigInteger.valueOf(4);
    valueB = BigInteger.valueOf(2);
    assertFalse(BIUtil.isLessThan(valueA, valueB));
  }

  @Test
  public void testIsZero() {
    BigInteger value = BigInteger.ZERO;
    assertTrue(BIUtil.isZero(value));

    value = BigInteger.valueOf(1);
    assertFalse(BIUtil.isZero(value));

    value = BigInteger.valueOf(-1);
    assertFalse(BIUtil.isZero(value));
  }

  @Test
  public void testIsEqual() {
    BigInteger valueA = BigInteger.valueOf(1);
    BigInteger valueB = BigInteger.valueOf(1);
    assertTrue(BIUtil.isEqual(valueA, valueB));

    valueA = BigInteger.valueOf(2);
    valueB = BigInteger.valueOf(3);
    assertFalse(BIUtil.isEqual(valueA, valueB));
  }

  @Test
  public void testIsNotEqual() {
    BigInteger valueA = BigInteger.valueOf(1);
    BigInteger valueB = BigInteger.valueOf(2);
    assertTrue(BIUtil.isNotEqual(valueA, valueB));

    valueA = BigInteger.valueOf(3);
    valueB = BigInteger.valueOf(3);
    assertFalse(BIUtil.isNotEqual(valueA, valueB));
  }

  @Test
  public void testIsMoreThan() {
    BigInteger valueA = BigInteger.valueOf(3);
    BigInteger valueB = BigInteger.valueOf(2);
    assertTrue(BIUtil.isMoreThan(valueA, valueB));

    valueA = BigInteger.valueOf(1);
    valueB = BigInteger.valueOf(2);
    assertFalse(BIUtil.isMoreThan(valueA, valueB));

    valueA = BigInteger.valueOf(2);
    valueB = BigInteger.valueOf(2);
    assertFalse(BIUtil.isMoreThan(valueA, valueB));
  }

  @Test
  public void testSum() {
    BigInteger valueA = BigInteger.valueOf(5);
    BigInteger valueB = BigInteger.valueOf(7);
    BigInteger expected = BigInteger.valueOf(12);
    assertEquals(expected, BIUtil.sum(valueA, valueB));
  }

  @Test
  public void testToBI_byteArray() {
    byte[] data = {1, 0, 0, 0, 0, 0, 0, 1}; // BigInteger(128)
    BigInteger expected = new BigInteger(1, data);
    assertEquals(expected, BIUtil.toBI(data));
  }

  @Test
  public void testToBI_long() {
    long data = 123456789L;
    BigInteger expected = BigInteger.valueOf(data);
    assertEquals(expected, BIUtil.toBI(data));
  }

  @Test
  public void testIsPositive() {
    BigInteger value = BigInteger.valueOf(1);
    assertTrue(BIUtil.isPositive(value));

    value = BigInteger.ZERO;
    assertFalse(BIUtil.isPositive(value));

    value = BigInteger.valueOf(-1);
    assertFalse(BIUtil.isPositive(value));
  }

  @Test
  public void testIsNotCovers() {
    BigInteger covers = BigInteger.valueOf(5);
    BigInteger value = BigInteger.valueOf(10);
    assertTrue(BIUtil.isNotCovers(covers, value));

    covers = BigInteger.valueOf(10);
    value = BigInteger.valueOf(5);
    assertFalse(BIUtil.isNotCovers(covers, value));

    covers = BigInteger.valueOf(10);
    value = BigInteger.valueOf(10);
    assertFalse(BIUtil.isNotCovers(covers, value));
  }

  @Test
  public void testMax() {
    BigInteger first = BigInteger.valueOf(5);
    BigInteger second = BigInteger.valueOf(10);
    assertEquals(second, BIUtil.max(first, second));

    first = BigInteger.valueOf(15);
    second = BigInteger.valueOf(10);
    assertEquals(first, BIUtil.max(first, second));

    first = BigInteger.valueOf(10);
    second = BigInteger.valueOf(10);
    assertEquals(first, BIUtil.max(first, second));
  }

  @Test
  public void testAddSafely() {
    int a = Integer.MAX_VALUE;
    int b = 1;
    int expected = Integer.MAX_VALUE;
    assertEquals(expected, BIUtil.addSafely(a, b));

    a = Integer.MAX_VALUE - 1;
    b = 2;
    assertEquals(expected, BIUtil.addSafely(a, b));

    a = 1;
    expected = 3;
    assertEquals(expected, BIUtil.addSafely(a, b));
  }
}
