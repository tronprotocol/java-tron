package org.web3j.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.exceptions.MessageDecodingException;
import org.web3j.exceptions.MessageEncodingException;

/**
 * Covers the message codec helpers vendored from web3j. Numeric backs Hash, Sign
 * and ECKeyPair, which the DNS tree signing path depends on.
 */
public class NumericTest {

  private static final byte[] HEX_RANGE_BYTES = new byte[] {0x12, 0x34, 0x56, 0x78};

  @Test
  public void encodeQuantity() {
    Assert.assertEquals("0x0", Numeric.encodeQuantity(BigInteger.ZERO));
    Assert.assertEquals("0x1", Numeric.encodeQuantity(BigInteger.ONE));
    Assert.assertEquals("0x1f4", Numeric.encodeQuantity(BigInteger.valueOf(500)));
    Assert.assertEquals("0x9184e72a000",
        Numeric.encodeQuantity(new BigInteger("10000000000000")));
  }

  @Test(expected = MessageEncodingException.class)
  public void encodeQuantityRejectsNegative() {
    Numeric.encodeQuantity(BigInteger.valueOf(-1));
  }

  @Test
  public void decodeQuantity() {
    Assert.assertEquals(BigInteger.ZERO, Numeric.decodeQuantity("0x0"));
    Assert.assertEquals(BigInteger.valueOf(500), Numeric.decodeQuantity("0x1f4"));
    Assert.assertEquals(new BigInteger("10000000000000"),
        Numeric.decodeQuantity("0x9184e72a000"));
    // A bare decimal string is accepted through the isLongValue path.
    Assert.assertEquals(BigInteger.valueOf(123), Numeric.decodeQuantity("123"));
    Assert.assertEquals(BigInteger.valueOf(-1), Numeric.decodeQuantity("-1"));
  }

  @Test(expected = MessageDecodingException.class)
  public void decodeQuantityRejectsNull() {
    Numeric.decodeQuantity(null);
  }

  @Test(expected = MessageDecodingException.class)
  public void decodeQuantityRejectsTooShort() {
    Numeric.decodeQuantity("0x");
  }

  @Test(expected = MessageDecodingException.class)
  public void decodeQuantityRejectsMissingPrefix() {
    Numeric.decodeQuantity("ff");
  }

  @Test(expected = MessageDecodingException.class)
  public void decodeQuantityRejectsNonHexAfterPrefix() {
    Numeric.decodeQuantity("0xzz");
  }

  @Test
  public void hexPrefixHandling() {
    Assert.assertTrue(Numeric.containsHexPrefix("0xff"));
    Assert.assertFalse(Numeric.containsHexPrefix("ff"));
    Assert.assertFalse(Numeric.containsHexPrefix(""));
    Assert.assertFalse(Numeric.containsHexPrefix(null));
    Assert.assertFalse(Numeric.containsHexPrefix("0"));

    Assert.assertEquals("ff", Numeric.cleanHexPrefix("0xff"));
    Assert.assertEquals("ff", Numeric.cleanHexPrefix("ff"));
    Assert.assertEquals("0xff", Numeric.prependHexPrefix("ff"));
    Assert.assertEquals("0xff", Numeric.prependHexPrefix("0xff"));
  }

  @Test
  public void toBigIntConversions() {
    Assert.assertEquals(BigInteger.valueOf(0x1234), Numeric.toBigInt("0x1234"));
    Assert.assertEquals(BigInteger.valueOf(0x1234), Numeric.toBigInt("1234"));
    Assert.assertEquals(BigInteger.valueOf(0x1234), Numeric.toBigIntNoPrefix("1234"));
    Assert.assertEquals(new BigInteger("12345678", 16), Numeric.toBigInt(HEX_RANGE_BYTES));
    Assert.assertEquals(BigInteger.valueOf(0x3456),
        Numeric.toBigInt(HEX_RANGE_BYTES, 1, 2));
    // Always treated as unsigned: a leading 0xFF is not -1.
    Assert.assertEquals(BigInteger.valueOf(255), Numeric.toBigInt(new byte[] {(byte) 0xFF}));
  }

