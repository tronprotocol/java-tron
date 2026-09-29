package org.tron.p2p.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
  public void testToHexStringNull() {
    Assert.assertEquals("", ByteArray.toHexString(null));
  }

  @Test
  public void testFromHexString() {
    // null yields the shared empty array rather than null
    Assert.assertEquals(0, ByteArray.fromHexString(null).length);
    // a 0x prefix is stripped
    Assert.assertArrayEquals(new byte[] {10}, ByteArray.fromHexString("0x0a"));
    // an odd-length string is left-padded with a zero nibble
    Assert.assertArrayEquals(new byte[] {10}, ByteArray.fromHexString("a"));
    Assert.assertArrayEquals(new byte[] {-1}, ByteArray.fromHexString("ff"));
    // both together
    Assert.assertArrayEquals(new byte[] {10}, ByteArray.fromHexString("0xa"));
  }

  @Test
  public void testToLongAndToInt() {
    Assert.assertEquals(0, ByteArray.toLong(null));
    Assert.assertEquals(0, ByteArray.toLong(new byte[0]));
    // unsigned big-endian: 0x0100 == 256
    Assert.assertEquals(256L, ByteArray.toLong(new byte[] {1, 0}));
    // 0xff is read unsigned, not as -1
    Assert.assertEquals(255L, ByteArray.toLong(new byte[] {-1}));

    Assert.assertEquals(0, ByteArray.toInt(null));
    Assert.assertEquals(0, ByteArray.toInt(new byte[0]));
    Assert.assertEquals(256, ByteArray.toInt(new byte[] {1, 0}));
  }

  @Test
  public void testFromStringAndToStr() {
    // blank input (including whitespace-only) maps to null
    Assert.assertNull(ByteArray.fromString(null));
    Assert.assertNull(ByteArray.fromString(""));
    Assert.assertNull(ByteArray.fromString("  "));
    Assert.assertArrayEquals(new byte[] {97, 98}, ByteArray.fromString("ab"));

    Assert.assertNull(ByteArray.toStr(null));
    Assert.assertNull(ByteArray.toStr(new byte[0]));
    Assert.assertEquals("ab", ByteArray.toStr(new byte[] {97, 98}));
  }

  @Test
  public void testFromLongAndFromInt() {
    // big-endian, fixed width: 8 bytes for long, 4 for int
    Assert.assertArrayEquals(new byte[] {0, 0, 0, 0, 0, 0, 0, 1}, ByteArray.fromLong(1L));
    Assert.assertArrayEquals(new byte[] {0, 0, 0, 1}, ByteArray.fromInt(1));
    Assert.assertArrayEquals(new byte[] {0, 0, 1, 0}, ByteArray.fromInt(256));
  }

  @Test
  public void testToJsonHex() {
    Assert.assertEquals("0x", ByteArray.toJsonHex((byte[]) null));
    Assert.assertEquals("0x", ByteArray.toJsonHex(new byte[0]));
    Assert.assertEquals("0x0a", ByteArray.toJsonHex(new byte[] {10}));

    Assert.assertNull(ByteArray.toJsonHex((Long) null));
    Assert.assertEquals("0xff", ByteArray.toJsonHex(Long.valueOf(255L)));
    Assert.assertEquals("0xff", ByteArray.toJsonHex(255));
    Assert.assertEquals("0xabc", ByteArray.toJsonHex("abc"));
  }

  @Test
  public void testHexToBigInteger() {
    // a 0x prefix selects base 16, its absence selects base 10
    Assert.assertEquals(new BigInteger("255"), ByteArray.hexToBigInteger("0xff"));
    Assert.assertEquals(new BigInteger("255"), ByteArray.hexToBigInteger("255"));
  }

  @Test
  public void testJsonHexToInt() throws Exception {
    Assert.assertEquals(255, ByteArray.jsonHexToInt("0xff"));
    try {
      ByteArray.jsonHexToInt("ff");
      Assert.fail("expected a missing 0x prefix to be rejected");
    } catch (Exception e) {
      Assert.assertEquals("Incorrect hex syntax", e.getMessage());
    }
  }

  @Test
  public void testSubArray() {
    byte[] input = new byte[] {1, 2, 3, 4};
    // end is exclusive
    Assert.assertArrayEquals(new byte[] {2, 3}, ByteArray.subArray(input, 1, 3));
    Assert.assertArrayEquals(new byte[0], ByteArray.subArray(input, 2, 2));
    Assert.assertArrayEquals(input, ByteArray.subArray(input, 0, 4));
  }

  @Test
  public void testIsEmpty() {
    Assert.assertTrue(ByteArray.isEmpty(null));
    Assert.assertTrue(ByteArray.isEmpty(new byte[0]));
    Assert.assertFalse(ByteArray.isEmpty(new byte[] {0}));
  }

  @Test
  public void testMatrixContains() {
    List<byte[]> source = new ArrayList<>();
    source.add(new byte[] {1, 2});
    source.add(new byte[] {3});
    // compares by content, not identity
    Assert.assertTrue(ByteArray.matrixContains(source, new byte[] {1, 2}));
    Assert.assertTrue(ByteArray.matrixContains(source, new byte[] {3}));
    Assert.assertFalse(ByteArray.matrixContains(source, new byte[] {2, 1}));
    Assert.assertFalse(ByteArray.matrixContains(new ArrayList<>(), new byte[] {1}));
  }

  @Test
  public void testFromHex() {
    Assert.assertEquals("ab", ByteArray.fromHex("ab"));
    Assert.assertEquals("ab", ByteArray.fromHex("0xab"));
    // odd length is left-padded after the prefix is stripped
    Assert.assertEquals("0abc", ByteArray.fromHex("0xabc"));
    Assert.assertEquals("0a", ByteArray.fromHex("a"));
  }

  @Test
  public void testByte2int() {
    // reads the byte as unsigned
    Assert.assertEquals(255, ByteArray.byte2int((byte) -1));
    Assert.assertEquals(127, ByteArray.byte2int((byte) 127));
    Assert.assertEquals(0, ByteArray.byte2int((byte) 0));
    Assert.assertEquals(128, ByteArray.byte2int((byte) -128));
  }

  @Test
  public void testFromObject() {
    // a Serializable round-trips to a non-empty stream; two equal inputs
    // serialise identically
    byte[] bytes = ByteArray.fromObject("test");
    Assert.assertNotNull(bytes);
    Assert.assertTrue(bytes.length > 0);
    Assert.assertArrayEquals(bytes, ByteArray.fromObject("test"));
  }

  @Test
  public void testSubdomain() {
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com","abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com.","abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com","abc.com."));
    Assert.assertTrue(AwsClient.isSubdomain("cde.abc.com.","abc.com."));

    Assert.assertFalse(AwsClient.isSubdomain("a-sub.abc.com","sub.abc.com"));
    Assert.assertTrue(AwsClient.isSubdomain(".sub.abc.com","sub.abc.com"));
  }
}
