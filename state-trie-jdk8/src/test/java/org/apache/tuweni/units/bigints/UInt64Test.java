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
package org.apache.tuweni.units.bigints;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UInt64Test {

  private static UInt64 v(long v) {
    return UInt64.valueOf(v);
  }

  private static UInt64 hv(String s) {
    return UInt64.fromHexString(s);
  }


  private static Bytes b(String s) {
    return Bytes.fromHexString(s);
  }

  @Test
  void valueOfLong() {
    assertThrows(IllegalArgumentException.class, () -> UInt64.valueOf(-1));
    assertThrows(IllegalArgumentException.class, () -> UInt64.valueOf(Long.MIN_VALUE));
    assertThrows(IllegalArgumentException.class, () -> UInt64.valueOf(~0L));
  }

  @Test
  void valueOfBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> UInt64.valueOf(BigInteger.valueOf(-1)));
    assertThrows(IllegalArgumentException.class, () -> UInt64.valueOf(BigInteger.valueOf(2).pow(64)));
  }

  @ParameterizedTest
  @MethodSource("addProvider")
  void add(UInt64 v1, UInt64 v2, UInt64 expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addProvider() {
    return Stream
        .of(
            Arguments.of(v(1), v(0), v(1)),
            Arguments.of(v(5), v(0), v(5)),
            Arguments.of(v(0), v(1), v(1)),
            Arguments.of(v(0), v(100), v(100)),
            Arguments.of(v(2), v(2), v(4)),
            Arguments.of(v(100), v(90), v(190)),
            Arguments.of(UInt64.MAX_VALUE, v(1), v(0)),
            Arguments.of(UInt64.MAX_VALUE, v(2), v(1)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFF0"), v(1), hv("0xFFFFFFFFFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), v(1), UInt64.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addLongProvider")
  void addLong(UInt64 v1, long v2, UInt64 expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 0L, v(1)),
            Arguments.of(v(5), 0L, v(5)),
            Arguments.of(v(0), 1L, v(1)),
            Arguments.of(v(0), 100L, v(100)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(100), 90L, v(190)),
            Arguments.of(UInt64.MAX_VALUE, 1L, v(0)),
            Arguments.of(UInt64.MAX_VALUE, 2L, v(1)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFF0"), 1L, hv("0xFFFFFFFFFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), 1L, UInt64.MAX_VALUE),
            Arguments.of(v(10), -5L, v(5)),
            Arguments.of(v(0), -1L, UInt64.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addModProvider")
  void addMod(UInt64 v1, UInt64 v2, UInt64 m, UInt64 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(1), UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), v(1), UInt64.valueOf(2), v(0)),
            Arguments.of(UInt64.MAX_VALUE.subtract(2), v(1), UInt64.MAX_VALUE, UInt64.MAX_VALUE.subtract(1)),
            Arguments.of(UInt64.MAX_VALUE.subtract(1), v(1), UInt64.MAX_VALUE, v(0)),
            Arguments.of(v(2), v(1), UInt64.valueOf(2), v(1)),
            Arguments.of(v(3), v(2), UInt64.valueOf(6), v(5)),
            Arguments.of(v(3), v(4), UInt64.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModOfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(v(1), UInt64.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModUInt64UInt64Provider")
  void addModUInt64UInt64(UInt64 v1, UInt64 v2, UInt64 m, UInt64 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModUInt64UInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.ONE, UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), UInt64.ONE, UInt64.valueOf(2), v(0)),
            Arguments.of(UInt64.MAX_VALUE.subtract(2), UInt64.ONE, UInt64.MAX_VALUE, UInt64.MAX_VALUE.subtract(1)),
            Arguments.of(UInt64.MAX_VALUE.subtract(1), UInt64.ONE, UInt64.MAX_VALUE, v(0)),
            Arguments.of(v(2), UInt64.ONE, UInt64.valueOf(2), v(1)),
            Arguments.of(v(3), UInt64.valueOf(2), UInt64.valueOf(6), v(5)),
            Arguments.of(v(3), UInt64.valueOf(4), UInt64.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModLongUInt64OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, UInt64.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongUInt64Provider")
  void addModLongUInt64(UInt64 v1, long v2, UInt64 m, UInt64 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 1L, UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), 1L, UInt64.valueOf(2), v(0)),
            Arguments.of(UInt64.MAX_VALUE.subtract(2), 1L, UInt64.MAX_VALUE, UInt64.MAX_VALUE.subtract(1)),
            Arguments.of(UInt64.MAX_VALUE.subtract(1), 1L, UInt64.MAX_VALUE, v(0)),
            Arguments.of(v(2), 1L, UInt64.valueOf(2), v(1)),
            Arguments.of(v(2), -1L, UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), -7L, UInt64.valueOf(5), v(4)));
  }

  @Test
  void shouldThrowForAddModUInt64UInt64OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(UInt64.ONE, UInt64.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongLongProvider")
  void addModLongLong(UInt64 v1, long v2, long m, UInt64 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongLongProvider() {
    return Stream
        .of(Arguments.of(v(0), 1L, 2L, v(1)), Arguments.of(v(1), 1L, 2L, v(0)), Arguments.of(v(2), 1L, 2L, v(1)));
  }

  @Test
  void shouldThrowForAddModLongLongOfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, 0));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForAddModLongLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, -5));
    assertEquals("addMod unsigned with negative modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("subtractProvider")
  void subtract(UInt64 v1, UInt64 v2, UInt64 expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractProvider() {
    return Stream
        .of(
            Arguments.of(v(1), v(0), v(1)),
            Arguments.of(v(5), v(0), v(5)),
            Arguments.of(v(2), v(1), v(1)),
            Arguments.of(v(100), v(100), v(0)),
            Arguments.of(v(0), v(1), UInt64.MAX_VALUE),
            Arguments.of(v(1), v(2), UInt64.MAX_VALUE),
            Arguments.of(UInt64.MAX_VALUE, v(1), hv("0xFFFFFFFFFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractLongProvider")
  void subtractLong(UInt64 v1, long v2, UInt64 expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 0L, v(1)),
            Arguments.of(v(5), 0L, v(5)),
            Arguments.of(v(2), 1L, v(1)),
            Arguments.of(v(100), 100L, v(0)),
            Arguments.of(v(0), 1L, UInt64.MAX_VALUE),
            Arguments.of(v(1), 2L, UInt64.MAX_VALUE),
            Arguments.of(UInt64.MAX_VALUE, 1L, hv("0xFFFFFFFFFFFFFFFE")),
            Arguments.of(v(0), -1L, v(1)),
            Arguments.of(v(0), -100L, v(100)),
            Arguments.of(v(2), -2L, v(4)));
  }

  @ParameterizedTest
  @MethodSource("multiplyProvider")
  void multiply(UInt64 v1, UInt64 v2, UInt64 expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(2), v(0)),
            Arguments.of(v(1), v(2), v(2)),
            Arguments.of(v(2), v(2), v(4)),
            Arguments.of(v(3), v(2), v(6)),
            Arguments.of(v(4), v(2), v(8)),
            Arguments.of(v(10), v(18), v(180)),
            Arguments.of(v(2), v(8), v(16)),
            Arguments.of(v(7), v(8), v(56)),
            Arguments.of(v(8), v(8), v(64)),
            Arguments.of(v(17), v(8), v(136)),
            Arguments.of(v(22), v(0), v(0)));
  }

  @ParameterizedTest
  @MethodSource("multiplyLongProvider")
  void multiplyLong(UInt64 v1, long v2, UInt64 expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(1), 2L, v(2)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(3), 2L, v(6)),
            Arguments.of(v(4), 2L, v(8)),
            Arguments.of(v(10), 18L, v(180)),
            Arguments.of(v(2), 8L, v(16)),
            Arguments.of(v(7), 8L, v(56)),
            Arguments.of(v(8), 8L, v(64)),
            Arguments.of(v(17), 8L, v(136)),
            Arguments.of(v(22), 0L, v(0)),
            Arguments.of(hv("0x0FFFFFFFFFFFFFFF"), 2L, hv("0x1FFFFFFFFFFFFFFE")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), 2L, hv("0xFFFFFFFFFFFFFFFE")));
  }

  @Test
  void shouldThrowForMultiplyLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiply(-5));
    assertEquals("multiply unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModProvider")
  void multiplyMod(UInt64 v1, UInt64 v2, UInt64 m, UInt64 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(5), UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), v(3), UInt64.valueOf(7), v(6)),
            Arguments.of(v(2), v(3), UInt64.valueOf(6), v(0)),
            Arguments.of(v(2), v(0), UInt64.valueOf(6), v(0)),
            Arguments.of(hv("0x0FFFFFFFFFFFFFFE"), v(2), UInt64.MAX_VALUE, hv("0x1FFFFFFFFFFFFFFC")));
  }

  @Test
  void shouldThrowForMultiplyModOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(v(1), UInt64.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongUInt64Provider")
  void multiplyModLongUInt64(UInt64 v1, long v2, UInt64 m, UInt64 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 5L, UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), 3L, UInt64.valueOf(7), v(6)),
            Arguments.of(v(2), 3L, UInt64.valueOf(6), v(0)),
            Arguments.of(v(2), 0L, UInt64.valueOf(6), v(0)),
            Arguments.of(hv("0x0FFFFFFFFFFFFFFE"), 2L, UInt64.MAX_VALUE, hv("0x1FFFFFFFFFFFFFFC")));
  }

  @Test
  void shouldThrowForMultiplyModLongUInt64OfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(1L, UInt64.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongUInt64OfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(-1, UInt64.valueOf(2)));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongLongProvider")
  void multiplyModLongLong(UInt64 v1, long v2, long m, UInt64 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 5L, 2L, v(0)),
            Arguments.of(v(2), 3L, 7L, v(6)),
            Arguments.of(v(2), 3L, 6L, v(0)),
            Arguments.of(v(2), 0L, 6L, v(0)),
            Arguments.of(hv("0x0FFFFFFFFFFFFFFE"), 2L, Long.MAX_VALUE, hv("0x1FFFFFFFFFFFFFFC")));
  }

  @Test
  void shouldThrowForMultiplyModLongLongOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(1, 0));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongLongOfModNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiplyMod(5, -7));
    assertEquals("multiplyMod unsigned with negative modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(-1, 2));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideProvider")
  void divide(UInt64 v1, UInt64 v2, UInt64 expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(2), v(0)),
            Arguments.of(v(1), v(2), v(0)),
            Arguments.of(v(2), v(2), v(1)),
            Arguments.of(v(3), v(2), v(1)),
            Arguments.of(v(4), v(2), v(2)),
            Arguments.of(v(2), v(8), v(0)),
            Arguments.of(v(7), v(8), v(0)),
            Arguments.of(v(8), v(8), v(1)),
            Arguments.of(v(9), v(8), v(1)),
            Arguments.of(v(17), v(8), v(2)),
            Arguments.of(v(1024), v(8), v(128)),
            Arguments.of(v(1026), v(8), v(128)));
  }

  @Test
  void shouldThrowForDivideByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(v(0)));
    assertEquals("divide by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideLongProvider")
  void divideLong(UInt64 v1, long v2, UInt64 expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(1), 2L, v(0)),
            Arguments.of(v(2), 2L, v(1)),
            Arguments.of(v(3), 2L, v(1)),
            Arguments.of(v(4), 2L, v(2)),
            Arguments.of(v(2), 8L, v(0)),
            Arguments.of(v(7), 8L, v(0)),
            Arguments.of(v(8), 8L, v(1)),
            Arguments.of(v(9), 8L, v(1)),
            Arguments.of(v(17), 8L, v(2)),
            Arguments.of(v(1024), 8L, v(128)),
            Arguments.of(v(1026), 8L, v(128)));
  }

  @Test
  void shouldThrowForDivideLongByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(0));
    assertEquals("divide by zero", exception.getMessage());
  }

  @Test
  void shouldThrowForDivideLongByNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(-5));
    assertEquals("divide unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("powUInt64Provider")
  void powUInt64(UInt64 v1, UInt64 v2, UInt64 expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), UInt64.valueOf(2), v(4)),
            Arguments.of(v(2), UInt64.valueOf(8), v(256)),
            Arguments.of(v(3), UInt64.valueOf(3), v(27)),
            Arguments.of(hv("0xFFFFFFFFFFF0F0F0"), UInt64.valueOf(3), hv("0xF2A920E119A2F000")));
  }

  @ParameterizedTest
  @MethodSource("powLongProvider")
  void powLong(UInt64 v1, long v2, UInt64 expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(2), 8L, v(256)),
            Arguments.of(v(3), 3L, v(27)),
            Arguments.of(hv("0xFFFFFFFFFFF0F0F0"), 3L, hv("0xF2A920E119A2F000")));
  }

  @ParameterizedTest
  @MethodSource("modLongProvider")
  void modLong(UInt64 v1, long v2, UInt64 expected) {
    assertValueEquals(expected, v1.mod(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> modLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(1), 2L, v(1)),
            Arguments.of(v(2), 2L, v(0)),
            Arguments.of(v(3), 2L, v(1)),
            Arguments.of(v(0), 8L, v(0)),
            Arguments.of(v(1), 8L, v(1)),
            Arguments.of(v(2), 8L, v(2)),
            Arguments.of(v(3), 8L, v(3)),
            Arguments.of(v(7), 8L, v(7)),
            Arguments.of(v(8), 8L, v(0)),
            Arguments.of(v(9), 8L, v(1)),
            Arguments.of(v(1024), 8L, v(0)),
            Arguments.of(v(1026), 8L, v(2)));
  }

  @Test
  void shouldThrowForModLongByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).mod(0));
    assertEquals("mod by zero", exception.getMessage());
  }

  @Test
  void shouldThrowForModLongByNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).mod(-5));
    assertEquals("mod by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("andProvider")
  void and(UInt64 v1, Object v2, UInt64 expected) {
    if (v2 instanceof UInt64) {
      assertValueEquals(expected, v1.and((UInt64) v2));
    } else if (v2 instanceof Bytes) {
      assertValueEquals(expected, v1.and((Bytes) v2));
    } else {
      throw new IllegalArgumentException(v2.getClass().getName());
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> andProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFF00000000"), hv("0x0000000000000000")),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFFFF000000"), hv("0x00000000FF000000")),
            Arguments.of(hv("0x00000000FFFFFFFF"), b("0xFFFFFFFFFF000000"), hv("0x00000000FF000000")));
  }

  @ParameterizedTest
  @MethodSource("orProvider")
  void or(UInt64 v1, Object v2, UInt64 expected) {
    if (v2 instanceof UInt64) {
      assertValueEquals(expected, v1.or((UInt64) v2));
    } else if (v2 instanceof Bytes) {
      assertValueEquals(expected, v1.or((Bytes) v2));
    } else {
      throw new IllegalArgumentException(v2.getClass().getName());
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> orProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFF00000000"), hv("0xFFFFFFFFFFFFFFFF")),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFF00000000"), hv("0xFFFFFFFFFFFFFFFF")),
            Arguments.of(hv("0x00000000000000FF"), hv("0xFFFFFFFF00000000"), hv("0xFFFFFFFF000000FF")),
            Arguments.of(hv("0x00000000000000FF"), b("0xFFFFFFFF00000000"), hv("0xFFFFFFFF000000FF")));
  }

  @ParameterizedTest
  @MethodSource("xorProvider")
  void xor(UInt64 v1, Object v2, UInt64 expected) {
    if (v2 instanceof UInt64) {
      assertValueEquals(expected, v1.xor((UInt64) v2));
    } else if (v2 instanceof Bytes) {
      assertValueEquals(expected, v1.xor((Bytes) v2));
    } else {
      throw new IllegalArgumentException(v2.getClass().getName());
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> xorProvider() {
    return Stream
        .of(
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), hv("0xFFFFFFFFFFFFFFFF"), hv("0x0000000000000000")),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFF00000000"), hv("0xFFFFFFFFFFFFFFFF")),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFFFF000000"), hv("0xFFFFFFFF00FFFFFF")),
            Arguments.of(hv("0x00000000FFFFFFFF"), b("0xFFFFFFFFFF000000"), hv("0xFFFFFFFF00FFFFFF")));
  }

  @ParameterizedTest
  @MethodSource("notProvider")
  void not(UInt64 value, UInt64 expected) {
    assertValueEquals(expected, value.not());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> notProvider() {
    return Stream
        .of(
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), hv("0x0000000000000000")),
            Arguments.of(hv("0x0000000000000000"), hv("0xFFFFFFFFFFFFFFFF")),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0xFFFFFFFF00000000")));
  }

  @ParameterizedTest
  @MethodSource("shiftLeftProvider")
  void shiftLeft(UInt64 value, int distance, UInt64 expected) {
    assertValueEquals(expected, value.shiftLeft(distance));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> shiftLeftProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x01"), 1, hv("0x02")),
            Arguments.of(hv("0x01"), 2, hv("0x04")),
            Arguments.of(hv("0x01"), 8, hv("0x0100")),
            Arguments.of(hv("0x01"), 9, hv("0x0200")),
            Arguments.of(hv("0x01"), 16, hv("0x10000")),
            Arguments.of(hv("0x00FF00"), 4, hv("0x0FF000")),
            Arguments.of(hv("0x00FF00"), 8, hv("0xFF0000")),
            Arguments.of(hv("0x00FF00"), 1, hv("0x01FE00")),
            Arguments.of(hv("0x0000000000000001"), 16, hv("0x0000000000010000")),
            Arguments.of(hv("0x0000000000000001"), 15, hv("0x0000000000008000")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), 55, hv("0xFF80000000000000")),
            Arguments.of(hv("0x00000000FFFFFFFF"), 50, hv("0xFFFC000000000000")));
  }

  @ParameterizedTest
  @MethodSource("shiftRightProvider")
  void shiftRight(UInt64 value, int distance, UInt64 expected) {
    assertValueEquals(expected, value.shiftRight(distance));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> shiftRightProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x01"), 1, hv("0x00")),
            Arguments.of(hv("0x10"), 1, hv("0x08")),
            Arguments.of(hv("0x10"), 2, hv("0x04")),
            Arguments.of(hv("0x10"), 8, hv("0x00")),
            Arguments.of(hv("0x1000"), 4, hv("0x0100")),
            Arguments.of(hv("0x1000"), 5, hv("0x0080")),
            Arguments.of(hv("0x1000"), 8, hv("0x0010")),
            Arguments.of(hv("0x1000"), 9, hv("0x0008")),
            Arguments.of(hv("0x1000"), 16, hv("0x0000")),
            Arguments.of(hv("0x00FF00"), 4, hv("0x000FF0")),
            Arguments.of(hv("0x00FF00"), 8, hv("0x0000FF")),
            Arguments.of(hv("0x00FF00"), 1, hv("0x007F80")),
            Arguments.of(hv("0x1000000000000000"), 16, hv("0x0000100000000000")),
            Arguments.of(hv("0x1000000000000000"), 15, hv("0x0000200000000000")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), 55, hv("0x00000000000001FF")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), 202, hv("0x0000000000000000")));
  }

  @ParameterizedTest
  @MethodSource("intValueProvider")
  void intValue(UInt64 value, int expected) {
    assertEquals(expected, value.intValue());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> intValueProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x"), 0),
            Arguments.of(hv("0x00"), 0),
            Arguments.of(hv("0x00000000"), 0),
            Arguments.of(hv("0x01"), 1),
            Arguments.of(hv("0x0001"), 1),
            Arguments.of(hv("0x000001"), 1),
            Arguments.of(hv("0x00000001"), 1),
            Arguments.of(hv("0x0100"), 256),
            Arguments.of(hv("0x000100"), 256),
            Arguments.of(hv("0x00000100"), 256));
  }

  @Test
  void shouldThrowForIntValueOfOversizeValue() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> hv("0x0100000000").intValue());
    assertEquals("Value does not fit a 4 byte int", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("longValueProvider")
  void longValue(UInt64 value, long expected) {
    assertEquals(expected, value.toLong());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> longValueProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x"), 0L),
            Arguments.of(hv("0x00"), 0L),
            Arguments.of(hv("0x00000000"), 0L),
            Arguments.of(hv("0x01"), 1L),
            Arguments.of(hv("0x0001"), 1L),
            Arguments.of(hv("0x000001"), 1L),
            Arguments.of(hv("0x00000001"), 1L),
            Arguments.of(hv("0x0000000001"), 1L),
            Arguments.of(hv("0x000000000001"), 1L),
            Arguments.of(hv("0x0100"), 256L),
            Arguments.of(hv("0x000100"), 256L),
            Arguments.of(hv("0x00000100"), 256L),
            Arguments.of(hv("0x00000100"), 256L),
            Arguments.of(hv("0x000000000100"), 256L),
            Arguments.of(hv("0x00000000000100"), 256L),
            Arguments.of(hv("0x00000000000100"), 256L),
            Arguments.of(hv("0xFFFFFFFF"), (1L << 32) - 1));
  }

  @ParameterizedTest
  @MethodSource("compareToProvider")
  void compareTo(UInt64 v1, UInt64 v2, int expected) {
    assertEquals(expected, v1.compareTo(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> compareToProvider() {
    return Stream
        .of(
            Arguments.of(v(5), v(5), 0),
            Arguments.of(v(5), v(3), 1),
            Arguments.of(v(5), v(6), -1),
            Arguments.of(hv("0x0000000000000000"), hv("0x0000000000000000"), 0),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), hv("0xFFFFFFFFFFFFFFFF"), 0),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0x00000000FFFFFFFF"), 0),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), hv("0x0000000000000000"), 1),
            Arguments.of(hv("0x0000000000000000"), hv("0xFFFFFFFFFFFFFFFF"), -1),
            Arguments.of(hv("0x00000001FFFFFFFF"), hv("0x00000000FFFFFFFF"), 1),
            Arguments.of(hv("0x00000000FFFFFFFE"), hv("0x00000000FFFFFFFF"), -1));
  }

  @ParameterizedTest
  @MethodSource("toBytesProvider")
  void toBytesTest(UInt64 value, Bytes expected) {
    assertEquals(expected, value.toBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.fromHexString("0x0000000000000000")),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x0000000001000000")),
            Arguments.of(hv("0x0100000000"), Bytes.fromHexString("0x0000000100000000")),
            Arguments.of(hv("0xf100000000ab"), Bytes.fromHexString("0x0000F100000000AB")));
  }

  @ParameterizedTest
  @MethodSource("toMinimalBytesProvider")
  void toMinimalBytesTest(UInt64 value, Bytes expected) {
    assertEquals(expected, value.toMinimalBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toMinimalBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.EMPTY),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")),
            Arguments.of(hv("0x0100000000"), Bytes.fromHexString("0x0100000000")),
            Arguments.of(hv("0xf100000000ab"), Bytes.fromHexString("0xf100000000ab")),
            Arguments.of(hv("0100000000000000"), Bytes.fromHexString("0x0100000000000000")));
  }

  @ParameterizedTest
  @MethodSource("numberOfLeadingZerosProvider")
  void numberOfLeadingZeros(UInt64 value, int expected) {
    assertEquals(expected, value.numberOfLeadingZeros());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> numberOfLeadingZerosProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), 64),
            Arguments.of(hv("0x01"), 63),
            Arguments.of(hv("0x02"), 62),
            Arguments.of(hv("0x03"), 62),
            Arguments.of(hv("0x0F"), 60),
            Arguments.of(hv("0x8F"), 56),
            Arguments.of(hv("0x100000000"), 31));
  }

  @ParameterizedTest
  @MethodSource("bitLengthProvider")
  void bitLength(UInt64 value, int expected) {
    assertEquals(expected, value.bitLength());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> bitLengthProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), 0),
            Arguments.of(hv("0x01"), 1),
            Arguments.of(hv("0x02"), 2),
            Arguments.of(hv("0x03"), 2),
            Arguments.of(hv("0x0F"), 4),
            Arguments.of(hv("0x8F"), 8),
            Arguments.of(hv("0x100000000"), 33));
  }

  @ParameterizedTest
  @MethodSource("addExactProvider")
  void addExact(UInt64 value, UInt64 operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactProvider() {
    return Stream.of(Arguments.of(UInt64.MAX_VALUE, v(1)), Arguments.of(UInt64.MAX_VALUE, UInt64.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addExactLongProvider")
  void addExactLong(UInt64 value, long operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactLongProvider() {
    return Stream
        .of(Arguments.of(UInt64.MAX_VALUE, 3), Arguments.of(UInt64.MAX_VALUE, Long.MAX_VALUE), Arguments.of(v(0), -1));
  }

  @ParameterizedTest
  @MethodSource("subtractExactProvider")
  void subtractExact(UInt64 value, UInt64 operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactProvider() {
    return Stream.of(Arguments.of(v(0), v(1)), Arguments.of(v(0), UInt64.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("subtractExactLongProvider")
  void subtractExactLong(UInt64 value, long operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactLongProvider() {
    return Stream.of(Arguments.of(v(0), 1), Arguments.of(v(0), Long.MAX_VALUE), Arguments.of(UInt64.MAX_VALUE, -1));
  }

  private void assertValueEquals(UInt64 expected, UInt64 actual) {
    String msg = String.format("Expected %s but got %s", expected.toHexString(), actual.toHexString());
    assertEquals(expected, actual, msg);
  }

  @Test
  void testToDecimalString() {
    assertEquals("3456", UInt64.valueOf(3456).toDecimalString());
  }
}