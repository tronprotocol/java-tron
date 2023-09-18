/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.bytes;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

abstract class CommonBytesTests {

  abstract Bytes h(String hex);

  abstract MutableBytes m(int size);

  abstract Bytes w(byte[] bytes);

  abstract Bytes of(int... bytes);

  BigInteger bi(String decimal) {
    return new BigInteger(decimal);
  }

  @Test
  void asUnsignedBigInteger() {
    // Make sure things are interpreted unsigned.
    assertEquals(bi("255"), h("0xFF").toUnsignedBigInteger());

    // Try 2^100 + Long.MAX_VALUE, as an easy to define a big not too special big integer.
    BigInteger expected = BigInteger.valueOf(2).pow(100).add(BigInteger.valueOf(Long.MAX_VALUE));

    // 2^100 is a one followed by 100 zeros, that's 12 bytes of zeros (=96) plus 4 more zeros (so
    // 0x10 == 16).
    MutableBytes v = m(13);
    v.set(0, (byte) 16);
    v.setLong(v.size() - 8, Long.MAX_VALUE);
    assertEquals(expected, v.toUnsignedBigInteger());
  }

  @Test
  void testAsSignedBigInteger() {
    // Make sure things are interpreted signed.
    assertEquals(bi("-1"), h("0xFF").toBigInteger());

    // Try 2^100 + Long.MAX_VALUE, as an easy to define a big but not too special big integer.
    BigInteger expected = BigInteger.valueOf(2).pow(100).add(BigInteger.valueOf(Long.MAX_VALUE));

    // 2^100 is a one followed by 100 zeros, that's 12 bytes of zeros (=96) plus 4 more zeros (so
    // 0x10 == 16).
    MutableBytes v = m(13);
    v.set(0, (byte) 16);
    v.setLong(v.size() - 8, Long.MAX_VALUE);
    assertEquals(expected, v.toBigInteger());

    // And for a large negative one, we use -(2^100 + Long.MAX_VALUE), which is:
    //  2^100 + Long.MAX_VALUE = 0x10(4 bytes of 0)7F(  7 bytes of 1)
    //                 inverse = 0xEF(4 bytes of 1)80(  7 bytes of 0)
    //                      +1 = 0xEF(4 bytes of 1)80(6 bytes of 0)01
    expected = expected.negate();
    v = m(13);
    v.set(0, (byte) 0xEF);
    for (int i = 1; i < 5; i++) {
      v.set(i, (byte) 0xFF);
    }
    v.set(5, (byte) 0x80);
    // 6 bytes of 0
    v.set(12, (byte) 1);
    assertEquals(expected, v.toBigInteger());
  }

  @Test
  void testSize() {
    assertEquals(0, w(new byte[0]).size());
    assertEquals(1, w(new byte[1]).size());
    assertEquals(10, w(new byte[10]).size());
  }

  @Test
  void testGet() {
    Bytes v = w(new byte[] {1, 2, 3, 4});
    assertEquals((int) (byte) 1, (int) v.get(0));
    assertEquals((int) (byte) 2, (int) v.get(1));
    assertEquals((int) (byte) 3, (int) v.get(2));
    assertEquals((int) (byte) 4, (int) v.get(3));
  }

