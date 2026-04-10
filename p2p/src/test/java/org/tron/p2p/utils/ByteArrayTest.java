package org.tron.p2p.utils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.dns.update.AwsClient;

public class ByteArrayTest {

  @Test
  public void testHexToString() {
    byte[] data = new byte[] {-128, -127, -1, 0, 1, 127};
    Assert.assertEquals("8081ff00017f", ByteArray.toHexString(data));
  }

  @Test
  public void testSubdomain() {
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com", "abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com.", "abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com", "abc.com."));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com.", "abc.com."));

    Assert.assertFalse(AwsClient.isSubdomain("a-sub.abc.com", "sub.abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain(".sub.abc.com", "sub.abc.com"));
  }

  // --- toHexString ---

  @Test
  public void testToHexStringNull() {
    Assert.assertEquals("", ByteArray.toHexString(null));
  }

  // --- fromHexString ---

  @Test
  public void testFromHexStringBasic() {
    byte[] result = ByteArray.fromHexString("abcd");
    Assert.assertArrayEquals(new byte[] {(byte) 0xab, (byte) 0xcd}, result);
  }

  @Test
  public void testFromHexStringWithPrefix() {
    byte[] result = ByteArray.fromHexString("0xabcd");
    Assert.assertArrayEquals(new byte[] {(byte) 0xab, (byte) 0xcd}, result);
  }

  @Test
  public void testFromHexStringOddLength() {
    byte[] result = ByteArray.fromHexString("abc");
    Assert.assertArrayEquals(new byte[] {0x0a, (byte) 0xbc}, result);
  }

  @Test
  public void testFromHexStringNull() {
    Assert.assertArrayEquals(ByteArray.EMPTY_BYTE_ARRAY, ByteArray.fromHexString(null));
  }

  // --- toLong ---

  @Test
  public void testToLong() {
    byte[] bytes = new byte[] {0x00, 0x01};
    Assert.assertEquals(1L, ByteArray.toLong(bytes));
  }

  @Test
  public void testToLongEmpty() {
    Assert.assertEquals(0L, ByteArray.toLong(new byte[0]));
    Assert.assertEquals(0L, ByteArray.toLong(null));
  }

  // --- toInt ---

  @Test
  public void testToInt() {
    byte[] bytes = new byte[] {0x00, (byte) 0xff};
    Assert.assertEquals(255, ByteArray.toInt(bytes));
  }

  @Test
  public void testToIntEmpty() {
    Assert.assertEquals(0, ByteArray.toInt(new byte[0]));
    Assert.assertEquals(0, ByteArray.toInt(null));
  }

  // --- fromString / toStr ---

  @Test
  public void testFromString() {
    Assert.assertArrayEquals("hello".getBytes(), ByteArray.fromString("hello"));
  }

  @Test
  public void testFromStringBlank() {
    Assert.assertNull(ByteArray.fromString(null));
    Assert.assertNull(ByteArray.fromString(""));
    Assert.assertNull(ByteArray.fromString("   "));
  }

  @Test
  public void testToStr() {
    Assert.assertEquals("abc", ByteArray.toStr("abc".getBytes()));
  }

  @Test
  public void testToStrEmpty() {
    Assert.assertNull(ByteArray.toStr(null));
    Assert.assertNull(ByteArray.toStr(new byte[0]));
  }

  // --- fromLong / fromInt ---

  @Test
  public void testFromLong() {
    byte[] result = ByteArray.fromLong(256L);
    Assert.assertEquals(256L, ByteArray.toLong(result));
  }

  @Test
  public void testFromInt() {
    byte[] result = ByteArray.fromInt(42);
    Assert.assertEquals(42, ByteArray.toInt(result));
  }

  // --- fromObject ---

  @Test
  public void testFromObjectSerializable() {
    byte[] result = ByteArray.fromObject("hello");
    Assert.assertNotNull(result);
    Assert.assertTrue(result.length > 0);
  }

  // --- toJsonHex ---

  @Test
  public void testToJsonHexBytes() {
    Assert.assertEquals("0x", ByteArray.toJsonHex(new byte[0]));
    Assert.assertEquals("0x", ByteArray.toJsonHex((byte[]) null));
    Assert.assertEquals("0xab", ByteArray.toJsonHex(new byte[] {(byte) 0xab}));
  }

  @Test
  public void testToJsonHexLong() {
    Assert.assertEquals("0xff", ByteArray.toJsonHex(255L));
    Assert.assertNull(ByteArray.toJsonHex((Long) null));
  }

  @Test
  public void testToJsonHexInt() {
    Assert.assertEquals("0x10", ByteArray.toJsonHex(16));
  }

  @Test
  public void testToJsonHexString() {
    Assert.assertEquals("0xabc", ByteArray.toJsonHex("abc"));
  }

  // --- hexToBigInteger ---

  @Test
  public void testHexToBigIntegerWithPrefix() {
    Assert.assertEquals(BigInteger.valueOf(255), ByteArray.hexToBigInteger("0xff"));
  }

  @Test
  public void testHexToBigIntegerDecimal() {
    Assert.assertEquals(BigInteger.valueOf(123), ByteArray.hexToBigInteger("123"));
  }

  // --- jsonHexToInt ---

  @Test
  public void testJsonHexToInt() throws Exception {
    Assert.assertEquals(255, ByteArray.jsonHexToInt("0xff"));
    Assert.assertEquals(0, ByteArray.jsonHexToInt("0x0"));
  }

  @Test(expected = Exception.class)
  public void testJsonHexToIntNoPrefix() throws Exception {
    ByteArray.jsonHexToInt("ff");
  }

  // --- subArray ---

  @Test
  public void testSubArray() {
    byte[] input = new byte[] {1, 2, 3, 4, 5};
    Assert.assertArrayEquals(new byte[] {2, 3, 4}, ByteArray.subArray(input, 1, 4));
  }

  // --- isEmpty ---

  @Test
  public void testIsEmpty() {
    Assert.assertTrue(ByteArray.isEmpty(null));
    Assert.assertTrue(ByteArray.isEmpty(new byte[0]));
    Assert.assertFalse(ByteArray.isEmpty(new byte[] {1}));
  }

  // --- matrixContains ---

  @Test
  public void testMatrixContains() {
    byte[] a = new byte[] {1, 2};
    byte[] b = new byte[] {3, 4};
    byte[] c = new byte[] {1, 2};
    Assert.assertTrue(ByteArray.matrixContains(Arrays.asList(a, b), c));
    Assert.assertFalse(ByteArray.matrixContains(
        Collections.singletonList(b), new byte[] {5, 6}));
  }

  // --- fromHex ---

  @Test
  public void testFromHex() {
    Assert.assertEquals("abcd", ByteArray.fromHex("0xabcd"));
    Assert.assertEquals("abcd", ByteArray.fromHex("abcd"));
    Assert.assertEquals("0abc", ByteArray.fromHex("abc"));
  }

  // --- byte2int ---

  @Test
  public void testByte2int() {
    Assert.assertEquals(0, ByteArray.byte2int((byte) 0));
    Assert.assertEquals(255, ByteArray.byte2int((byte) -1));
    Assert.assertEquals(128, ByteArray.byte2int((byte) -128));
    Assert.assertEquals(127, ByteArray.byte2int((byte) 127));
  }

  // --- constants ---

  @Test
  public void testConstants() {
    Assert.assertEquals(0, ByteArray.EMPTY_BYTE_ARRAY.length);
    Assert.assertArrayEquals(new byte[] {0}, ByteArray.ZERO_BYTE_ARRAY);
    Assert.assertEquals(32, ByteArray.WORD_SIZE);
  }
}
