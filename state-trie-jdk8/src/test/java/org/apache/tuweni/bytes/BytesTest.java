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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.*;

class BytesTest extends CommonBytesTests {

  @Override
  Bytes h(String hex) {
    return Bytes.fromHexString(hex);
  }

  @Override
  MutableBytes m(int size) {
    return MutableBytes.create(size);
  }

  @Override
  Bytes w(byte[] bytes) {
    return Bytes.wrap(bytes);
  }

  @Override
  Bytes of(int... bytes) {
    return Bytes.of(bytes);
  }

  @Test
  void wrapEmpty() {
    Bytes wrap = Bytes.wrap(new byte[0]);
    assertEquals(Bytes.EMPTY, wrap);
  }

  @ParameterizedTest
  @MethodSource("wrapProvider")
  void wrap(Object arr) {
    byte[] bytes = (byte[]) arr;
    Bytes value = Bytes.wrap(bytes);
    assertEquals(bytes.length, value.size());
    assertArrayEquals(value.toArray(), bytes);
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> wrapProvider() {
    return Stream
        .of(
            Arguments.of(new Object[] {new byte[10]}),
            Arguments.of(new Object[] {new byte[] {1}}),
            Arguments.of(new Object[] {new byte[] {1, 2, 3, 4}}),
            Arguments.of(new Object[] {new byte[] {-1, 127, -128}}));
  }

  @Test
  void wrapNull() {
    assertThrows(NullPointerException.class, () -> Bytes.wrap((byte[]) null));
  }

  /**
   * Checks that modifying a wrapped array modifies the value itself.
   */
  @Test
  void wrapReflectsUpdates() {
    byte[] bytes = new byte[] {1, 2, 3, 4, 5};
    Bytes value = Bytes.wrap(bytes);

    assertEquals(bytes.length, value.size());
    assertArrayEquals(value.toArray(), bytes);

    bytes[1] = 127;
    bytes[3] = 127;

    assertEquals(bytes.length, value.size());
    assertArrayEquals(value.toArray(), bytes);
  }

  @Test
  void wrapSliceEmpty() {
    assertEquals(Bytes.EMPTY, Bytes.wrap(new byte[0], 0, 0));
    assertEquals(Bytes.EMPTY, Bytes.wrap(new byte[] {1, 2, 3}, 0, 0));
    assertEquals(Bytes.EMPTY, Bytes.wrap(new byte[] {1, 2, 3}, 2, 0));
  }

  @ParameterizedTest
  @MethodSource("wrapSliceProvider")
  void wrapSlice(Object arr, int offset, int length) {
    assertWrapSlice((byte[]) arr, offset, length);
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> wrapSliceProvider() {
    return Stream
        .of(
            Arguments.of(new byte[] {1, 2, 3, 4}, 0, 4),
            Arguments.of(new byte[] {1, 2, 3, 4}, 0, 2),
            Arguments.of(new byte[] {1, 2, 3, 4}, 2, 1),
            Arguments.of(new byte[] {1, 2, 3, 4}, 2, 2));
  }

  private void assertWrapSlice(byte[] bytes, int offset, int length) {
    Bytes value = Bytes.wrap(bytes, offset, length);
    assertEquals(length, value.size());
    assertArrayEquals(value.toArray(), Arrays.copyOfRange(bytes, offset, offset + length));
  }

  @Test
  void wrapSliceNull() {
    assertThrows(NullPointerException.class, () -> Bytes.wrap(null, 0, 2));
  }

  @Test
  void wrapSliceNegativeOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> assertWrapSlice(new byte[] {1, 2, 3, 4}, -1, 4));
  }

  @Test
  void wrapSliceOutOfBoundOffset() {
    assertThrows(IndexOutOfBoundsException.class, () -> assertWrapSlice(new byte[] {1, 2, 3, 4}, 5, 1));
  }