  @Test
  void testGetNegativeIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4}).get(-1));
  }

  @Test
  void testGetOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4}).get(4));
  }

  @Test
  void testGetInt() {
    Bytes value = w(new byte[] {0, 0, 1, 0, -1, -1, -1, -1});

    // 0x00000100 = 256
    assertEquals(256, value.getInt(0));
    // 0x000100FF = 65536 + 255 = 65791
    assertEquals(65791, value.getInt(1));
    // 0x0100FFFF = 16777216 (2^24) + (65536 - 1) = 16842751
    assertEquals(16842751, value.getInt(2));
    // 0xFFFFFFFF = -1
    assertEquals(-1, value.getInt(4));
  }

  @Test
  void testGetIntNegativeIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4}).getInt(-1));
  }

  @Test
  void testGetIntOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4}).getInt(4));
  }

  @Test
  void testGetIntNotEnoughBytes() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4}).getInt(1));
  }

  @Test
  void testAsInt() {
    assertEquals(0, Bytes.EMPTY.toInt());
    Bytes value1 = w(new byte[] {0, 0, 1, 0});
    // 0x00000100 = 256
    assertEquals(256, value1.toInt());
    assertEquals(256, value1.slice(2).toInt());

    Bytes value2 = w(new byte[] {0, 1, 0, -1});
    // 0x000100FF = 65536 + 255 = 65791
    assertEquals(65791, value2.toInt());
    assertEquals(65791, value2.slice(1).toInt());

    Bytes value3 = w(new byte[] {1, 0, -1, -1});
    // 0x0100FFFF = 16777216 (2^24) + (65536 - 1) = 16842751
    assertEquals(16842751, value3.toInt());

    Bytes value4 = w(new byte[] {-1, -1, -1, -1});
    // 0xFFFFFFFF = -1
    assertEquals(-1, value4.toInt());
  }

  @Test
  void testAsIntTooManyBytes() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> w(new byte[] {1, 2, 3, 4, 5}).toInt());
    assertEquals("Value of size 5 has more than 4 bytes", exception.getMessage());
  }

  @Test
  void testGetLong() {
    Bytes value1 = w(new byte[] {0, 0, 1, 0, -1, -1, -1, -1, 0, 0});
    // 0x00000100FFFFFFFF = (2^40) + (2^32) - 1 = 1103806595071
    assertEquals(1103806595071L, value1.getLong(0));
    // 0x 000100FFFFFFFF00 = (2^48) + (2^40) - 1 - 255 = 282574488338176
    assertEquals(282574488338176L, value1.getLong(1));

    Bytes value2 = w(new byte[] {-1, -1, -1, -1, -1, -1, -1, -1});
    assertEquals(-1L, value2.getLong(0));
  }

  @Test
  void testGetLongNegativeIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).getLong(-1));
  }

  @Test
  void testGetLongOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).getLong(8));
  }

  @Test
  void testGetLongNotEnoughBytes() {
    assertThrows(IndexOutOfBoundsException.class, () -> w(new byte[] {1, 2, 3, 4}).getLong(0));
  }

  @Test
  void testAsLong() {
    assertEquals(0, Bytes.EMPTY.toLong());
    Bytes value1 = w(new byte[] {0, 0, 1, 0, -1, -1, -1, -1});
    // 0x00000100FFFFFFFF = (2^40) + (2^32) - 1 = 1103806595071
    assertEquals(1103806595071L, value1.toLong());
    assertEquals(1103806595071L, value1.slice(2).toLong());
    Bytes value2 = w(new byte[] {0, 1, 0, -1, -1, -1, -1, 0});
    // 0x000100FFFFFFFF00 = (2^48) + (2^40) - 1 - 255 = 282574488338176
    assertEquals(282574488338176L, value2.toLong());
    assertEquals(282574488338176L, value2.slice(1).toLong());

    Bytes value3 = w(new byte[] {-1, -1, -1, -1, -1, -1, -1, -1});
    assertEquals(-1L, value3.toLong());
  }

  @Test
  void testAsLongTooManyBytes() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> w(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).toLong());
    assertEquals("Value of size 9 has more than 8 bytes", exception.getMessage());
  }

  @Test
  void testSlice() {
    assertEquals(h("0x"), h("0x0123456789").slice(0, 0));
    assertEquals(h("0x"), h("0x0123456789").slice(2, 0));
    assertEquals(h("0x01"), h("0x0123456789").slice(0, 1));
    assertEquals(h("0x0123"), h("0x0123456789").slice(0, 2));

    assertEquals(h("0x4567"), h("0x0123456789").slice(2, 2));
    assertEquals(h("0x23456789"), h("0x0123456789").slice(1, 4));
  }

  @Test
  void testSliceNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> h("0x012345").slice(-1, 2));
  }

  @Test
  void testSliceOffsetOutOfBound() {
    assertThrows(IndexOutOfBoundsException.class, () -> h("0x012345").slice(3, 2));
  }

  @Test
  void testSliceTooLong() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> h("0x012345").slice(1, 3));
    assertEquals(
        "Provided length 3 is too big: the value has size 3 and has only 2 bytes from 1",
        exception.getMessage());
  }

  @Test
  void testMutableCopy() {
    Bytes v = h("0x012345");
    MutableBytes mutableCopy = v.mutableCopy();

    // Initially, copy must be equal.
    assertEquals(mutableCopy, v);

    // Upon modification, original should not have been modified.
    mutableCopy.set(0, (byte) -1);
    assertNotEquals(mutableCopy, v);
    assertEquals(h("0x012345"), v);
    assertEquals(h("0xFF2345"), mutableCopy);
  }

  @Test
  void testCopyTo() {
    MutableBytes dest;

    // The follow does nothing, but simply making sure it doesn't throw.
    dest = MutableBytes.EMPTY;
    Bytes.EMPTY.copyTo(dest);
    assertEquals(Bytes.EMPTY, dest);

    dest = MutableBytes.create(1);
    of(1).copyTo(dest);
    assertEquals(h("0x01"), dest);

    dest = MutableBytes.create(1);
    of(10).copyTo(dest);
    assertEquals(h("0x0A"), dest);

    dest = MutableBytes.create(2);
    of(0xff, 0x03).copyTo(dest);
    assertEquals(h("0xFF03"), dest);

    dest = MutableBytes.create(4);
    of(0xff, 0x03).copyTo(dest.mutableSlice(1, 2));
    assertEquals(h("0x00FF0300"), dest);
  }

  @Test
  void testCopyToTooSmall() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(2)));
    assertEquals("Cannot copy 3 bytes to destination of non-equal size 2", exception.getMessage());
  }

  @Test
  void testCopyToTooBig() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(4)));
    assertEquals("Cannot copy 3 bytes to destination of non-equal size 4", exception.getMessage());
  }

  @Test
  void testCopyToWithOffset() {
    MutableBytes dest;

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    Bytes.EMPTY.copyTo(dest, 0);
    assertEquals(h("0x010203"), dest);

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    of(1).copyTo(dest, 1);
    assertEquals(h("0x010103"), dest);

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    of(2).copyTo(dest, 0);
    assertEquals(h("0x020203"), dest);

    dest = MutableBytes.wrap(new byte[] {1, 2, 3});
    of(1, 1).copyTo(dest, 1);
    assertEquals(h("0x010101"), dest);

    dest = MutableBytes.create(4);
    of(0xff, 0x03).copyTo(dest, 1);
    assertEquals(h("0x00FF0300"), dest);
  }

  @Test
  void testCopyToWithOffsetTooSmall() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(4), 2));
    assertEquals("Cannot copy 3 bytes, destination has only 2 bytes from index 2", exception.getMessage());
  }

  @Test
  void testCopyToWithNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(10), -1));
  }

  @Test
  void testCopyToWithOutOfBoundIndex() {
    assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).copyTo(MutableBytes.create(10), 10));
  }

  @Test
  void testAppendTo() {
    testAppendTo(Bytes.EMPTY, Buffer.buffer(), Bytes.EMPTY);
    testAppendTo(Bytes.EMPTY, Buffer.buffer(h("0x1234").toArrayUnsafe()), h("0x1234"));
    testAppendTo(h("0x1234"), Buffer.buffer(), h("0x1234"));
    testAppendTo(h("0x5678"), Buffer.buffer(h("0x1234").toArrayUnsafe()), h("0x12345678"));
  }

  private void testAppendTo(Bytes toAppend, Buffer buffer, Bytes expected) {
    toAppend.appendTo(buffer);
    assertEquals(expected, Bytes.wrap(buffer.getBytes()));
  }

  @Test
  void testIsZero() {
    assertTrue(Bytes.EMPTY.isZero());
    assertTrue(Bytes.of(0).isZero());
    assertTrue(Bytes.of(0, 0, 0).isZero());

    assertFalse(Bytes.of(1).isZero());
    assertFalse(Bytes.of(1, 0, 0).isZero());
    assertFalse(Bytes.of(0, 0, 1).isZero());
    assertFalse(Bytes.of(0, 0, 1, 0, 0).isZero());
  }

  @Test
  void testIsEmpty() {
    assertTrue(Bytes.EMPTY.isEmpty());

    assertFalse(Bytes.of(0).isEmpty());
    assertFalse(Bytes.of(0, 0, 0).isEmpty());
    assertFalse(Bytes.of(1).isEmpty());
  }

  @Test
  void findsCommonPrefix() {
    Bytes v = Bytes.of(1, 2, 3, 4, 5, 6, 7);
    Bytes o = Bytes.of(1, 2, 3, 4, 4, 3, 2);
    assertEquals(4, v.commonPrefixLength(o));
    assertEquals(Bytes.of(1, 2, 3, 4), v.commonPrefix(o));
  }

  @Test
  void findsCommonPrefixOfShorter() {
    Bytes v = Bytes.of(1, 2, 3, 4, 5, 6, 7);
    Bytes o = Bytes.of(1, 2, 3, 4);
    assertEquals(4, v.commonPrefixLength(o));
    assertEquals(Bytes.of(1, 2, 3, 4), v.commonPrefix(o));
  }

  @Test
  void findsCommonPrefixOfLonger() {
    Bytes v = Bytes.of(1, 2, 3, 4);
    Bytes o = Bytes.of(1, 2, 3, 4, 4, 3, 2);
    assertEquals(4, v.commonPrefixLength(o));
    assertEquals(Bytes.of(1, 2, 3, 4), v.commonPrefix(o));
  }

  @Test
  void findsCommonPrefixOfSliced() {
    Bytes v = Bytes.of(1, 2, 3, 4).slice(2, 2);
    Bytes o = Bytes.of(3, 4, 3, 3, 2).slice(3, 2);
    assertEquals(1, v.commonPrefixLength(o));
    assertEquals(Bytes.of(3), v.commonPrefix(o));
  }

  @Test
  void testTrimLeadingZeroes() {
    assertEquals(h("0x"), h("0x").trimLeadingZeros());
    assertEquals(h("0x"), h("0x00").trimLeadingZeros());
    assertEquals(h("0x"), h("0x00000000").trimLeadingZeros());

    assertEquals(h("0x01"), h("0x01").trimLeadingZeros());
    assertEquals(h("0x01"), h("0x00000001").trimLeadingZeros());

    assertEquals(h("0x3010"), h("0x3010").trimLeadingZeros());
    assertEquals(h("0x3010"), h("0x00003010").trimLeadingZeros());

    assertEquals(h("0xFFFFFFFF"), h("0xFFFFFFFF").trimLeadingZeros());
    assertEquals(h("0xFFFFFFFF"), h("0x000000000000FFFFFFFF").trimLeadingZeros());
  }

  @Test
  void testQuantityHexString() {
    assertEquals("0x0", h("0x").toQuantityHexString());
    assertEquals("0x0", h("0x0000").toQuantityHexString());
    assertEquals("0x1000001", h("0x01000001").toQuantityHexString());
  }

  @Test
  void testHexString() {
    assertEquals("0x", h("0x").toShortHexString());
    assertEquals("0x", h("0x0000").toShortHexString());
    assertEquals("0x1000001", h("0x01000001").toShortHexString());

    assertEquals("0000", h("0x0000").toUnprefixedHexString());
    assertEquals("1234", h("0x1234").toUnprefixedHexString());
    assertEquals("0022", h("0x0022").toUnprefixedHexString());
  }

  @Test
  void testEllipsisHexString() {
    assertEquals("0x", h("0x").toEllipsisHexString());
    assertEquals("0x0000", h("0x0000").toEllipsisHexString());
    assertEquals("0x01000001", h("0x01000001").toEllipsisHexString());
    assertEquals("0x0100000001", h("0x0100000001").toEllipsisHexString());
    assertEquals("0x0100..0001", h("0x010000000001").toEllipsisHexString());
    assertEquals("0x1234..5678", h("0x123456789abcdef012345678").toEllipsisHexString());
    assertEquals("0x1234..789a", h("0x123456789abcdef0123456789a").toEllipsisHexString());
    assertEquals("0x1234..9abc", h("0x123456789abcdef0123456789abc").toEllipsisHexString());
    assertEquals("0x1234..bcde", h("0x123456789abcdef0123456789abcde").toEllipsisHexString());
    assertEquals("0x1234..def0", h("0x123456789abcdef0123456789abcdef0").toEllipsisHexString());
    assertEquals("0x1234..def0", h("0x123456789abcdef0123456789abcdef0").toEllipsisHexString());
  }

  @Test
  void slideToEnd() {
    assertEquals(Bytes.of(1, 2, 3, 4), Bytes.of(1, 2, 3, 4).slice(0));
    assertEquals(Bytes.of(2, 3, 4), Bytes.of(1, 2, 3, 4).slice(1));
    assertEquals(Bytes.of(3, 4), Bytes.of(1, 2, 3, 4).slice(2));
    assertEquals(Bytes.of(4), Bytes.of(1, 2, 3, 4).slice(3));
  }

  @Test
  void slicePastEndReturnsEmpty() {
    assertEquals(Bytes.EMPTY, Bytes.of(1, 2, 3, 4).slice(4));
    assertEquals(Bytes.EMPTY, Bytes.of(1, 2, 3, 4).slice(5));
  }

  @Test
  void testUpdate() throws NoSuchAlgorithmException {
    // Digest the same byte array in 4 ways:
    //  1) directly from the array
    //  2) after wrapped using the update() method
    //  3) after wrapped and copied using the update() method
    //  4) after wrapped but getting the byte manually
    // and check all compute the same digest.
    MessageDigest md1 = MessageDigest.getInstance("SHA-1");
    MessageDigest md2 = MessageDigest.getInstance("SHA-1");
    MessageDigest md3 = MessageDigest.getInstance("SHA-1");
    MessageDigest md4 = MessageDigest.getInstance("SHA-1");

    byte[] toDigest = new BigInteger("12324029423415041783577517238472017314").toByteArray();
    Bytes wrapped = w(toDigest);

    byte[] digest1 = md1.digest(toDigest);

    wrapped.update(md2);
    byte[] digest2 = md2.digest();

    wrapped.copy().update(md3);
    byte[] digest3 = md3.digest();

    for (int i = 0; i < wrapped.size(); i++)
      md4.update(wrapped.get(i));
    byte[] digest4 = md4.digest();

    assertArrayEquals(digest2, digest1);
    assertArrayEquals(digest3, digest1);
    assertArrayEquals(digest4, digest1);
  }

  @Test
  void testArrayExtraction() {
    // extractArray() and getArrayUnsafe() have essentially the same contract...
    testArrayExtraction(Bytes::toArray);
    testArrayExtraction(Bytes::toArrayUnsafe);

    // But on top of the basic, extractArray() guarantees modifying the returned array is safe from
    // impacting the original value (not that getArrayUnsafe makes no guarantees here one way or
    // another, so there is nothing to test).
    byte[] orig = new byte[] {1, 2, 3, 4};
    Bytes value = w(orig);
    byte[] extracted = value.toArray();
    assertArrayEquals(orig, extracted);
    Arrays.fill(extracted, (byte) -1);
    assertArrayEquals(extracted, new byte[] {-1, -1, -1, -1});
    assertArrayEquals(orig, new byte[] {1, 2, 3, 4});
    assertEquals(of(1, 2, 3, 4), value);
  }

  private void testArrayExtraction(Function<Bytes, byte[]> extractor) {
    byte[] bytes = new byte[0];
    assertArrayEquals(extractor.apply(Bytes.EMPTY), bytes);

    byte[][] toTest = new byte[][] {new byte[] {1}, new byte[] {1, 2, 3, 4, 5, 6}, new byte[] {-1, -1, 0, -1}};
    for (byte[] array : toTest) {
      assertArrayEquals(extractor.apply(w(array)), array);
    }

    // Test slightly more complex interactions
    assertArrayEquals(extractor.apply(w(new byte[] {1, 2, 3, 4, 5}).slice(2, 2)), new byte[] {3, 4});
    assertArrayEquals(extractor.apply(w(new byte[] {1, 2, 3, 4, 5}).slice(2, 0)), new byte[] {});
  }

  @Test
  void testToString() {
    assertEquals("0x", Bytes.EMPTY.toString());

    assertEquals("0x01", of(1).toString());
    assertEquals("0x0aff03", of(0x0a, 0xff, 0x03).toString());
  }

  @Test
  void testHasLeadingZeroByte() {
    assertFalse(Bytes.fromHexString("0x").hasLeadingZeroByte());
    assertTrue(Bytes.fromHexString("0x0012").hasLeadingZeroByte());
    assertFalse(Bytes.fromHexString("0x120012").hasLeadingZeroByte());
  }

  @Test
  void testNumberOfLeadingZeroBytes() {
    assertEquals(0, Bytes.fromHexString("0x12").numberOfLeadingZeroBytes());
    assertEquals(1, Bytes.fromHexString("0x0012").numberOfLeadingZeroBytes());
    assertEquals(2, Bytes.fromHexString("0x000012").numberOfLeadingZeroBytes());
    assertEquals(0, Bytes.fromHexString("0x").numberOfLeadingZeroBytes());
    assertEquals(1, Bytes.fromHexString("0x00").numberOfLeadingZeroBytes());
    assertEquals(2, Bytes.fromHexString("0x0000").numberOfLeadingZeroBytes());
    assertEquals(3, Bytes.fromHexString("0x000000").numberOfLeadingZeroBytes());
  }

  @Test
  void testNumberOfTrailingZeroBytes() {
    assertEquals(0, Bytes.fromHexString("0x12").numberOfTrailingZeroBytes());
    assertEquals(1, Bytes.fromHexString("0x1200").numberOfTrailingZeroBytes());
    assertEquals(2, Bytes.fromHexString("0x120000").numberOfTrailingZeroBytes());
    assertEquals(0, Bytes.fromHexString("0x").numberOfTrailingZeroBytes());
    assertEquals(1, Bytes.fromHexString("0x00").numberOfTrailingZeroBytes());
    assertEquals(2, Bytes.fromHexString("0x0000").numberOfTrailingZeroBytes());
    assertEquals(3, Bytes.fromHexString("0x000000").numberOfTrailingZeroBytes());
  }

  @Test
  void testHasLeadingZeroBit() {
    assertFalse(Bytes.fromHexString("0x").hasLeadingZero());
    assertTrue(Bytes.fromHexString("0x01").hasLeadingZero());
    assertFalse(Bytes.fromHexString("0xFF0012").hasLeadingZero());
  }

  @Test
  void testEquals() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes b2 = w(key);
    assertEquals(b.hashCode(), b2.hashCode());
  }

  @Test
  void testEqualsWithOffset() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key).slice(16, 4);
    Bytes b2 = w(key).slice(16, 8).slice(0, 4);
    assertEquals(b, b2);
  }

  @Test
  void testHashCode() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes b2 = w(key);
    assertEquals(b.hashCode(), b2.hashCode());
  }

  @Test
  void testHashCodeWithOffset() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key).slice(16, 16);
    Bytes b2 = w(key).slice(16, 16);
    assertEquals(b.hashCode(), b2.hashCode());
  }

  @Test
  void testHashCodeWithByteBufferWrappingBytes() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes other = Bytes.wrapByteBuffer(ByteBuffer.wrap(key));
    assertEquals(b.hashCode(), other.hashCode());
  }

  @Test
  void testEqualsWithByteBufferWrappingBytes() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes other = Bytes.wrapByteBuffer(ByteBuffer.wrap(key));
    assertEquals(b, other);
  }

  @Test
  void testHashCodeWithBufferWrappingBytes() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes other = Bytes.wrapBuffer(Buffer.buffer(key));
    assertEquals(b.hashCode(), other.hashCode());
  }

  @Test
  void testEqualsWithBufferWrappingBytes() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes other = Bytes.wrapBuffer(Buffer.buffer(key));
    assertEquals(b, other);
  }

  @Test
  void testHashCodeWithByteBufWrappingBytes() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes other = Bytes.wrapByteBuf(Unpooled.copiedBuffer(key));
    assertEquals(b.hashCode(), other.hashCode());
  }

  @Test
  void testEqualsWithByteBufWrappingBytes() {
    SecureRandom random = new SecureRandom();
    byte[] key = new byte[32];
    random.nextBytes(key);
    Bytes b = w(key);
    Bytes other = Bytes.wrapByteBuf(Unpooled.copiedBuffer(key));
    assertEquals(b, other);
  }
}