  @Test
  public void toHexStringVariants() {
    BigInteger value = BigInteger.valueOf(0x1f4);
    Assert.assertEquals("0x1f4", Numeric.toHexStringWithPrefix(value));
    Assert.assertEquals("1f4", Numeric.toHexStringNoPrefix(value));
    Assert.assertEquals("0x12345678", Numeric.toHexString(HEX_RANGE_BYTES));
    Assert.assertEquals("12345678", Numeric.toHexStringNoPrefix(HEX_RANGE_BYTES));
    Assert.assertEquals("3456", Numeric.toHexString(HEX_RANGE_BYTES, 1, 2, false));
    Assert.assertEquals("0x3456", Numeric.toHexString(HEX_RANGE_BYTES, 1, 2, true));
  }

  @Test
  public void toHexStringWithPrefixSafePadsSingleDigit() {
    Assert.assertEquals("0x01", Numeric.toHexStringWithPrefixSafe(BigInteger.ONE));
    Assert.assertEquals("0x1f4", Numeric.toHexStringWithPrefixSafe(BigInteger.valueOf(500)));
  }

  @Test
  public void toHexStringZeroPadded() {
    Assert.assertEquals("0x0001f4",
        Numeric.toHexStringWithPrefixZeroPadded(BigInteger.valueOf(500), 6));
    Assert.assertEquals("0001f4",
        Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(500), 6));
    // Exact fit needs no padding.
    Assert.assertEquals("1f4", Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(500), 3));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void zeroPaddedRejectsOversizedValue() {
    Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(0x1f4), 2);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void zeroPaddedRejectsNegative() {
    Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(-1), 20);
  }

  @Test
  public void toBytesPadded() {
    Assert.assertArrayEquals(new byte[] {0, 0, 0x01, (byte) 0xf4},
        Numeric.toBytesPadded(BigInteger.valueOf(500), 4));
    // A value whose two's-complement form carries a leading zero byte has it dropped.
    Assert.assertArrayEquals(new byte[] {0, (byte) 0xFF},
        Numeric.toBytesPadded(BigInteger.valueOf(255), 2));
  }

  @Test(expected = RuntimeException.class)
  public void toBytesPaddedRejectsOversized() {
    Numeric.toBytesPadded(BigInteger.valueOf(0x1f4), 1);
  }

  @Test
  public void hexStringToByteArray() {
    Assert.assertArrayEquals(new byte[] {}, Numeric.hexStringToByteArray(""));
    Assert.assertArrayEquals(HEX_RANGE_BYTES, Numeric.hexStringToByteArray("0x12345678"));
    Assert.assertArrayEquals(HEX_RANGE_BYTES, Numeric.hexStringToByteArray("12345678"));
    // Odd length is left-padded with a nibble rather than rejected.
    Assert.assertArrayEquals(new byte[] {0x01, 0x23}, Numeric.hexStringToByteArray("123"));
  }

  @Test
  public void asByte() {
    Assert.assertEquals((byte) 0x00, Numeric.asByte(0x0, 0x0));
    Assert.assertEquals((byte) 0x12, Numeric.asByte(0x1, 0x2));
    Assert.assertEquals((byte) 0xff, Numeric.asByte(0xf, 0xf));
  }

  @Test
  public void isIntegerValue() {
    Assert.assertTrue(Numeric.isIntegerValue(BigDecimal.ZERO));
    Assert.assertTrue(Numeric.isIntegerValue(BigDecimal.valueOf(5)));
    Assert.assertTrue(Numeric.isIntegerValue(new BigDecimal("5.0")));
    Assert.assertFalse(Numeric.isIntegerValue(new BigDecimal("5.5")));
  }
}
