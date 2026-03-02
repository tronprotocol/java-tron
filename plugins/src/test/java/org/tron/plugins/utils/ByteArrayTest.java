package org.tron.plugins.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class ByteArrayTest {

  @Test
  public void testToStrToInt() {
    String test = "abc";
    byte[] testBytes = test.getBytes();
    Assert.assertEquals(test, ByteArray.toStr(testBytes));

    int i = 5;
    Assert.assertEquals(ByteArray.toInt(ByteArray.fromInt(i)), 5);
  }

  @Test
  public void testFromHexString() {
    Assert.assertArrayEquals(ByteArray.EMPTY_BYTE_ARRAY, ByteArray.fromHexString(null));

    Assert.assertArrayEquals(ByteArray.fromHexString("12"), ByteArray.fromHexString("0x12"));

    Assert.assertArrayEquals(ByteArray.fromHexString("0x2"), ByteArray.fromHexString("0x02"));
  }

  @Test
  public void testCompareUnsigned() {
    byte[] a = new byte[] {1, 2};
    Assert.assertEquals(0, ByteArray.compareUnsigned(a, a));
    Assert.assertEquals(-1, ByteArray.compareUnsigned(null, a));
    Assert.assertEquals(1, ByteArray.compareUnsigned(a, null));

    byte[] b = new byte[] {1, 3};
    Assert.assertEquals(-1, ByteArray.compareUnsigned(a, b));
    Assert.assertEquals(1, ByteArray.compareUnsigned(b, a));

    byte[] c = new byte[] {1, 2, 3};
    Assert.assertEquals(-1, ByteArray.compareUnsigned(a, c));
    Assert.assertEquals(1, ByteArray.compareUnsigned(c, a));
  }
}
