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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Bytes32Test {

  @Test
  void testConcatenation() {
    Bytes wrapped = Bytes.wrap(Bytes.wrap(new byte[32]), Bytes.wrap(new byte[6]));
    assertEquals(37, wrapped.slice(0, 37).size());
    Bytes wrappedCopy = wrapped.slice(0, 37).copy();
    assertEquals(wrapped.slice(0, 37), wrappedCopy);
  }

  @Test
  void constantBytes32slice() {
    assertEquals(Bytes32.ZERO.slice(12, 20).size(), 20);
  }

  @Test
  void constantBytesslice() {
    assertEquals(Bytes.repeat((byte) 1, 63).slice(12, 20).size(), 20);
  }

  @Test
  void testMutableBytes32WrapWithOffset() {
    Bytes bytes = Bytes
        .fromHexString(
            "0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    MutableBytes mutableBytes = MutableBytes.create(48);
    bytes.copyTo(mutableBytes);
    assertEquals(
        "0x112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00",
        Bytes32.wrap(mutableBytes, 1).toHexString());
  }

  @Test
  void testMutableBytes32SliceWithOffset() {
    Bytes bytes = Bytes
        .fromHexString(
            "0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    MutableBytes mutableBytes = MutableBytes.create(48);
    bytes.copyTo(mutableBytes);
    assertEquals("0x11", mutableBytes.slice(1, 1).toHexString());
    assertEquals("0x1122", mutableBytes.slice(1, 2).toHexString());
    assertEquals("0x112233445566778899aa", mutableBytes.slice(1, 10).toHexString());
    assertEquals("0x112233445566778899aabbccddeeff", mutableBytes.slice(1, 15).toHexString());
    assertEquals(
        "0x112233445566778899aabbccddeeff00112233445566778899aabbccddee",
        mutableBytes.slice(1, 30).toHexString());
    assertEquals(
        "0x112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00",
        mutableBytes.slice(1, 32).toHexString());
    assertEquals(
        "0x112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddee",
        mutableBytes.slice(1, 46).toHexString());
  }

  @Test
  void testBytes32SliceWithOffset() {
    Bytes bytes = Bytes
        .fromHexString(
            "0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    assertEquals("0x11", bytes.slice(1, 1).toHexString());
    assertEquals("0x1122", bytes.slice(1, 2).toHexString());
    assertEquals("0x112233445566778899aa", bytes.slice(1, 10).toHexString());
    assertEquals("0x112233445566778899aabbccddeeff", bytes.slice(1, 15).toHexString());
    assertEquals("0x112233445566778899aabbccddeeff00112233445566778899aabbccddee", bytes.slice(1, 30).toHexString());
    assertEquals(
        "0x112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00",
        bytes.slice(1, 32).toHexString());
    assertEquals(
        "0x112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddee",
        bytes.slice(1, 46).toHexString());
  }

  @Test
  void failsWhenWrappingArraySmallerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.wrap(new byte[31]));
    assertEquals("Expected 32 bytes but got 31", exception.getMessage());
  }

  @Test
  void failsWhenWrappingArrayLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.wrap(new byte[33]));
    assertEquals("Expected 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void leftPadAValueToBytes32() {
    Bytes32 b32 = Bytes32.leftPad(Bytes.of(1, 2, 3));
    assertEquals(32, b32.size());
    for (int i = 0; i < 28; ++i) {
      assertEquals((byte) 0, b32.get(i));
    }
    assertEquals((byte) 1, b32.get(29));
    assertEquals((byte) 2, b32.get(30));
    assertEquals((byte) 3, b32.get(31));
  }

  @Test
  void rightPadAValueToBytes32() {
    Bytes32 b32 = Bytes32.rightPad(Bytes.of(1, 2, 3));
    assertEquals(32, b32.size());
    for (int i = 3; i < 32; ++i) {
      assertEquals((byte) 0, b32.get(i));
    }
    assertEquals((byte) 1, b32.get(0));
    assertEquals((byte) 2, b32.get(1));
    assertEquals((byte) 3, b32.get(2));
  }

  @Test
  void failsWhenLeftPaddingValueLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.leftPad(MutableBytes.create(33)));
    assertEquals("Expected at most 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void failsWhenRightPaddingValueLargerThan32() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> Bytes32.rightPad(MutableBytes.create(33)));
    assertEquals("Expected at most 32 bytes but got 33", exception.getMessage());
  }

  @Test
  void testWrapSlicesCorrectly() {
    Bytes input = Bytes
        .fromHexString(
            "0xA99A76ED7796F7BE22D5B7E85DEEB7C5677E88E511E0B337618F8C4EB61349B4BF2D153F649F7B53359FE8B94A38E44C00000000000000000000000000000000");
    Bytes32 value = Bytes32.wrap(input, 0);
    assertEquals(Bytes.fromHexString("0xA99A76ED7796F7BE22D5B7E85DEEB7C5677E88E511E0B337618F8C4EB61349B4"), value);

    Bytes32 secondValue = Bytes32.wrap(input, 32);
    assertEquals(
        Bytes.fromHexString("0xBF2D153F649F7B53359FE8B94A38E44C00000000000000000000000000000000"),
        secondValue);
  }
}