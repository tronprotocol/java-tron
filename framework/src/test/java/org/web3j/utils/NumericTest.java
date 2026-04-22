package org.web3j.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;
import org.web3j.exceptions.MessageDecodingException;
import org.web3j.exceptions.MessageEncodingException;

public class NumericTest {

  // --- encodeQuantity ---

  @Test
  public void testEncodeQuantityZero() {
    assertEquals("0x0", Numeric.encodeQuantity(BigInteger.ZERO));
  }

  @Test
  public void testEncodeQuantityPositive() {
    assertEquals("0xff", Numeric.encodeQuantity(BigInteger.valueOf(255)));
    assertEquals("0x1", Numeric.encodeQuantity(BigInteger.ONE));
    assertEquals("0x400", Numeric.encodeQuantity(BigInteger.valueOf(1024)));
  }

  @Test(expected = MessageEncodingException.class)
  public void testEncodeQuantityNegativeThrows() {
    Numeric.encodeQuantity(BigInteger.valueOf(-1));
  }

  // --- decodeQuantity ---

  @Test
  public void testDecodeQuantityHex() {
    assertEquals(BigInteger.valueOf(255), Numeric.decodeQuantity("0xff"));
    assertEquals(BigInteger.ZERO, Numeric.decodeQuantity("0x0"));
    assertEquals(BigInteger.valueOf(1024), Numeric.decodeQuantity("0x400"));
  }

  @Test
  public void testDecodeQuantityLongValue() {
    assertEquals(BigInteger.valueOf(123), Numeric.decodeQuantity("123"));
    assertEquals(BigInteger.ZERO, Numeric.decodeQuantity("0"));
  }

  @Test(expected = MessageDecodingException.class)
  public void testDecodeQuantityInvalidShortHex() {
    Numeric.decodeQuantity("0x");
  }

  @Test(expected = MessageDecodingException.class)
  public void testDecodeQuantityNullThrows() {
    Numeric.decodeQuantity(null);
  }

  @Test(expected = MessageDecodingException.class)
  public void testDecodeQuantityNoPrefix() {
    // Not a long and not valid hex (no 0x prefix, but not parseable as long)
    Numeric.decodeQuantity("gg");
  }

  // --- cleanHexPrefix / containsHexPrefix / prependHexPrefix ---

  @Test
  public void testCleanHexPrefix() {
    assertEquals("abcdef", Numeric.cleanHexPrefix("0xabcdef"));
    assertEquals("abcdef", Numeric.cleanHexPrefix("abcdef"));
  }

  @Test
  public void testContainsHexPrefix() {
    assertTrue(Numeric.containsHexPrefix("0xabc"));
    assertFalse(Numeric.containsHexPrefix("abc"));
    assertFalse(Numeric.containsHexPrefix(""));
    assertFalse(Numeric.containsHexPrefix(null));
    assertFalse(Numeric.containsHexPrefix("0"));
  }

  @Test
  public void testPrependHexPrefix() {
    assertEquals("0xabc", Numeric.prependHexPrefix("abc"));
    assertEquals("0xabc", Numeric.prependHexPrefix("0xabc"));
  }

  // --- toBigInt ---

  @Test
  public void testToBigIntFromBytes() {
    byte[] bytes = new byte[] {0x01, 0x00};
    assertEquals(BigInteger.valueOf(256), Numeric.toBigInt(bytes));
  }

  @Test
  public void testToBigIntFromBytesWithOffset() {
    byte[] bytes = new byte[] {(byte) 0xff, 0x01, 0x00, (byte) 0xff};
    assertEquals(BigInteger.valueOf(256), Numeric.toBigInt(bytes, 1, 2));
  }

  @Test
  public void testToBigIntFromHexString() {
    assertEquals(BigInteger.valueOf(255), Numeric.toBigInt("0xff"));
    assertEquals(BigInteger.valueOf(255), Numeric.toBigInt("ff"));
  }

  @Test
  public void testToBigIntNoPrefix() {
    assertEquals(BigInteger.valueOf(255), Numeric.toBigIntNoPrefix("ff"));
    assertEquals(BigInteger.valueOf(4096), Numeric.toBigIntNoPrefix("1000"));
  }

  // --- toHexString ---

  @Test
  public void testToHexStringWithPrefix() {
    byte[] bytes = new byte[] {(byte) 0xab, (byte) 0xcd};
    assertEquals("0xabcd", Numeric.toHexString(bytes));
  }

  @Test
  public void testToHexStringNoPrefix() {
    byte[] bytes = new byte[] {(byte) 0xab, (byte) 0xcd};
    assertEquals("abcd", Numeric.toHexStringNoPrefix(bytes));
  }