  @Test
  void wrapSliceNegativeLength() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> assertWrapSlice(new byte[] {1, 2, 3, 4}, 0, -2));
    assertEquals("Invalid negative length", exception.getMessage());
  }

  @Test
  void wrapSliceTooBig() {
    Throwable exception =
        assertThrows(IllegalArgumentException.class, () -> assertWrapSlice(new byte[] {1, 2, 3, 4}, 2, 3));
    assertEquals("Provided length 3 is too big: the value has only 2 bytes from offset 2", exception.getMessage());
  }

  /**
   * Checks that modifying a wrapped array modifies the value itself, but only if within the wrapped slice.
   */
  @Test
  void wrapSliceReflectsUpdates() {
    byte[] bytes = new byte[] {1, 2, 3, 4, 5};
    assertWrapSlice(bytes, 2, 2);
    bytes[2] = 127;
    bytes[3] = 127;
    assertWrapSlice(bytes, 2, 2);

    Bytes wrapped = Bytes.wrap(bytes, 2, 2);
    Bytes copy = wrapped.copy();

    // Modify the bytes outside of the wrapped slice and check this doesn't affect the value (that
    // it is still equal to the copy from before the updates)
    bytes[0] = 127;
    assertEquals(copy, wrapped);

    // Sanity check for copy(): modify within the wrapped slice and check the copy differs now.
    bytes[2] = 42;
    assertEquals("0x2a7f", wrapped.toHexString());
    assertEquals(Bytes.fromHexString("0x7f7f"), copy);
  }

  @Test
  void ofBytes() {
    assertArrayEquals(Bytes.of().toArray(), new byte[] {});
    assertArrayEquals(Bytes.of((byte) 1, (byte) 2).toArray(), new byte[] {1, 2});
    assertArrayEquals(Bytes.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).toArray(), new byte[] {1, 2, 3, 4, 5});
    assertArrayEquals(Bytes.of((byte) -1, (byte) 2, (byte) -3).toArray(), new byte[] {-1, 2, -3});
  }

  @Test
  void ofInts() {
    assertArrayEquals(Bytes.of(1, 2).toArray(), new byte[] {1, 2});
    assertArrayEquals(Bytes.of(1, 2, 3, 4, 5).toArray(), new byte[] {1, 2, 3, 4, 5});
    assertArrayEquals(Bytes.of(0xff, 0x7f, 0x80).toArray(), new byte[] {-1, 127, -128});
  }

  @Test
  void ofIntsTooBig() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.of(2, 3, 256));
    assertEquals("3th value 256 does not fit a byte", exception.getMessage());
  }

  @Test
  void ofIntsTooLow() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.of(2, -1, 3));
    assertEquals("2th value -1 does not fit a byte", exception.getMessage());
  }

  @Test
  void minimalBytes() {
    assertEquals(h("0x"), Bytes.minimalBytes(0));
    assertEquals(h("0x01"), Bytes.minimalBytes(1));
    assertEquals(h("0x04"), Bytes.minimalBytes(4));
    assertEquals(h("0x10"), Bytes.minimalBytes(16));
    assertEquals(h("0xFF"), Bytes.minimalBytes(255));
    assertEquals(h("0x0100"), Bytes.minimalBytes(256));
    assertEquals(h("0x0200"), Bytes.minimalBytes(512));
    assertEquals(h("0x010000"), Bytes.minimalBytes(1L << 16));
    assertEquals(h("0x01000000"), Bytes.minimalBytes(1L << 24));
    assertEquals(h("0x0100000000"), Bytes.minimalBytes(1L << 32));
    assertEquals(h("0x010000000000"), Bytes.minimalBytes(1L << 40));
    assertEquals(h("0x01000000000000"), Bytes.minimalBytes(1L << 48));
    assertEquals(h("0x0100000000000000"), Bytes.minimalBytes(1L << 56));
    assertEquals(h("0xFFFFFFFFFFFFFFFF"), Bytes.minimalBytes(-1L));
  }

  @Test
  void ofUnsignedShort() {
    assertEquals(h("0x0000"), Bytes.ofUnsignedShort(0));
    assertEquals(h("0x0001"), Bytes.ofUnsignedShort(1));
    assertEquals(h("0x0100"), Bytes.ofUnsignedShort(256));
    assertEquals(h("0xFFFF"), Bytes.ofUnsignedShort(65535));
  }

  @Test
  void ofUnsignedShortLittleEndian() {
    assertEquals(h("0x0000"), Bytes.ofUnsignedShort(0, LITTLE_ENDIAN));
    assertEquals(h("0x0100"), Bytes.ofUnsignedShort(1, LITTLE_ENDIAN));
    assertEquals(h("0x0001"), Bytes.ofUnsignedShort(256, LITTLE_ENDIAN));
    assertEquals(h("0xFFFF"), Bytes.ofUnsignedShort(65535, LITTLE_ENDIAN));
  }

  @Test
  void ofUnsignedShortNegative() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.ofUnsignedShort(-1));
    assertEquals(
        "Value -1 cannot be represented as an unsigned short (it is negative or too big)",
        exception.getMessage());
  }

  @Test
  void ofUnsignedShortTooBig() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.ofUnsignedShort(65536));
    assertEquals(
        "Value 65536 cannot be represented as an unsigned short (it is negative or too big)",
        exception.getMessage());
  }

  @Test
  void asUnsignedBigIntegerConstants() {
    assertEquals(bi("0"), Bytes.EMPTY.toUnsignedBigInteger());
    assertEquals(bi("1"), Bytes.of(1).toUnsignedBigInteger());
  }

  @Test
  void asSignedBigIntegerConstants() {
    assertEquals(bi("0"), Bytes.EMPTY.toBigInteger());
    assertEquals(bi("1"), Bytes.of(1).toBigInteger());
  }

  @Test
  void fromHexStringLenient() {
    assertEquals(Bytes.of(), Bytes.fromHexStringLenient(""));
    assertEquals(Bytes.of(), Bytes.fromHexStringLenient("0x"));
    assertEquals(Bytes.of(0), Bytes.fromHexStringLenient("0"));
    assertEquals(Bytes.of(0), Bytes.fromHexStringLenient("0x0"));
    assertEquals(Bytes.of(0), Bytes.fromHexStringLenient("00"));
    assertEquals(Bytes.of(0), Bytes.fromHexStringLenient("0x00"));
    assertEquals(Bytes.of(1), Bytes.fromHexStringLenient("0x1"));
    assertEquals(Bytes.of(1), Bytes.fromHexStringLenient("0x01"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("1FF2A"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x1FF2A"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x1ff2a"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x1fF2a"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("01FF2A"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x01FF2A"));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x01ff2A"));
  }

  @Test
  void compareTo() {
    assertEquals(1, Bytes.of(0x05).compareTo(Bytes.of(0x01)));
    assertEquals(1, Bytes.of(0x05).compareTo(Bytes.of(0x01)));
    assertEquals(1, Bytes.of(0xef).compareTo(Bytes.of(0x01)));
    assertEquals(1, Bytes.of(0xef).compareTo(Bytes.of(0x00, 0x01)));
    assertEquals(1, Bytes.of(0x00, 0x00, 0xef).compareTo(Bytes.of(0x00, 0x01)));
    assertEquals(1, Bytes.of(0x00, 0xef).compareTo(Bytes.of(0x00, 0x00, 0x01)));
    assertEquals(1, Bytes.of(0xef, 0xf0).compareTo(Bytes.of(0xff)));
    assertEquals(1, Bytes.of(0xef, 0xf0).compareTo(Bytes.of(0x01)));
    assertEquals(1, Bytes.of(0xef, 0xf1).compareTo(Bytes.of(0xef, 0xf0)));
    assertEquals(1, Bytes.of(0x00, 0x00, 0x01).compareTo(Bytes.of(0x00, 0x00)));
    assertEquals(0, Bytes.of(0xef, 0xf0).compareTo(Bytes.of(0xef, 0xf0)));
    assertEquals(-1, Bytes.of(0xef, 0xf0).compareTo(Bytes.of(0xef, 0xf5)));
    assertEquals(-1, Bytes.of(0xef).compareTo(Bytes.of(0xff)));
    assertEquals(-1, Bytes.of(0x01).compareTo(Bytes.of(0xff)));
    assertEquals(-1, Bytes.of(0x01).compareTo(Bytes.of(0x01, 0xff)));
    assertEquals(-1, Bytes.of(0x00, 0x00, 0x01).compareTo(Bytes.of(0x00, 0x02)));
    assertEquals(-1, Bytes.of(0x00, 0x01).compareTo(Bytes.of(0x00, 0x00, 0x05)));
    assertEquals(0, Bytes.fromHexString("0x0000").compareTo(Bytes.fromHexString("0x00")));
    assertEquals(0, Bytes.fromHexString("0x00").compareTo(Bytes.fromHexString("0x0000")));
    assertEquals(0, Bytes.fromHexString("0x000000").compareTo(Bytes.fromHexString("0x000000")));
    assertEquals(-1, Bytes.fromHexString("0x000001").compareTo(Bytes.fromHexString("0x0001")));
  }

  @Test
  void fromHexStringLenientInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexStringLenient("foo"));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation", exception.getMessage());
  }

  @Test
  void fromHexStringLenientLeftPadding() {
    assertEquals(Bytes.of(), Bytes.fromHexStringLenient("", 0));
    assertEquals(Bytes.of(0), Bytes.fromHexStringLenient("", 1));
    assertEquals(Bytes.of(0, 0), Bytes.fromHexStringLenient("", 2));
    assertEquals(Bytes.of(0, 0), Bytes.fromHexStringLenient("0x", 2));
    assertEquals(Bytes.of(0, 0, 0), Bytes.fromHexStringLenient("0", 3));
    assertEquals(Bytes.of(0, 0, 0), Bytes.fromHexStringLenient("0x0", 3));
    assertEquals(Bytes.of(0, 0, 0), Bytes.fromHexStringLenient("00", 3));
    assertEquals(Bytes.of(0, 0, 0), Bytes.fromHexStringLenient("0x00", 3));
    assertEquals(Bytes.of(0, 0, 1), Bytes.fromHexStringLenient("0x1", 3));
    assertEquals(Bytes.of(0, 0, 1), Bytes.fromHexStringLenient("0x01", 3));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("1FF2A", 3));
    assertEquals(Bytes.of(0x00, 0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x1FF2A", 4));
    assertEquals(Bytes.of(0x00, 0x00, 0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x1ff2a", 5));
    assertEquals(Bytes.of(0x00, 0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x1fF2a", 4));
    assertEquals(Bytes.of(0x00, 0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("01FF2A", 4));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x01FF2A", 3));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexStringLenient("0x01ff2A", 3));
  }

  @Test
  void fromHexStringLenientLeftPaddingInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexStringLenient("foo", 10));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation", exception.getMessage());
  }

  @Test
  void fromHexStringLenientLeftPaddingInvalidSize() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexStringLenient("0x001F34", 2));
    assertEquals("Hex value is too large: expected at most 2 bytes but got 3", exception.getMessage());
  }

  @Test
  void fromHexString() {
    assertEquals(Bytes.of(), Bytes.fromHexString("0x"));
    assertEquals(Bytes.of(0), Bytes.fromHexString("00"));
    assertEquals(Bytes.of(0), Bytes.fromHexString("0x00"));
    assertEquals(Bytes.of(1), Bytes.fromHexString("0x01"));
    assertEquals(Bytes.of(1, 0xff, 0x2a), Bytes.fromHexString("01FF2A"));
    assertEquals(Bytes.of(1, 0xff, 0x2a), Bytes.fromHexString("0x01FF2A"));
    assertEquals(Bytes.of(1, 0xff, 0x2a), Bytes.fromHexString("0x01ff2a"));
    assertEquals(Bytes.of(1, 0xff, 0x2a), Bytes.fromHexString("0x01fF2a"));
  }

  @Test
  void fromHexStringInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexString("fooo"));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation", exception.getMessage());
  }

  @Test
  void fromHexStringNotLenient() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexString("0x100"));
    assertEquals("Invalid odd-length hex binary representation", exception.getMessage());
  }

  @Test
  void fromHexStringLeftPadding() {
    assertEquals(Bytes.of(), Bytes.fromHexString("0x", 0));
    assertEquals(Bytes.of(0, 0), Bytes.fromHexString("0x", 2));
    assertEquals(Bytes.of(0, 0, 0, 0), Bytes.fromHexString("0x", 4));
    assertEquals(Bytes.of(0, 0), Bytes.fromHexString("00", 2));
    assertEquals(Bytes.of(0, 0), Bytes.fromHexString("0x00", 2));
    assertEquals(Bytes.of(0, 0, 1), Bytes.fromHexString("0x01", 3));
    assertEquals(Bytes.of(0x00, 0x01, 0xff, 0x2a), Bytes.fromHexString("01FF2A", 4));
    assertEquals(Bytes.of(0x01, 0xff, 0x2a), Bytes.fromHexString("0x01FF2A", 3));
    assertEquals(Bytes.of(0x00, 0x00, 0x01, 0xff, 0x2a), Bytes.fromHexString("0x01ff2a", 5));
    assertEquals(Bytes.of(0x00, 0x00, 0x01, 0xff, 0x2a), Bytes.fromHexString("0x01fF2a", 5));
  }

  @Test
  void fromHexStringLeftPaddingInvalidInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexString("fooo", 4));
    assertEquals("Illegal character 'o' found at index 1 in hex binary representation", exception.getMessage());
  }

  @Test
  void fromHexStringLeftPaddingNotLenient() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexString("0x100", 4));
    assertEquals("Invalid odd-length hex binary representation", exception.getMessage());
  }

  @Test
  void fromHexStringLeftPaddingInvalidSize() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes.fromHexStringLenient("0x001F34", 2));
    assertEquals("Hex value is too large: expected at most 2 bytes but got 3", exception.getMessage());
  }

  @Test
  void fromBase64Roundtrip() {
    Bytes value = Bytes.fromBase64String("deadbeefISDAbest");
    assertEquals("deadbeefISDAbest", value.toBase64String());
  }

  @Test
  void littleEndianRoundtrip() {
    int val = Integer.MAX_VALUE - 5;
    Bytes littleEndianEncoded = Bytes.ofUnsignedInt(val, LITTLE_ENDIAN);
    assertEquals(4, littleEndianEncoded.size());
    Bytes bigEndianEncoded = Bytes.ofUnsignedInt(val);
    assertEquals(bigEndianEncoded.get(0), littleEndianEncoded.get(3));
    assertEquals(bigEndianEncoded.get(1), littleEndianEncoded.get(2));
    assertEquals(bigEndianEncoded.get(2), littleEndianEncoded.get(1));
    assertEquals(bigEndianEncoded.get(3), littleEndianEncoded.get(0));

    int read = littleEndianEncoded.toInt(LITTLE_ENDIAN);
    assertEquals(val, read);
  }

  @Test
  void littleEndianLongRoundtrip() {
    long val = 1L << 46;
    Bytes littleEndianEncoded = Bytes.ofUnsignedLong(val, LITTLE_ENDIAN);
    assertEquals(8, littleEndianEncoded.size());
    Bytes bigEndianEncoded = Bytes.ofUnsignedLong(val);
    assertEquals(bigEndianEncoded.get(0), littleEndianEncoded.get(7));
    assertEquals(bigEndianEncoded.get(1), littleEndianEncoded.get(6));
    assertEquals(bigEndianEncoded.get(2), littleEndianEncoded.get(5));
    assertEquals(bigEndianEncoded.get(3), littleEndianEncoded.get(4));
    assertEquals(bigEndianEncoded.get(4), littleEndianEncoded.get(3));
    assertEquals(bigEndianEncoded.get(5), littleEndianEncoded.get(2));
    assertEquals(bigEndianEncoded.get(6), littleEndianEncoded.get(1));
    assertEquals(bigEndianEncoded.get(7), littleEndianEncoded.get(0));

    long read = littleEndianEncoded.toLong(LITTLE_ENDIAN);
    assertEquals(val, read);
  }

  @Test
  void reverseBytes() {
    Bytes bytes = Bytes.fromHexString("0x000102030405");
    assertEquals(Bytes.fromHexString("0x050403020100"), bytes.reverse());
  }

  @Test
  void reverseBytesEmptyArray() {
    Bytes bytes = Bytes.fromHexString("0x");
    assertEquals(Bytes.fromHexString("0x"), bytes.reverse());
  }

  @Test
  void mutableBytesIncrement() {
    MutableBytes one = MutableBytes.of(1);
    one.increment();
    assertEquals(Bytes.of(2), one);
  }

  @Test
  void mutableBytesIncrementMax() {
    MutableBytes maxed = MutableBytes.of(1, 0xFF);
    maxed.increment();
    assertEquals(Bytes.of(2, 0), maxed);
  }

  @Test
  void mutableBytesIncrementOverflow() {
    MutableBytes maxed = MutableBytes.of(0xFF, 0xFF, 0xFF);
    maxed.increment();
    assertEquals(Bytes.of(0, 0, 0), maxed);
  }

  @Test
  void mutableBytesDecrement() {
    MutableBytes one = MutableBytes.of(2);
    one.decrement();
    assertEquals(Bytes.of(1), one);
  }

  @Test
  void mutableBytesDecrementMax() {
    MutableBytes maxed = MutableBytes.of(1, 0);
    maxed.decrement();
    assertEquals(Bytes.of(0, 0xFF), maxed);
  }

  @Test
  void mutableBytesDecrementOverflow() {
    MutableBytes maxed = MutableBytes.of(0x00, 0x00, 0x00);
    maxed.decrement();
    assertEquals(Bytes.of(0xFF, 0xFF, 0xFF), maxed);
  }

  @Test
  void concatenation() {
    MutableBytes value1 = MutableBytes.wrap(Bytes.fromHexString("deadbeef").toArrayUnsafe());
    Bytes result = Bytes.concatenate(value1, value1);
    assertEquals(Bytes.fromHexString("deadbeefdeadbeef"), result);
    value1.set(0, (byte) 0);
    assertEquals(Bytes.fromHexString("deadbeefdeadbeef"), result);
  }

  @Test
  void wrap() {
    MutableBytes value1 = MutableBytes.wrap(Bytes.fromHexString("deadbeef").toArrayUnsafe());
    Bytes result = Bytes.wrap(value1, value1);
    assertEquals(Bytes.fromHexString("deadbeefdeadbeef"), result);
    value1.set(0, (byte) 0);
    assertEquals(Bytes.fromHexString("0x00adbeef00adbeef"), result);
  }

  @Test
  void random() {
    Bytes value = Bytes.random(20);
    assertNotEquals(value, Bytes.random(20));
    assertEquals(20, value.size());
  }

  @Test
  void getInt() {
    Bytes value = Bytes.fromHexString("0x00000001");
    assertEquals(1, value.getInt(0));
    assertEquals(16777216, value.getInt(0, LITTLE_ENDIAN));
    assertEquals(1, value.toInt());
    assertEquals(16777216, value.toInt(LITTLE_ENDIAN));
  }

  @Test
  void getLong() {
    Bytes value = Bytes.fromHexString("0x0000000000000001");
    assertEquals(1, value.getLong(0));
    assertEquals(72057594037927936L, value.getLong(0, LITTLE_ENDIAN));
    assertEquals(1, value.toLong());
    assertEquals(72057594037927936L, value.toLong(LITTLE_ENDIAN));
  }

  @Test
  void numberOfLeadingZeros() {
    Bytes value = Bytes.fromHexString("0x00000001");
    assertEquals(31, value.numberOfLeadingZeros());
  }

  @Test
  void and() {
    Bytes value = Bytes.fromHexString("0x01000001").and(Bytes.fromHexString("0x01000000"));
    assertEquals(Bytes.fromHexString("0x01000000"), value);
  }

  @Test
  void andResult() {
    MutableBytes result = MutableBytes.create(4);
    Bytes.fromHexString("0x01000001").and(Bytes.fromHexString("0x01000000"), result);
    assertEquals(Bytes.fromHexString("0x01000000"), result);
  }

  @Test
  void or() {
    Bytes value = Bytes.fromHexString("0x01000001").or(Bytes.fromHexString("0x01000000"));
    assertEquals(Bytes.fromHexString("0x01000001"), value);
  }

  @Test
  void orResult() {
    MutableBytes result = MutableBytes.create(4);
    Bytes.fromHexString("0x01000001").or(Bytes.fromHexString("0x01000000"), result);
    assertEquals(Bytes.fromHexString("0x01000001"), result);
  }

  @Test
  void xor() {
    Bytes value = Bytes.fromHexString("0x01000001").xor(Bytes.fromHexString("0x01000000"));
    assertEquals(Bytes.fromHexString("0x00000001"), value);
  }

  @Test
  void xorResult() {
    MutableBytes result = MutableBytes.create(4);
    Bytes.fromHexString("0x01000001").xor(Bytes.fromHexString("0x01000000"), result);
    assertEquals(Bytes.fromHexString("0x00000001"), result);
  }

  @Test
  void not() {
    Bytes value = Bytes.fromHexString("0x01000001").not();
    assertEquals(Bytes.fromHexString("0xfefffffe"), value);
  }

  @Test
  void notResult() {
    MutableBytes result = MutableBytes.create(4);
    Bytes.fromHexString("0x01000001").not(result);
    assertEquals(Bytes.fromHexString("0xfefffffe"), result);
  }

  @Test
  void shiftRight() {
    Bytes value = Bytes.fromHexString("0x01000001").shiftRight(2);
    assertEquals(Bytes.fromHexString("0x00400000"), value);
  }

  @Test
  void shiftRightResult() {
    MutableBytes result = MutableBytes.create(4);
    Bytes.fromHexString("0x01000001").shiftRight(2, result);
    assertEquals(Bytes.fromHexString("0x00400000"), result);
  }

  @Test
  void shiftLeft() {
    Bytes value = Bytes.fromHexString("0x01000001").shiftLeft(2);
    assertEquals(Bytes.fromHexString("0x04000004"), value);
  }

  @Test
  void shiftLeftResult() {
    MutableBytes result = MutableBytes.create(4);
    Bytes.fromHexString("0x01000001").shiftLeft(2, result);
    assertEquals(Bytes.fromHexString("0x04000004"), result);
  }

  @Test
  void commonPrefix() {
    Bytes value = Bytes.fromHexString("0x01234567");
    Bytes value2 = Bytes.fromHexString("0x01236789");
    assertEquals(2, value.commonPrefixLength(value2));
    assertEquals(Bytes.fromHexString("0x0123"), value.commonPrefix(value2));
  }

  @Test
  void testWrapByteBufEmpty() {
    ByteBuf buffer = Unpooled.buffer(0);
    assertSame(Bytes.EMPTY, Bytes.wrapByteBuf(buffer));
  }

  @Test
  void testWrapByteBufWithIndexEmpty() {
    ByteBuf buffer = Unpooled.buffer(3);
    assertSame(Bytes.EMPTY, Bytes.wrapByteBuf(buffer, 3, 0));
  }

  @Test
  void testWrapByteBufSizeWithOffset() {
    ByteBuf buffer = Unpooled.buffer(10);
    assertEquals(1, Bytes.wrapByteBuf(buffer, 1, 1).size());
  }

  @Test
  void testWrapByteBufSize() {
    ByteBuf buffer = Unpooled.buffer(20);
    assertEquals(20, Bytes.wrapByteBuf(buffer).size());
  }

  @Test
  void testWrapByteBufReadableBytes() {
    ByteBuf buffer = Unpooled.buffer(20).writeByte(3);
    assertEquals(1, Bytes.wrapByteBuf(buffer, 0, buffer.readableBytes()).size());
  }

  @Test
  void segmentBytes() {
    Bytes b = Bytes
        .wrap(
            Bytes32.ZERO,
            Bytes32.random(),
            Bytes32.rightPad(Bytes.fromHexStringLenient("0x1")),
            Bytes.fromHexString("0xf000"));
    Bytes32[] result = Bytes.segment(b);
    assertEquals(4, result.length);
    assertEquals(Bytes32.rightPad(Bytes.fromHexString("0xf000")), result[3]);
  }

  @Test
  void segments() {
    Bytes value = Bytes.fromHexString("0x7b600035f660115760006000526001601ff35b60016000526001601ff3600052601c6000f3");
    Bytes32[] result = Bytes.segment(value);
    assertEquals(Bytes.fromHexString("0x7b600035f660115760006000526001601ff35b60016000526001601ff3600052"), result[0]);
    assertEquals(Bytes.fromHexString("0x601c6000f3000000000000000000000000000000000000000000000000000000"), result[1]);
  }

  @Test
  void testTrimLeadingZeros() {
    Bytes b = Bytes.fromHexString("0x000000f300567800");
    assertEquals(Bytes.fromHexString("0xf300567800"), b.trimLeadingZeros());
  }

  @Test
  void testTrimTrailingZeros() {
    Bytes b = Bytes.fromHexString("0x000000f300567800");
    assertEquals(Bytes.fromHexString("0x000000f3005678"), b.trimTrailingZeros());
  }
}