  @Test
  public void testToHexStringWithOffsetAndLength() {
    byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04};
    assertEquals("0x0203", Numeric.toHexString(bytes, 1, 2, true));
    assertEquals("0203", Numeric.toHexString(bytes, 1, 2, false));
  }

  @Test
  public void testToHexStringWithPrefixBigInteger() {
    assertEquals("0xff", Numeric.toHexStringWithPrefix(BigInteger.valueOf(255)));
  }

  @Test
  public void testToHexStringNoPrefixBigInteger() {
    assertEquals("ff", Numeric.toHexStringNoPrefix(BigInteger.valueOf(255)));
  }

  // --- toHexStringZeroPadded ---

  @Test
  public void testToHexStringWithPrefixZeroPadded() {
    assertEquals("0x00ff", Numeric.toHexStringWithPrefixZeroPadded(BigInteger.valueOf(255), 4));
  }

  @Test
  public void testToHexStringNoPrefixZeroPadded() {
    assertEquals("00ff", Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(255), 4));
  }

  @Test
  public void testToHexStringZeroPaddedExactSize() {
    assertEquals("ff", Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(255), 2));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testToHexStringZeroPaddedTooLarge() {
    Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(256), 1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testToHexStringZeroPaddedNegative() {
    Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(-1), 4);
  }

  // --- toHexStringWithPrefixSafe ---

  @Test
  public void testToHexStringWithPrefixSafeSingleDigit() {
    // Value "0" would produce "0", which is length 1 < 2, so it pads to "00"
    assertEquals("0x00", Numeric.toHexStringWithPrefixSafe(BigInteger.ZERO));
  }

  @Test
  public void testToHexStringWithPrefixSafeMultipleDigits() {
    assertEquals("0xff", Numeric.toHexStringWithPrefixSafe(BigInteger.valueOf(255)));
  }

  // --- hexStringToByteArray ---

  @Test
  public void testHexStringToByteArrayEvenLength() {
    assertArrayEquals(new byte[] {(byte) 0xab, (byte) 0xcd},
        Numeric.hexStringToByteArray("abcd"));
  }

  @Test
  public void testHexStringToByteArrayWithPrefix() {
    assertArrayEquals(new byte[] {(byte) 0xab, (byte) 0xcd},
        Numeric.hexStringToByteArray("0xabcd"));
  }

  @Test
  public void testHexStringToByteArrayOddLength() {
    // "abc" => odd, prepend implicit 0 => "0abc" => {0x0a, 0xbc}
    assertArrayEquals(new byte[] {0x0a, (byte) 0xbc},
        Numeric.hexStringToByteArray("abc"));
  }

  @Test
  public void testHexStringToByteArrayEmpty() {
    assertArrayEquals(new byte[] {}, Numeric.hexStringToByteArray(""));
    assertArrayEquals(new byte[] {}, Numeric.hexStringToByteArray("0x"));
  }

  // --- toBytesPadded ---

  @Test
  public void testToBytesPadded() {
    byte[] result = Numeric.toBytesPadded(BigInteger.valueOf(255), 4);
    assertArrayEquals(new byte[] {0, 0, 0, (byte) 0xff}, result);
  }

  @Test
  public void testToBytesPaddedWithLeadingZeroByte() {
    // BigInteger(128).toByteArray() = [0, -128] (leading zero for sign)
    byte[] result = Numeric.toBytesPadded(BigInteger.valueOf(128), 2);
    assertArrayEquals(new byte[] {0, (byte) 0x80}, result);
  }

  @Test(expected = RuntimeException.class)
  public void testToBytesPaddedTooSmall() {
    Numeric.toBytesPadded(BigInteger.valueOf(65536), 1);
  }

  // --- asByte ---

  @Test
  public void testAsByte() {
    assertEquals((byte) 0xAB, Numeric.asByte(0x0A, 0x0B));
    assertEquals((byte) 0x00, Numeric.asByte(0, 0));
    assertEquals((byte) 0xFF, Numeric.asByte(0x0F, 0x0F));
  }

  // --- isIntegerValue ---

  @Test
  public void testIsIntegerValueTrue() {
    assertTrue(Numeric.isIntegerValue(BigDecimal.ZERO));
    assertTrue(Numeric.isIntegerValue(new BigDecimal("10")));
    assertTrue(Numeric.isIntegerValue(new BigDecimal("10.00")));
    assertTrue(Numeric.isIntegerValue(new BigDecimal("1E+2")));
  }

  @Test
  public void testIsIntegerValueFalse() {
    assertFalse(Numeric.isIntegerValue(new BigDecimal("10.5")));
    assertFalse(Numeric.isIntegerValue(new BigDecimal("0.1")));
  }
}
